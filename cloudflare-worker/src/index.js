/**
 * Shikshak Sathi Bihar — Cloudflare Worker + R2 Storage Backend
 *
 * Authentication:
 *   - Admin operations require `Authorization: Bearer <Firebase ID Token>`.
 *   - Tokens are verified cryptographically against Google Secure Token JWKS
 *     and restricted to verified emails listed in `env.ADMIN_EMAILS`
 *     (default: satyamprakash100@gmail.com).
 *   - Normal users do NOT need login; GET /files/<key> serves public assets with
 *     aggressive Cloudflare CDN Edge Cache + immutable Cache-Control headers.
 *   - No Cloudflare API tokens or R2 S3 keys are ever placed in the Android APK.
 */

const GOOGLE_JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

const MAX_UPLOAD_BYTES = 50 * 1024 * 1024; // 50 MB max per document/media upload
const ALLOWED_FOLDERS = new Set([
  "notices",
  "downloads",
  "news",
  "banners",
  "holidays",
  "manifests",
  "misc",
]);

const ALLOWED_MIME_TYPES = new Set([
  "application/pdf",
  "image/jpeg",
  "image/jpg",
  "image/png",
  "image/webp",
  "image/gif",
  "application/json",
]);

let cachedJwks = null;
let cachedJwksExpiresAt = 0;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    try {
      // 1. Health Check
      if (path === "/health" && request.method === "GET") {
        return jsonResponse({
          ok: true,
          service: "shikshak-sathi-r2-worker",
          projectId: env.FIREBASE_PROJECT_ID || "gen-lang-client-0268244143",
          publicR2Configured: Boolean((env.R2_PUBLIC_BASE_URL || "").trim()),
          timestamp: Date.now(),
        });
      }

      // 2. Public Fast CDN File Access: GET /files/<objectKey> or HEAD /files/<objectKey>
      if (path.startsWith("/files/") && (request.method === "GET" || request.method === "HEAD")) {
        return await handlePublicFileRead(request, env, ctx, url);
      }

      // 3. Admin-Protected API Endpoints
      if (path.startsWith("/api/admin/")) {
        const authResult = await verifyAdminFirebaseToken(request, env);
        if (!authResult.ok) {
          return jsonResponse({ success: false, error: authResult.error }, authResult.status);
        }

        if (path === "/api/admin/upload" && request.method === "POST") {
          return await handleAdminUpload(request, env, url, authResult.email);
        }

        if (
          (path === "/api/admin/delete" && request.method === "POST") ||
          (path === "/api/admin/files" && request.method === "DELETE")
        ) {
          return await handleAdminDelete(request, env, url);
        }

        if (path === "/api/admin/files" && request.method === "GET") {
          return await handleAdminListFiles(env, url);
        }

        if (path === "/api/admin/publish-manifest" && request.method === "POST") {
          return await handlePublishManifest(request, env, url, authResult.email);
        }

        return jsonResponse({ success: false, error: "Not found" }, 404);
      }

      return jsonResponse({ success: false, error: "Route not found" }, 404);
    } catch (err) {
      console.error("Worker unhandled error:", err);
      return jsonResponse(
        { success: false, error: err?.message || "Internal server error" },
        500
      );
    }
  },
};

/**
 * Serves public PDFs, images, and manifests from R2 with Cloudflare Edge caching
 * and byte-range support for fast progressive PDF viewing.
 */
