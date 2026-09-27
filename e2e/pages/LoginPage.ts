import { expect, type Locator, type Page } from "@playwright/test";

import type { TestUser } from "../fixtures/users";

/**
 * Page Object Model for the Login / Sign-up screen.
 *
 * NOTE: selectors below are TODOs until the auth screen ships. They use
 * stable `data-testid` attributes so the frontend agent can add them
 * without re-coupling tests to copy text.
 */
export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly displayNameInput: Locator;
  readonly submitButton: Locator;
  readonly switchToSignUp: Locator;
  readonly switchToLogin: Locator;
  readonly errorBanner: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.getByTestId("auth-email");
    this.passwordInput = page.getByTestId("auth-password");
    this.displayNameInput = page.getByTestId("auth-display-name");
    this.submitButton = page.getByTestId("auth-submit");
    this.switchToSignUp = page.getByTestId("auth-switch-to-signup");
    this.switchToLogin = page.getByTestId("auth-switch-to-login");
    this.errorBanner = page.getByTestId("auth-error");
  }

  async goto(): Promise<void> {
    await this.page.goto("/login");
  }

  async signUp(user: TestUser): Promise<void> {
    await this.goto();
    await this.switchToSignUp.click();
    await this.displayNameInput.fill(user.displayName);
    await this.emailInput.fill(user.email);
    await this.passwordInput.fill(user.password);
    await this.submitButton.click();
  }

  async login(user: TestUser): Promise<void> {
    await this.goto();
    await this.emailInput.fill(user.email);
    await this.passwordInput.fill(user.password);
    await this.submitButton.click();
  }

  async expectVisible(): Promise<void> {
    await expect(this.emailInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
    await expect(this.submitButton).toBeVisible();
  }
}
