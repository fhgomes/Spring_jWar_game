/**
 * Test-user factory for the Playwright suite.
 *
 * Test users are provisioned via the Firebase Auth Emulator REST API
 * (see https://firebase.google.com/docs/emulator-suite/connect_auth#rest)
 * so the tests never touch the real Firebase project.
 *
 * Until the emulator is wired into docker-compose, the `provisionUser`
 * helper short-circuits to a stubbed account and emits a console warning.
 * The shape stays stable so the Page Object Models below can already
 * consume it.
 */

export interface TestUser {
  uid: string;
  email: string;
  password: string;
  displayName: string;
}

const FIREBASE_EMULATOR_HOST =
  process.env.FIREBASE_EMULATOR_HOST ?? "localhost:9099";

const FB_PROJECT_ID = process.env.FB_PROJECT_ID ?? "jwar-local";

/**
 * Deterministic seed for repeatable test runs. Bumping the suffix below
 * forces a new batch of test accounts.
 */
const SEED = "v1";

function makeUser(slot: string): TestUser {
  const uid = `e2e-${SEED}-${slot}`;
  return {
    uid,
    email: `${uid}@example.test`,
    password: `Password!${slot}-${SEED}`,
    displayName: `E2E User ${slot.toUpperCase()}`,
  };
}

export const TEST_USERS = {
  host: makeUser("host"),
  guest: makeUser("guest"),
  spectator: makeUser("spectator"),
} as const satisfies Record<string, TestUser>;

/**
 * Create (or upsert) a user inside the Firebase Auth Emulator.
 *
 * TODO(testing-strategy spec 012, FR-003): switch this to the real
 * emulator REST call once the emulator is part of the local stack.
 * Reference endpoint:
 *   POST http://<emulator>/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key
 * with body `{ "email": ..., "password": ..., "returnSecureToken": true }`.
 */
export async function provisionUser(user: TestUser): Promise<TestUser> {
  if (process.env.PLAYWRIGHT_SKIP_PROVISIONING === "1") {
    return user;
  }

  const url =
    `http://${FIREBASE_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/` +
    `accounts:signUp?key=fake-api-key`;

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        email: user.email,
        password: user.password,
        returnSecureToken: true,
      }),
    });

    if (!res.ok && res.status !== 400) {
      // 400 here typically means "EMAIL_EXISTS" -- acceptable on rerun.
      const body = await res.text();
      throw new Error(
        `Firebase emulator signUp failed (${res.status}): ${body}`
      );
    }
  } catch (err) {
    // Surface a clear hint when the emulator isn't running; don't crash
    // the test setup so suites that don't need real auth can still run.
    console.warn(
      `[e2e/fixtures/users] could not reach Firebase emulator at ` +
        `${FIREBASE_EMULATOR_HOST} (project=${FB_PROJECT_ID}). ` +
        `Auth tests will be skipped. Cause: ${(err as Error).message}`
    );
  }

  return user;
}

/**
 * Clear all users in the emulator. Call from `globalTeardown` to keep
 * runs independent.
 */
export async function resetEmulatorUsers(): Promise<void> {
  const url =
    `http://${FIREBASE_EMULATOR_HOST}/emulator/v1/projects/` +
    `${FB_PROJECT_ID}/accounts`;
  try {
    await fetch(url, { method: "DELETE" });
  } catch {
    // best-effort
  }
}