async function handlePublicFileRead(request, env, ctx, url) {
  if (!env.R2_BUCKET) {
    return jsonResponse({ success: false, error: "R2_BUCKET binding missing" }, 500);
  }

  const rawKey = decodeURIComponent(url.pathname.slice("/files/".length));
  const key = sanitizeObjectKey(rawKey);
  if (!key) {
    return jsonResponse({ success: false, error: "Invalid file key" }, 400);
  }

  // If R2.dev public bucket URL is enabled, optional direct redirect can save Worker egress,
  // or serve directly via Worker Edge Cache for custom Cache-Control & Content-Disposition.
  const isManifest = key.startsWith("manifests/");
  const isRangeRequest = request.headers.has("range");
  const forceDownload = url.searchParams.get("download") === "1";

  // Check Cloudflare Edge Cache for non-range GET requests
  const cache = caches.default;
  const cacheKey = new Request(url.toString(), { method: "GET" });
  if (request.method === "GET" && !isRangeRequest) {
    const cached = await cache.match(cacheKey);
    if (cached) {
      const hitHeaders = new Headers(cached.headers);
      hitHeaders.set("X-Edge-Cache", "HIT");
      applyCorsHeaders(hitHeaders);
      return new Response(cached.body, {
        status: cached.status,
        statusText: cached.statusText,
        headers: hitHeaders,
      });
    }
  }

  if (request.method === "HEAD") {
    const headObj = await env.R2_BUCKET.head(key);
    if (!headObj) {
      return new Response("Not Found", { status: 404, headers: corsHeaders() });
    }
    const headers = buildObjectHeaders(headObj, key, isManifest, forceDownload);
    return new Response(null, { status: 200, headers });
  }

  const r2GetOptions = {};
  if (isRangeRequest) {
    r2GetOptions.range = request.headers;
  }

  const object = await env.R2_BUCKET.get(key, r2GetOptions);
  if (!object) {
    return new Response("Not Found", { status: 404, headers: corsHeaders() });
  }

  const headers = buildObjectHeaders(object, key, isManifest, forceDownload);
  headers.set("X-Edge-Cache", "MISS");

  // Handle conditional ETag validation
  const ifNoneMatch = request.headers.get("if-none-match");
  if (ifNoneMatch && object.httpEtag && ifNoneMatch === object.httpEtag) {
    return new Response(null, { status: 304, headers });
  }

  const status = isRangeRequest && object.range ? 206 : 200;
  if (status === 206 && object.range) {
    const start = object.range.offset ?? 0;
    const length = object.range.length ?? object.size;
    const end = start + length - 1;
    headers.set("Content-Range", `bytes ${start}-${end}/${object.size}`);
    headers.set("Content-Length", String(length));
  } else if (typeof object.size === "number") {
    headers.set("Content-Length", String(object.size));
  }

  const response = new Response(object.body, { status, headers });

  // Cache full 200 responses at Cloudflare Edge
  if (status === 200) {
    ctx.waitUntil(cache.put(cacheKey, response.clone()));
  }

  return response;
}

function buildObjectHeaders(object, key, isManifest, forceDownload) {
  const headers = new Headers();
  object.writeHttpMetadata(headers);

  if (!headers.get("Content-Type")) {
    headers.set("Content-Type", inferMimeType(key));
  }

  // Immutable versioning for media & PDFs; short 5m TTL for mutable manifests
  if (isManifest) {
    headers.set("Cache-Control", "public, max-age=300, stale-while-revalidate=60");
  } else {
    headers.set("Cache-Control", "public, max-age=31536000, immutable");
  }

  if (object.httpEtag) {
    headers.set("ETag", object.httpEtag);
  }
  headers.set("Accept-Ranges", "bytes");

  const filename = key.split("/").pop() || "file";
  const disposition = forceDownload ? "attachment" : "inline";
  headers.set("Content-Disposition", `${disposition}; filename="${filename.replace(/"/g, "")}"`);
  applyCorsHeaders(headers);
  return headers;
}

/**
 * Handles Admin file uploads (multipart/form-data) to R2.
 * Supports replacing an existing R2 file cleanly when `replaceKey` or `replaceUrl` is supplied.
 */
