import { test, expect } from "@playwright/test";

import { LobbyPage } from "../pages/LobbyPage";
import { LoginPage } from "../pages/LoginPage";
import { TEST_USERS, provisionUser } from "../fixtures/users";

/**
 * Auth golden path: sign-up -> logout -> login.
 *
 * Requires the Firebase Auth Emulator to be running. Until that wiring
 * lands (see TODO in fixtures/users.ts), these tests are tagged skip so
 * CI does not flap. Remove `test.skip` once the emulator is part of the
 * compose stack.
 */
test.describe("Auth golden path", () => {
  test.skip(
    !process.env.FIREBASE_EMULATOR_HOST,
    "Firebase Auth Emulator not configured -- see e2e/fixtures/users.ts TODO."
  );

  test.beforeAll(async () => {
    await provisionUser(TEST_USERS.host);
  });

  test("user can sign up, log out, and log back in", async ({ page }) => {
    const login = new LoginPage(page);
    const lobby = new LobbyPage(page);

    await test.step("sign-up reaches the lobby", async () => {
      await login.signUp(TEST_USERS.host);
      await lobby.expectVisible();
    });

    await test.step("logout returns to login screen", async () => {
      await lobby.logout();
      await login.expectVisible();
    });

    await test.step("login with existing credentials reaches the lobby", async () => {
      await login.login(TEST_USERS.host);
      await lobby.expectVisible();
    });
  });

  test("login form surfaces an error for bad credentials", async ({
    page,
  }) => {
    const login = new LoginPage(page);
    await login.goto();
    await login.emailInput.fill(TEST_USERS.host.email);
    await login.passwordInput.fill("not-the-right-password");
    await login.submitButton.click();
    await expect(login.errorBanner).toBeVisible();
  });
});
