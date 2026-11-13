# Shikshak Sathi Bihar — Cloudflare R2 & Workers Backend Setup

This Cloudflare Worker serves as the secure storage & CDN API for **Shikshak Sathi Bihar**.
Because no custom domain is required, it works cleanly on Cloudflare's free `*.workers.dev` URL (and optionally a `*.r2.dev` public bucket URL).

## Architecture & Security
- **Zero Secrets in APK**: R2 access uses a native Cloudflare Worker R2 Bucket Binding (`R2_BUCKET`). No Cloudflare API tokens or R2 S3 access/secret keys are embedded anywhere in the Android app.
- **Admin Authorization**: All write/upload/delete/list routes (`/api/admin/*`) verify the caller's Firebase ID Token (`Authorization: Bearer <token>`) against Google's JWKS and allow only verified emails in `ADMIN_EMAILS` (`satyamprakash100@gmail.com`).
- **Normal Users**: Teacher devices require **no login** to read images, PDFs, or manifests. Files are served via `/files/<key>` with `Cache-Control: public, max-age=31536000, immutable`, Edge caching (`caches.default`), `ETag`, and HTTP `Range` support.

## Deployment Steps (Free `workers.dev` — No Custom Domain Needed)

1. Install dependencies and authenticate Wrangler:
   ```bash
   cd cloudflare-worker
   npm install
   npx wrangler login
   ```

2. Create the Cloudflare R2 bucket:
   ```bash
   npx wrangler r2 bucket create shikshak-sathi-media
   ```

3. Deploy the Worker:
   ```bash
   npx wrangler deploy
   ```
   Wrangler will print your free Worker URL, e.g.:
   `https://shikshak-sathi-r2-worker.<your-subdomain>.workers.dev`

4. (Optional — Direct Bucket CDN Reads) In the Cloudflare Dashboard → R2 → `shikshak-sathi-media` → **Settings** → **Public Development URL**, enable `r2.dev`, copy the `https://pub-xxxx.r2.dev` URL into `R2_PUBLIC_BASE_URL` in `wrangler.toml`, and re-run `npx wrangler deploy`.
   - If you skip step 4, `/files/<key>` on your Worker URL serves and Edge-caches all files automatically!

5. Set your deployed Worker URL in the Android app (`app/build.gradle.kts` → `R2_WORKER_BASE_URL`) and rebuild the APK.