async function handleAdminUpload(request, env, url, adminEmail) {
  if (!env.R2_BUCKET) {
    return jsonResponse({ success: false, error: "R2_BUCKET binding missing" }, 500);
  }

  const contentTypeHeader = request.headers.get("content-type") || "";
  if (!contentTypeHeader.includes("multipart/form-data")) {
    return jsonResponse(
      { success: false, error: "Expected multipart/form-data upload" },
      400
    );
  }

  const formData = await request.formData();
  const file = formData.get("file");
  if (!file || typeof file === "string") {
    return jsonResponse({ success: false, error: "Missing file field" }, 400);
  }

  if (file.size <= 0 || file.size > MAX_UPLOAD_BYTES) {
    return jsonResponse(
      { success: false, error: `File size must be between 1 byte and ${MAX_UPLOAD_BYTES / (1024 * 1024)} MB` },
      400
    );
  }

  const requestedFolder = String(formData.get("folder") || "misc").trim().toLowerCase();
  const folder = ALLOWED_FOLDERS.has(requestedFolder) ? requestedFolder : "misc";
  const title = String(formData.get("title") || file.name || "document").slice(0, 120);
  const replaceUrlOrKey = String(formData.get("replaceKey") || formData.get("replaceUrl") || "").trim();

  const rawFileName = file.name || "upload.bin";
  const fileMime = (file.type || inferMimeType(rawFileName)).toLowerCase();
  if (!ALLOWED_MIME_TYPES.has(fileMime)) {
    return jsonResponse(
      { success: false, error: `Unsupported file type: ${fileMime}` },
      415
    );
  }

  const ext = inferExtension(rawFileName, fileMime);
  const safeBase = sanitizeFilename(rawFileName.replace(/\.[^/.]+$/, "")) || "file";
  const now = new Date();
  const datePrefix = `${now.getUTCFullYear()}${String(now.getUTCMonth() + 1).padStart(2, "0")}`;
  const version = Date.now();
  const randomTag = crypto.randomUUID().slice(0, 8);
  const objectKey = `${folder}/${datePrefix}/${version}_${randomTag}_${safeBase}.${ext}`;

  await env.R2_BUCKET.put(objectKey, file.stream(), {
    httpMetadata: {
      contentType: fileMime,
      cacheControl: "public, max-age=31536000, immutable",
    },
    customMetadata: {
      title,
      uploadedBy: adminEmail,
      uploadedAt: String(version),
      originalFilename: rawFileName.slice(0, 120),
    },
  });

  // Delete replaced R2 file if provided & valid
  let replacedDeleted = false;
  if (replaceUrlOrKey) {
    const oldKey = extractKeyFromUrlOrKey(replaceUrlOrKey);
    if (oldKey && oldKey !== objectKey) {
      await env.R2_BUCKET.delete(oldKey);
      await purgeEdgeCache(url.origin, oldKey);
      replacedDeleted = true;
    }
  }

  const publicUrl = buildPublicFileUrl(env, url.origin, objectKey);

  return jsonResponse({
    success: true,
    key: objectKey,
    url: publicUrl,
    size: file.size,
    contentType: fileMime,
    fileType: ext.toUpperCase() === "PDF" ? "PDF" : ext.toUpperCase() === "PNG" ? "PNG" : ext.toUpperCase() === "WEBP" ? "WEBP" : "JPG",
    version,
    replacedDeleted,
  });
}

/**
 * Deletes one or more R2 objects (by key or public URL).
 */
async function handleAdminDelete(request, env, url) {
  if (!env.R2_BUCKET) {
    return jsonResponse({ success: false, error: "R2_BUCKET binding missing" }, 500);
  }

  let body = {};
  try {
    body = await request.json();
  } catch {
    const keyParam = url.searchParams.get("key") || url.searchParams.get("url") || "";
    body = { key: keyParam };
  }

  const candidates = [];
  if (typeof body.key === "string" && body.key.trim()) candidates.push(body.key.trim());
  if (typeof body.url === "string" && body.url.trim()) candidates.push(body.url.trim());
  if (Array.isArray(body.keys)) candidates.push(...body.keys.map((x) => String(x || "").trim()));
  if (Array.isArray(body.urls)) candidates.push(...body.urls.map((x) => String(x || "").trim()));

  const keysToDelete = [
    ...new Set(
      candidates
        .map((c) => extractKeyFromUrlOrKey(c))
        .filter((k) => Boolean(k))
    ),
  ];

  if (keysToDelete.length === 0) {
    return jsonResponse({ success: false, error: "No valid R2 keys provided" }, 400);
  }

  await env.R2_BUCKET.delete(keysToDelete);
  await Promise.all(keysToDelete.map((k) => purgeEdgeCache(url.origin, k)));

  return jsonResponse({
    success: true,
    deletedCount: keysToDelete.length,
    deletedKeys: keysToDelete,
  });
}

/**
 * Lists files in R2 bucket for Admin management.
 */
async function handleAdminListFiles(env, url) {
  if (!env.R2_BUCKET) {
    return jsonResponse({ success: false, error: "R2_BUCKET binding missing" }, 500);
  }

  const prefix = url.searchParams.get("prefix") || undefined;
  const cursor = url.searchParams.get("cursor") || undefined;
  const limit = Math.min(Math.max(Number(url.searchParams.get("limit") || "100"), 1), 500);

  const listing = await env.R2_BUCKET.list({
    prefix,
    cursor,
    limit,
    include: ["httpMetadata", "customMetadata"],
  });

  const files = listing.objects.map((obj) => ({
    key: obj.key,
    url: buildPublicFileUrl(env, url.origin, obj.key),
    size: obj.size,
    uploaded: obj.uploaded?.toISOString() || null,
    contentType: obj.httpMetadata?.contentType || null,
    customMetadata: obj.customMetadata || {},
  }));

  return jsonResponse({
    success: true,
    files,
    truncated: listing.truncated,
    cursor: listing.truncated ? listing.cursor : null,
  });
}

