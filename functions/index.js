/**
 * Cloud Functions for Shikshak Sathi Bihar
 * Services:
 * 1. Protected Admin Content Publishing & Audit Logging
 * 2. Rate-limited & App Check guarded AI Application Writer (gemini-3.8-flash)
 * 3. Instant FCM Topic Push Triggers on Firestore updates (named DB)
 */

const functions = require("firebase-functions/v1");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { GoogleGenerativeAI } = require("@google/generative-ai");

admin.initializeApp();

const DATABASE_ID = "ai-studio-android-shikshak-dd2aa215-bf07-4d91-a7ca-4cdbcad45c7d";
const db = getFirestore(DATABASE_ID);
const messaging = getMessaging();

const SUPER_ADMIN_EMAIL = "satyamprakash100@gmail.com";
const NOTIFICATION_CHANNEL_ID = "shikshak_sathi_admin_broadcast_v3";

/**
 * Validates verified administrator identity or admin_users directory role.
 */
async function assertAdminContext(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Admin authentication required");
  }
  const email = (context.auth.token.email || "").trim().toLowerCase();
  const isVerified = context.auth.token.email_verified === true;
  if (isVerified && email === SUPER_ADMIN_EMAIL) {
    return { uid: context.auth.uid, email };
  }
  const adminSnap = await db.collection("admin_users").doc(context.auth.uid).get();
  if (!adminSnap.exists || !["SUPER_ADMIN", "ADMIN"].includes(adminSnap.data().role)) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized admin action");
  }
  return { uid: context.auth.uid, email: adminSnap.data().email || email };
}

function clampString(val, maxLen) {
  return String(val || "").trim().slice(0, maxLen);
}

/**
 * Limited AI Application Writer Endpoint
 * Securely calls Gemini 3.8 Flash on server-side with App Check, input caps & daily device quotas.
 */
exports.generateOfficialApplication = functions
  .runWith({ enforceAppCheck: true, timeoutSeconds: 30 })
  .https.onCall(async (data) => {
    const deviceId = clampString(data?.deviceId || "unknown", 128).replace(/[^a-zA-Z0-9_-]/g, "_");
    const recipient = clampString(data?.recipient, 200);
    const appType = clampString(data?.applicationType, 120);
    const reason = clampString(data?.reason, 1200);
    const dates = clampString(data?.dates, 200);
    const applicantName = clampString(data?.applicantName, 160);
    const designation = clampString(data?.designation, 160);
    const schoolName = clampString(data?.schoolName, 200);
    const district = clampString(data?.district, 120);
    const language = data?.language === "en" ? "en" : "hi";

    if (!recipient || !appType || !reason) {
      throw new functions.https.HttpsError("invalid-argument", "Required application fields missing.");
    }

    const aiConfigSnap = await db.collection("ai_config").doc("global").get();
    const aiConfig = aiConfigSnap.exists ? aiConfigSnap.data() : { isEnabled: true, dailyLimitPerDevice: 10 };

    if (aiConfig.isEnabled === false) {
      throw new functions.https.HttpsError("unavailable", "AI Service is temporarily disabled by administrator.");
    }

    const todayStr = new Date().toISOString().split("T")[0];
    const usageRef = db.collection("ai_usage").doc(`${deviceId}_${todayStr}`);
    const usageSnap = await usageRef.get();
    const currentCount = usageSnap.exists ? (usageSnap.data().count || 0) : 0;

    if (currentCount >= (aiConfig.dailyLimitPerDevice || 10)) {
      throw new functions.https.HttpsError("resource-exhausted", "Daily AI application drafting quota reached for this device.");
    }

    await usageRef.set({
      deviceId,
      date: todayStr,
      count: FieldValue.increment(1),
      lastUsedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      throw new functions.https.HttpsError("internal", "AI key configuration error.");
    }

    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ model: "gemini-3.8-flash" });

    const prompt = `You are an official letter drafter for Bihar government school teachers.
Write a formal, standard official application with exact government etiquette.
Recipient: ${recipient}
Application Type: ${appType}
Reason: ${reason}
Dates: ${dates}
Teacher Details: ${applicantName}, ${designation}, ${schoolName}, ${district}
Language: ${language === "hi" ? "Standard Administrative Hindi" : "English"}

Output exact format:
SUBJECT: <One line precise subject>
BODY:
<Detailed formal body text>`;

    const result = await model.generateContent(prompt);
    const responseText = result.response.text();
    return { result: responseText };
  });

/**
 * Publish Notice with Automatic Audit Log
 */
