import { expect, type Locator, type Page } from "@playwright/test";

export type Phase = "ADD_TROOPS" | "ATTACK" | "MOVE";

/**
 * Page Object Model for the in-match game board.
 */
export class GameBoardPage {
  readonly page: Page;
  readonly currentTurnIndicator: Locator;
  readonly currentPhaseIndicator: Locator;
  readonly endTurnButton: Locator;
  readonly endPhaseButton: Locator;
  readonly attackButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.currentTurnIndicator = page.getByTestId("game-current-turn");
    this.currentPhaseIndicator = page.getByTestId("game-current-phase");
    this.endTurnButton = page.getByTestId("game-end-turn");
    this.endPhaseButton = page.getByTestId("game-end-phase");
    this.attackButton = page.getByTestId("game-attack-confirm");
  }

  async expectVisible(): Promise<void> {
    await expect(this.currentTurnIndicator).toBeVisible();
    await expect(this.currentPhaseIndicator).toBeVisible();
  }

  country(name: string): Locator {
    return this.page.getByTestId(`country-${name}`);
  }

  troopBadge(country: string): Locator {
    return this.country(country).getByTestId("country-troops");
  }

  async expectPhase(phase: Phase): Promise<void> {
    await expect(this.currentPhaseIndicator).toHaveAttribute(
      "data-phase",
      phase
    );
  }

  async expectCurrentPlayer(displayName: string): Promise<void> {
    await expect(this.currentTurnIndicator).toContainText(displayName);
  }

  /**
   * Click on a friendly country to add the currently-allotted troop.
   * Repeats `count` times.
   */
  async addTroops(country: string, count: number): Promise<void> {
    for (let i = 0; i < count; i += 1) {
      await this.country(country).click();
    }
  }

  async attack(from: string, to: string): Promise<void> {
    await this.country(from).click();
    await this.country(to).click();
    await this.attackButton.click();
  }

  async move(from: string, to: string, troops: number): Promise<void> {
    await this.country(from).click();
    await this.country(to).click();
    await this.page.getByTestId("game-move-slider").fill(String(troops));
    await this.page.getByTestId("game-move-confirm").click();
  }

  async endPhase(): Promise<void> {
    await this.endPhaseButton.click();
  }

  async endTurn(): Promise<void> {
    await this.endTurnButton.click();
  }
}