/**
 * Publishes static JSON manifests / app update metadata directly into R2.
 */
async function handlePublishManifest(request, env, url, adminEmail) {
  if (!env.R2_BUCKET) {
    return jsonResponse({ success: false, error: "R2_BUCKET binding missing" }, 500);
  }

  const body = await request.json();
  const name = sanitizeFilename(String(body.name || "manifest")).replace(/\.json$/i, "");
  const payload = body.data ?? body;
  const objectKey = `manifests/${name}.json`;
  const jsonText = JSON.stringify({
    updatedAt: Date.now(),
    updatedBy: adminEmail,
    data: payload,
  });

  await env.R2_BUCKET.put(objectKey, jsonText, {
    httpMetadata: {
      contentType: "application/json; charset=utf-8",
      cacheControl: "public, max-age=300, stale-while-revalidate=60",
    },
    customMetadata: {
      uploadedBy: adminEmail,
      uploadedAt: String(Date.now()),
    },
  });

  await purgeEdgeCache(url.origin, objectKey);

  return jsonResponse({
    success: true,
    key: objectKey,
    url: buildPublicFileUrl(env, url.origin, objectKey),
  });
}

/**
 * Cryptographically verifies Firebase ID Token JWT signature & admin email claims.
 */
async function verifyAdminFirebaseToken(request, env) {
  const authHeader = request.headers.get("authorization") || "";
  if (!authHeader.startsWith("Bearer ")) {
    return { ok: false, status: 401, error: "Missing Bearer authorization token" };
  }

  const token = authHeader.slice("Bearer ".length).trim();
  const parts = token.split(".");
  if (parts.length !== 3) {
    return { ok: false, status: 401, error: "Invalid JWT structure" };
  }

  const [headerB64, payloadB64, signatureB64] = parts;
  let header;
  let payload;
  try {
    header = JSON.parse(decodeBase64UrlToString(headerB64));
    payload = JSON.parse(decodeBase64UrlToString(payloadB64));
  } catch {
    return { ok: false, status: 401, error: "Malformed JWT payload" };
  }

  if (header.alg !== "RS256" || !header.kid) {
    return { ok: false, status: 401, error: "Unsupported JWT algorithm" };
  }

  const projectId = (env.FIREBASE_PROJECT_ID || "gen-lang-client-0268244143").trim();
  const nowSec = Math.floor(Date.now() / 1000);

  if (payload.aud !== projectId) {
    return { ok: false, status: 403, error: "Token audience mismatch" };
  }
  if (payload.iss !== `https://securetoken.google.com/${projectId}`) {
    return { ok: false, status: 403, error: "Token issuer mismatch" };
  }
  if (typeof payload.exp !== "number" || payload.exp <= nowSec) {
    return { ok: false, status: 401, error: "Firebase ID token expired" };
  }
  if (typeof payload.iat !== "number" || payload.iat > nowSec + 300) {
    return { ok: false, status: 401, error: "Token issued-at invalid" };
  }
  if (!payload.sub || typeof payload.sub !== "string") {
    return { ok: false, status: 401, error: "Missing token subject" };
  }

  const jwk = await getGoogleSigningJwk(header.kid);
  if (!jwk) {
    return { ok: false, status: 401, error: "Matching Google public key not found" };
  }

  const cryptoKey = await crypto.subtle.importKey(
    "jwk",
    jwk,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"]
  );

  const signingInput = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
  const signatureBytes = decodeBase64UrlToUint8Array(signatureB64);
  const validSignature = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    signatureBytes,
    signingInput
  );

  if (!validSignature) {
    return { ok: false, status: 401, error: "Invalid Firebase token signature" };
  }

  const email = String(payload.email || "").trim().toLowerCase();
  const isVerified = payload.email_verified === true;
  if (!email || !isVerified) {
    return { ok: false, status: 403, error: "Verified Google email required" };
  }

  const allowedEmails = String(env.ADMIN_EMAILS || "satyamprakash100@gmail.com")
    .split(",")
    .map((e) => e.trim().toLowerCase())
    .filter(Boolean);

  if (!allowedEmails.includes(email)) {
    return { ok: false, status: 403, error: "Unauthorized administrator account" };
  }

  return { ok: true, email, uid: payload.sub };
}

