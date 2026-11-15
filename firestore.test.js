const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date(Date.now() - 1000);
}

function validWebsitePayload(uid = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    title: "Ekativa Gold",
    heading: "Welcome to My Shop",
    content: "Best Gold Chains in Kerala with certified purity.",
    themeFinish: "GOLD",
    generatedHtml: "<html><body><h1>Welcome to My Shop</h1></body></html>",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validScriptPayload(uid = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    name: "gold_calc.py",
    code: "print('Ekativa Gold Studio')",
    lastOutput: "Ekativa Gold Studio",
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated user: cannot list or write websites", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/websites`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/websites/site_1`).set(validWebsitePayload()));
});

test("Unauthenticated user: cannot list or write scripts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/scripts`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/scripts/script_1`).set(validScriptPayload()));
});

test("Authenticated owner: can create, read, update, and delete own website", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.doc(`users/${ALICE_UID}/websites/site_1`);
  await assertSucceeds(ref.set(validWebsitePayload(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/websites`).get());
  await assertSucceeds(
    ref.update({
      title: "Ekativa Platinum",
      themeFinish: "PLATINUM",
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(ref.delete());
});

test("Authenticated owner: can create, read, update, and delete own script", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.doc(`users/${ALICE_UID}/scripts/script_1`);
  await assertSucceeds(ref.set(validScriptPayload(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(
    ref.update({
      code: "print(2 + 2)",
      lastOutput: "4",
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(ref.delete());
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's website or script", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/websites/site_1`).set(validWebsitePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/scripts/script_1`).set(validScriptPayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/websites/site_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/websites`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/websites/site_2`).set(validWebsitePayload(BOB_UID)));
  await assertFails(bobDb.doc(`users/${ALICE_UID}/websites/site_1`).update({ title: "Hacked", updatedAt: nowTimestamp() }));
  await assertFails(bobDb.doc(`users/${ALICE_UID}/websites/site_1`).delete());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/scripts/script_1`).get());
});

test("Dirty payloads: rejects ghost fields, invalid themeFinish, spoofed userId, and immutable createdAt change", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Ghost field on create
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/websites/site_ghost`).set({
      ...validWebsitePayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // Invalid themeFinish enum
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/websites/site_enum`).set({
      ...validWebsitePayload(ALICE_UID),
      themeFinish: "DIAMOND",
    })
  );

  // Spoofed userId
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/websites/site_spoof`).set(validWebsitePayload(BOB_UID))
  );

  // Immutable createdAt mutation on update
  const ref = aliceDb.doc(`users/${ALICE_UID}/websites/site_ok`);
  await assertSucceeds(ref.set(validWebsitePayload(ALICE_UID)));
  await assertFails(
    ref.update({
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
});
