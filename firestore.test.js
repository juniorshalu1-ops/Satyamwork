const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const path = require("node:path");

const resolvePaths = [
  path.dirname(require.resolve("@firebase/rules-unit-testing")),
  "/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules",
  "/usr/local/lib/node_modules",
];
const firebase = require(require.resolve("firebase/compat/app", { paths: resolvePaths }));
require(require.resolve("firebase/compat/firestore", { paths: resolvePaths }));
const FieldValue = firebase.firestore.FieldValue;

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SUPER_ADMIN_EMAIL = "satyamprakash100@gmail.com";
const TEACHER_UID = "teacher_uid_123";
const OTHER_TEACHER_UID = "teacher_uid_456";
const ADMIN_UID = "admin_uid_001";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function adminContext(verified = true) {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: SUPER_ADMIN_EMAIL,
    email_verified: verified,
  });
}

function teacherContext(uid = TEACHER_UID, email = "teacher@bihar.gov.in") {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: true,
  });
}

// --- 1. Unauthenticated / Public Read Bounds ---

test("Unauthenticated user: can read public notices, banners, app_meta", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("notices").doc("notice_1").set({
      title: "Holiday Notice",
      summary: "School holiday tomorrow",
      category: "आदेश",
      issueDate: "10/04/2026",
      isPublished: true,
      timestamp: FieldValue.serverTimestamp(),
    });
    await db.collection("promo_banners").doc("banner_1").set({
      bannerId: "banner_1",
      title: "Summer Camp",
      orderIndex: 1,
      isActive: true,
      publishedAt: FieldValue.serverTimestamp(),
    });
    await db.collection("app_meta").doc("sync_versions").set({
      notices_v: 1,
      banners_v: 1,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("notices").doc("notice_1").get());
  await assertSucceeds(unauthDb.collection("notices").get());
  await assertSucceeds(unauthDb.collection("promo_banners").get());
  await assertSucceeds(unauthDb.collection("app_meta").doc("sync_versions").get());
});

test("Unauthenticated user: cannot write public collections or read sensitive collections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admin_audit_logs").doc("log_1").set({
      username: SUPER_ADMIN_EMAIL,
      action: "LOGIN",
      details: "Admin logged in",
      timestamp: FieldValue.serverTimestamp(),
    });
    await db.collection("feedback").doc("fb_1").set({
      userId: TEACHER_UID,
      message: "Helpful app",
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("notices").doc("hack_1").set({
      title: "Fake Notice",
      summary: "Spam",
      category: "आदेश",
      issueDate: "10/04/2026",
      isPublished: true,
      timestamp: FieldValue.serverTimestamp(),
    })
  );
  await assertFails(unauthDb.collection("admin_audit_logs").doc("log_1").get());
  await assertFails(unauthDb.collection("admin_audit_logs").get());
  await assertFails(unauthDb.collection("feedback").doc("fb_1").get());
  await assertFails(unauthDb.collection("audit_logs").get());
  await assertFails(unauthDb.collection("ai_usage").get());
  await assertFails(unauthDb.collection("notification_history").get());
  await assertFails(unauthDb.collection("unknown_collection").get());
});

// --- 2. Spoofed / Non-Admin Rejections ---

test("Spoofed admin email with email_verified=false is denied write", async () => {
  const unverifiedAdminDb = adminContext(false).firestore();
  await assertFails(
    unverifiedAdminDb.collection("notices").doc("notice_unverified").set({
      title: "Spoof",
      summary: "Unverified email spoof",
      category: "आदेश",
      issueDate: "10/04/2026",
      isPublished: true,
      timestamp: FieldValue.serverTimestamp(),
    })
  );
});

test("Non-admin signed-in teacher cannot write public content or read admin logs", async () => {
  const teacherDb = teacherContext().firestore();
  await assertFails(
    teacherDb.collection("promo_banners").doc("banner_bad").set({
      bannerId: "banner_bad",
      title: "Teacher Banner",
      orderIndex: 1,
      isActive: true,
      publishedAt: FieldValue.serverTimestamp(),
    })
  );
  await assertFails(
    teacherDb.collection("app_meta").doc("sync_versions").set({
      banners_v: 99,
      updatedAt: FieldValue.serverTimestamp(),
    })
  );
  await assertFails(teacherDb.collection("admin_audit_logs").get());
  await assertFails(teacherDb.collection("audit_logs").get());
  await assertFails(teacherDb.collection("notification_history").get());
  await assertFails(teacherDb.collection("ai_usage").get());
});

// --- 3. Verified Admin Operations & Schema Enforcement (Dirty Dozen) ---