async function getGoogleSigningJwk(kid) {
  const now = Date.now();
  if (!cachedJwks || now >= cachedJwksExpiresAt) {
    const resp = await fetch(GOOGLE_JWKS_URL);
    if (!resp.ok) {
      throw new Error(`Failed to fetch Google JWKS: ${resp.status}`);
    }
    const cacheControl = resp.headers.get("cache-control") || "";
    const maxAgeMatch = /max-age=(\d+)/i.exec(cacheControl);
    const maxAgeMs = maxAgeMatch ? Number(maxAgeMatch[1]) * 1000 : 3600 * 1000;
    const data = await resp.json();
    cachedJwks = Array.isArray(data.keys) ? data.keys : [];
    cachedJwksExpiresAt = now + Math.max(maxAgeMs, 60 * 1000);
  }
  return cachedJwks.find((k) => k.kid === kid) || null;
}

function buildPublicFileUrl(env, workerOrigin, objectKey) {
  const publicBase = (env.R2_PUBLIC_BASE_URL || "").trim().replace(/\/+$/, "");
  if (publicBase) {
    return `${publicBase}/${objectKey}`;
  }
  return `${workerOrigin.replace(/\/+$/, "")}/files/${objectKey}`;
}

function extractKeyFromUrlOrKey(raw) {
  const value = String(raw || "").trim();
  if (!value) return "";

  if (!value.startsWith("http://") && !value.startsWith("https://")) {
    return sanitizeObjectKey(value);
  }

  try {
    const parsed = new URL(value);
    const pathname = decodeURIComponent(parsed.pathname || "");
    if (pathname.startsWith("/files/")) {
      return sanitizeObjectKey(pathname.slice("/files/".length));
    }
    if (parsed.hostname.endsWith(".r2.dev")) {
      return sanitizeObjectKey(pathname.replace(/^\/+/, ""));
    }
    return "";
  } catch {
    return "";
  }
}

function sanitizeObjectKey(rawKey) {
  const cleaned = String(rawKey || "")
    .replace(/\\/g, "/")
    .replace(/^\/+/, "")
    .replace(/\.\.+/g, "");
  if (!cleaned || cleaned.includes("..")) return "";
  return cleaned;
}

function sanitizeFilename(name) {
  return String(name || "")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 48);
}

function inferMimeType(filename) {
  const lower = String(filename || "").toLowerCase();
  if (lower.endsWith(".pdf")) return "application/pdf";
  if (lower.endsWith(".png")) return "image/png";
  if (lower.endsWith(".webp")) return "image/webp";
  if (lower.endsWith(".gif")) return "image/gif";
  if (lower.endsWith(".json")) return "application/json";
  return "image/jpeg";
}

function inferExtension(filename, mimeType) {
  const match = /\.([a-zA-Z0-9]{2,5})$/.exec(String(filename || ""));
  if (match) {
    const ext = match[1].toLowerCase();
    if (["pdf", "jpg", "jpeg", "png", "webp", "gif", "json"].includes(ext)) {
      return ext === "jpeg" ? "jpg" : ext;
    }
  }
  if (mimeType.includes("pdf")) return "pdf";
  if (mimeType.includes("png")) return "png";
  if (mimeType.includes("webp")) return "webp";
  if (mimeType.includes("gif")) return "gif";
  if (mimeType.includes("json")) return "json";
  return "jpg";
}

async function purgeEdgeCache(origin, objectKey) {
  try {
    const cache = caches.default;
    const targetUrl = `${origin.replace(/\/+$/, "")}/files/${objectKey}`;
    await cache.delete(new Request(targetUrl, { method: "GET" }));
  } catch {
    // Best-effort cache purge
  }
}

function decodeBase64UrlToUint8Array(b64url) {
  const b64 = b64url.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - (b64url.length % 4)) % 4);
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function decodeBase64UrlToString(b64url) {
  const bytes = decodeBase64UrlToUint8Array(b64url);
  return new TextDecoder().decode(bytes);
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, HEAD, POST, DELETE, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization, Range",
    "Access-Control-Max-Age": "86400",
  };
}

function applyCorsHeaders(headers) {
  const cors = corsHeaders();
  for (const [k, v] of Object.entries(cors)) {
    headers.set(k, v);
  }
}

function jsonResponse(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...corsHeaders(),
    },
  });
}
