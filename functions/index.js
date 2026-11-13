/**
 * Cloud Functions for Shikshak Sathi
 * Services:
 * 1. Admin Authentication & Username-to-Email mapping
 * 2. Protected Admin Content Publishing & Audit Logging
 * 3. Rate-limited AI Application Writer with daily quotas
 * 4. FCM Notifications
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { GoogleGenerativeAI } = require('@google/generative-ai');

admin.initializeApp();
const db = admin.firestore();

/**
 * Maps admin username (e.g. 'admin') to email address for Firebase Auth
 */
exports.adminUsernameLookup = functions.https.onCall(async (data, context) => {
  const username = (data.username || '').trim().toLowerCase();
  if (!username) {
    throw new functions.https.HttpsError('invalid-argument', 'Username is required');
  }

  // Look up in admin_users by username field
  const snap = await db.collection('admin_users')
    .where('username', '==', username)
    .limit(1)
    .get();

  if (snap.empty) {
    throw new functions.https.HttpsError('not-found', 'Admin user not found');
  }

  const adminDoc = snap.docs[0].data();
  return { email: adminDoc.email };
});

/**
 * Limited AI Application Writer Endpoint
 * Securely calls Gemini on server-side with quotas and official prompts
 */
exports.generateOfficialApplication = functions.https.onCall(async (data, context) => {
  const deviceId = (data.deviceId || 'unknown').trim();
  const recipient = (data.recipient || '').trim();
  const appType = (data.applicationType || '').trim();
  const reason = (data.reason || '').trim();
  const dates = (data.dates || '').trim();
  const applicantName = (data.applicantName || '').trim();
  const designation = (data.designation || '').trim();
  const schoolName = (data.schoolName || '').trim();
  const district = (data.district || '').trim();
  const language = data.language === 'en' ? 'en' : 'hi';

  // 1. Check AI global configuration
  const aiConfigSnap = await db.collection('ai_config').doc('global').get();
  const aiConfig = aiConfigSnap.exists ? aiConfigSnap.data() : { isEnabled: true, dailyLimitPerDevice: 10 };

  if (aiConfig.isEnabled === false) {
    throw new functions.https.HttpsError('unavailable', 'AI Service is temporarily disabled by administrator.');
  }

  // 2. Check Device Quota
  const todayStr = new Date().toISOString().split('T')[0];
  const usageRef = db.collection('ai_usage').doc(`${deviceId}_${todayStr}`);
  const usageSnap = await usageRef.get();
  const currentCount = usageSnap.exists ? (usageSnap.data().count || 0) : 0;

  if (currentCount >= (aiConfig.dailyLimitPerDevice || 10)) {
    throw new functions.https.HttpsError('resource-exhausted', 'Daily AI application drafting quota reached for this device.');
  }

  // 3. Increment quota atomically
  await usageRef.set({
    deviceId,
    date: todayStr,
    count: admin.firestore.FieldValue.increment(1),
    lastUsedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  // 4. Server-Side Gemini API Call
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new functions.https.HttpsError('internal', 'AI key configuration error.');
  }

  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });

  const prompt = `You are an official letter drafter for Bihar government school teachers.
Write a formal, standard official application with exact government etiquette.
Recipient: ${recipient}
Application Type: ${appType}
Reason: ${reason}
Dates: ${dates}
Teacher Details: ${applicantName}, ${designation}, ${schoolName}, ${district}
Language: ${language === 'hi' ? 'Standard Administrative Hindi' : 'English'}

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
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Admin authentication required');
  }

  const adminSnap = await db.collection('admin_users').doc(context.auth.uid).get();
  if (!adminSnap.exists || !['SUPER_ADMIN', 'ADMIN'].includes(adminSnap.data().role)) {
    throw new functions.https.HttpsError('permission-denied', 'Unauthorized admin action');
  }

  const noticeData = {
    title: data.title,
    summary: data.summary,
    category: data.category,
    issueDate: data.issueDate,
    department: data.department || 'शिक्षा विभाग, बिहार सरकार',
    orderNumber: data.orderNumber || '',
    officialUrl: data.officialUrl || '',
    isPublished: true,
    publishedBy: context.auth.uid,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  };

  const docRef = await db.collection('notices').add(noticeData);

  // Write immutable audit log
  await db.collection('audit_logs').add({
    adminUid: context.auth.uid,
    adminEmail: adminSnap.data().email || '',
    action: 'PUBLISH_NOTICE',
    targetDocId: docRef.id,
    timestamp: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, noticeId: docRef.id };
});