exports.publishNotice = functions.https.onCall(async (data, context) => {
  const adminActor = await assertAdminContext(context);

  const title = clampString(data?.title, 500);
  const summary = clampString(data?.summary, 4000);
  const category = clampString(data?.category || "सरकारी आदेश", 120);
  const issueDate = clampString(data?.issueDate, 64);

  if (!title) {
    throw new functions.https.HttpsError("invalid-argument", "Notice title is required.");
  }

  const noticeData = {
    title,
    summary,
    category,
    issueDate,
    department: clampString(data?.department || "शिक्षा विभाग, बिहार सरकार", 300),
    orderNumber: clampString(data?.orderNumber, 120),
    officialUrl: clampString(data?.officialUrl, 1000),
    isPublished: true,
    publishedBy: adminActor.uid,
    createdAt: FieldValue.serverTimestamp(),
    timestamp: FieldValue.serverTimestamp(),
  };

  const batch = db.batch();
  const docRef = db.collection("notices").doc();
  batch.set(docRef, noticeData);
  batch.set(
    db.collection("app_meta").doc("sync_versions"),
    { notices_v: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() },
    { merge: true }
  );
  batch.set(db.collection("audit_logs").doc(), {
    adminUid: adminActor.uid,
    adminEmail: adminActor.email,
    action: "PUBLISH_NOTICE",
    targetDocId: docRef.id,
    timestamp: FieldValue.serverTimestamp(),
  });
  await batch.commit();

  return { success: true, noticeId: docRef.id };
});

/**
 * Helper to dispatch high-priority FCM topic notifications and log delivery status.
 */
async function dispatchTopicPush({
  topic,
  title,
  body,
  section,
  contentType,
  contentId,
  targetSection,
  targetUrl,
  alertId,
}) {
  const safeTitle = clampString(title, 200) || "शिक्षक साथी बिहार";
  const safeBody = clampString(body, 600) || "नया विभागीय अपडेट उपलब्ध है।";

  const message = {
    topic,
    notification: {
      title: safeTitle,
      body: safeBody,
    },
    data: {
      alertId: clampString(alertId, 128),
      title: safeTitle,
      body: safeBody,
      section: clampString(section, 120),
      contentType: clampString(contentType, 64),
      contentId: clampString(contentId, 300),
      targetSection: clampString(targetSection, 64),
      targetUrl: clampString(targetUrl, 1000),
    },
    android: {
      priority: "high",
      ttl: 3600 * 1000,
      notification: {
        channelId: NOTIFICATION_CHANNEL_ID,
        sound: "default",
        defaultVibrateTimings: true,
      },
    },
  };

  const messageId = await messaging.send(message);
  await db.collection("notification_history").add({
    topic,
    title: safeTitle,
    body: safeBody,
    contentType: clampString(contentType, 64),
    targetSection: clampString(targetSection, 64),
    messageId,
    sentAt: FieldValue.serverTimestamp(),
  });
  return messageId;
}

/**
 * 1. Immediate FCM Push on Admin Broadcast Alert write
 */
exports.onBroadcastAlertWritten = onDocumentWritten(
  {
    document: "admin_broadcasts/latest_alert",
    database: DATABASE_ID,
  },
  async (event) => {
    const after = event.data?.after?.data();
    if (!after || !after.title) return null;

    const beforeAlertId = event.data?.before?.data()?.alertId;
    if (beforeAlertId && beforeAlertId === after.alertId) {
      return null;
    }

    const topic = after.contentType === "notice" ? "notices_urgent" : "teachers_all";
    return dispatchTopicPush({
      topic,
      title: after.title,
      body: after.details || after.section,
      section: after.section || "विभागीय अपडेट",
      contentType: after.contentType || "",
      contentId: after.contentId || "",
      targetSection: after.targetSection || "home",
      targetUrl: after.targetUrl || "",
      alertId: after.alertId || event.id,
    });
  }
);

/**
 * 2. Silent Data-Sync + Visible Push on Notice Creation/Update
 */
exports.onNoticeWrittenPush = onDocumentWritten(
  {
    document: "notices/{noticeId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const after = event.data?.after?.data();
    const isDelete = !after;
    if (isDelete) {
      await messaging.send({
        topic: "teachers_all",
        data: { syncSection: "notices", deletedId: event.params.noticeId },
        android: { priority: "high" },
      });
      return null;
    }
    if (!event.data?.before?.exists && after.isPublished !== false) {
      return dispatchTopicPush({
        topic: "notices_urgent",
        title: `नया विभागीय आदेश: ${after.title}`,
        body: after.summary || after.category,
        section: "विभागीय आदेश व सूचनाएं",
        contentType: "notice",
        contentId: after.orderNumber || after.title,
        targetSection: "notices",
        targetUrl: after.pdfUrl || after.officialUrl || "",
        alertId: `notice_${event.params.noticeId}`,
      });
    }
    return null;
  }
);

/**
 * 3. Silent Background Cache Refresh Signal on Banner or Holiday updates
 */
exports.onBannerWrittenSync = onDocumentWritten(
  {
    document: "promo_banners/{bannerId}",
    database: DATABASE_ID,
  },
  async (event) => {
    await messaging.send({
      topic: "teachers_all",
      data: {
        syncSection: "banners",
        bannerId: event.params.bannerId,
        deleted: String(!event.data?.after?.exists),
      },
      android: { priority: "high" },
    });
    return null;
  }
);

exports.onHolidayWrittenSync = onDocumentWritten(
  {
    document: "holiday_calendars/{calId}",
    database: DATABASE_ID,
  },
  async (event) => {
    await messaging.send({
      topic: "teachers_all",
      data: {
        syncSection: "holidays",
        calId: event.params.calId,
        deleted: String(!event.data?.after?.exists),
      },
      android: { priority: "high" },
    });
    return null;
  }
);