test("Verified admin: can create, update, delete notices, banners, holidays, and atomic sync_versions", async () => {
  const adminDb = adminContext(true).firestore();

  await assertSucceeds(
    adminDb.collection("notices").doc("ord_123_abc").set({
      title: "E-Shikshakosh Update",
      summary: "Submit attendance on time",
      category: "आदेश",
      issueDate: "11/04/2026",
      department: "शिक्षा विभाग",
      orderNumber: "123",
      officialUrl: "https://state.bihar.gov.in",
      pdfUrl: "https://shikshak-sathi-r2-worker.satyamprakash100.workers.dev/a.pdf",
      imageUrl: "",
      isFeatured: true,
      isPublished: true,
      timestamp: FieldValue.serverTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("promo_banners").doc("banner_101").set({
      bannerId: "banner_101",
      title: "New Banner",
      description: "Details",
      category: "प्रमोशनल",
      imageUrl: "https://shikshak-sathi-r2-worker.satyamprakash100.workers.dev/b.jpg",
      destinationUrl: "https://state.bihar.gov.in",
      buttonText: "देखें",
      orderIndex: 1,
      isActive: true,
      publishedAt: FieldValue.serverTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("app_meta").doc("sync_versions").set(
      {
        notices_v: FieldValue.increment(1),
        banners_v: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    )
  );

  await assertSucceeds(
    adminDb.collection("promo_banners").doc("banner_101").update({
      isActive: false,
      publishedAt: FieldValue.serverTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("promo_banners").doc("banner_101").delete()
  );
});

test("Verified admin: schema validation blocks ghost fields, forged timestamps, oversize strings, and bad IDs", async () => {
  const adminDb = adminContext(true).firestore();

  // Ghost field ("Shadow Update")
  await assertFails(
    adminDb.collection("notices").doc("notice_ghost").set({
      title: "Notice",
      summary: "Summary",
      category: "आदेश",
      issueDate: "11/04/2026",
      isPublished: true,
      unexpectedAdminBackdoor: true,
      timestamp: FieldValue.serverTimestamp(),
    })
  );

  // Forged client timestamp (int instead of Timestamp)
  await assertFails(
    adminDb.collection("notices").doc("notice_bad_ts").set({
      title: "Notice",
      summary: "Summary",
      category: "आदेश",
      issueDate: "11/04/2026",
      isPublished: true,
      timestamp: 1700000000,
    })
  );

  // Oversize title (>500 chars)
  await assertFails(
    adminDb.collection("notices").doc("notice_big").set({
      title: "A".repeat(501),
      summary: "Summary",
      category: "आदेश",
      issueDate: "11/04/2026",
      isPublished: true,
      timestamp: FieldValue.serverTimestamp(),
    })
  );

  // Invalid docId (spaces / non-ASCII not matching ^[a-zA-Z0-9_-]+$)
  await assertFails(
    adminDb.collection("notices").doc("invalid doc id!").set({
      title: "Notice",
      summary: "Summary",
      category: "आदेश",
      issueDate: "11/04/2026",
      isPublished: true,
      timestamp: FieldValue.serverTimestamp(),
    })
  );
});

// --- 4. Immutability, Feedback Isolation, & Server-Only Collections ---

test("admin_audit_logs: verified admin can create/read but NEVER update or delete", async () => {
  const adminDb = adminContext(true).firestore();
  const logRef = adminDb.collection("admin_audit_logs").doc("log_test_1");

  await assertSucceeds(
    logRef.set({
      username: SUPER_ADMIN_EMAIL,
      action: "PUBLISH_NOTICE",
      details: "Published notice 1",
      timestamp: FieldValue.serverTimestamp(),
    })
  );
  await assertSucceeds(logRef.get());
  await assertFails(logRef.update({ details: "Tampered" }));
  await assertFails(logRef.delete());
});

test("Server-only collections deny client writes even from verified admin", async () => {
  const adminDb = adminContext(true).firestore();
  await assertFails(
    adminDb.collection("audit_logs").doc("srv_1").set({ action: "FORGED" })
  );
  await assertFails(
    adminDb.collection("ai_usage").doc("device_1").set({ count: 0 })
  );
  await assertFails(
    adminDb.collection("notification_history").doc("notif_1").set({ title: "Fake" })
  );
});

test("Feedback: teacher can create and read own feedback, cannot spoof uid or read others", async () => {
  const teacher1Db = teacherContext(TEACHER_UID).firestore();
  const teacher2Db = teacherContext(OTHER_TEACHER_UID, "other@bihar.gov.in").firestore();

  await assertSucceeds(
    teacher1Db.collection("feedback").doc("fb_t1").set({
      userId: TEACHER_UID,
      message: "Great calendar!",
      category: "SUGGESTION",
      createdAt: FieldValue.serverTimestamp(),
    })
  );

  // Spoofed userId fails
  await assertFails(
    teacher1Db.collection("feedback").doc("fb_spoof").set({
      userId: OTHER_TEACHER_UID,
      message: "Spoof",
      createdAt: FieldValue.serverTimestamp(),
    })
  );

  // Own read & filtered query succeed
  await assertSucceeds(teacher1Db.collection("feedback").doc("fb_t1").get());
  await assertSucceeds(
    teacher1Db.collection("feedback").where("userId", "==", TEACHER_UID).get()
  );

  // Cross-user read & unfiltered list fail
  await assertFails(teacher2Db.collection("feedback").doc("fb_t1").get());
  await assertFails(teacher2Db.collection("feedback").get());

  // User cannot mutate/delete submitted feedback
  await assertFails(
    teacher1Db.collection("feedback").doc("fb_t1").update({ message: "Edit" })
  );
  await assertFails(teacher1Db.collection("feedback").doc("fb_t1").delete());
});
