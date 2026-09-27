import { expect, type Locator, type Page } from "@playwright/test";

/**
 * Page Object Model for the room (pre-match) screen.
 */
export class RoomPage {
  readonly page: Page;
  readonly playerList: Locator;
  readonly startMatchButton: Locator;
  readonly leaveRoomButton: Locator;
  readonly roomCode: Locator;

  constructor(page: Page) {
    this.page = page;
    this.playerList = page.getByTestId("room-player-list");
    this.startMatchButton = page.getByTestId("room-start-match");
    this.leaveRoomButton = page.getByTestId("room-leave");
    this.roomCode = page.getByTestId("room-code");
  }

  async gotoById(roomId: string): Promise<void> {
    await this.page.goto(`/rooms/${roomId}`);
  }

  async expectPlayerCount(n: number): Promise<void> {
    await expect(this.playerList.locator("[data-testid='room-player-row']"))
      .toHaveCount(n);
  }

  async startMatch(): Promise<void> {
    await this.startMatchButton.click();
    // After start we expect a navigation to the board.
    await this.page.waitForURL(/\/games\/[^/]+$/);
  }
}
