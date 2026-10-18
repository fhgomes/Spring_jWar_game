import { expect, type Locator, type Page } from "@playwright/test";

/**
 * Page Object Model for the lobby / room list.
 */
export class LobbyPage {
  readonly page: Page;
  readonly createRoomButton: Locator;
  readonly roomNameInput: Locator;
  readonly confirmCreateButton: Locator;
  readonly logoutButton: Locator;
  readonly heading: Locator;

  constructor(page: Page) {
    this.page = page;
    this.createRoomButton = page.getByTestId("lobby-create-room");
    this.roomNameInput = page.getByTestId("lobby-room-name");
    this.confirmCreateButton = page.getByTestId("lobby-confirm-create");
    this.logoutButton = page.getByTestId("lobby-logout");
    this.heading = page.getByTestId("lobby-heading");
  }

  async goto(): Promise<void> {
    await this.page.goto("/lobby");
  }

  async expectVisible(): Promise<void> {
    await expect(this.heading).toBeVisible();
  }

  /**
   * Create a room and return the new room id from the URL
   * (we assume `/rooms/<id>` post-create).
   */
  async createRoom(name: string): Promise<string> {
    await this.createRoomButton.click();
    await this.roomNameInput.fill(name);
    await this.confirmCreateButton.click();

    await this.page.waitForURL(/\/rooms\/[^/]+$/);
    const match = this.page.url().match(/\/rooms\/([^/?#]+)/);
    if (!match) {
      throw new Error(`Could not extract room id from ${this.page.url()}`);
    }
    return match[1];
  }

  async joinRoomByCode(code: string): Promise<void> {
    await this.page.getByTestId("lobby-room-code").fill(code);
    await this.page.getByTestId("lobby-join-by-code").click();
  }

  async logout(): Promise<void> {
    await this.logoutButton.click();
    await this.page.waitForURL("**/login");
  }
}
