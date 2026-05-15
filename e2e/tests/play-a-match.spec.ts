import { test, expect, type BrowserContext } from "@playwright/test";

import { GameBoardPage } from "../pages/GameBoardPage";
import { LobbyPage } from "../pages/LobbyPage";
import { LoginPage } from "../pages/LoginPage";
import { RoomPage } from "../pages/RoomPage";
import { TEST_USERS, provisionUser } from "../fixtures/users";

/**
 * Golden-path multiplayer match.
 *
 * Two browser contexts (host + guest) sign in, the host creates a room,
 * the guest joins, the host starts the match, and each plays one full
 * turn. Each side asserts the WebSocket-driven state updates show up
 * on the *opposite* context's UI.
 *
 * Skipped until:
 *   1. Firebase Auth Emulator is wired into the compose stack.
 *   2. The frontend exposes the `data-testid` attributes the Page
 *      Object Models below rely on (see pages/*.ts).
 *   3. spec 002 (game mechanics) ships so the engine accepts the
 *      add-attack-move-end flow this test exercises.
 */
test.describe("Play a match (two players, one turn each)", () => {
  test.skip(
    !process.env.FIREBASE_EMULATOR_HOST,
    "Multi-player E2E requires the Firebase Auth Emulator and frontend " +
      "testids -- see TODOs in e2e/fixtures/users.ts and e2e/pages/*.ts."
  );

  let hostContext: BrowserContext;
  let guestContext: BrowserContext;

  test.beforeAll(async ({ browser }) => {
    hostContext = await browser.newContext();
    guestContext = await browser.newContext();
    await provisionUser(TEST_USERS.host);
    await provisionUser(TEST_USERS.guest);
  });

  test.afterAll(async () => {
    await hostContext.close();
    await guestContext.close();
  });

  test("host + guest play one full turn each", async () => {
    const hostPage = await hostContext.newPage();
    const guestPage = await guestContext.newPage();

    const hostLogin = new LoginPage(hostPage);
    const guestLogin = new LoginPage(guestPage);
    const hostLobby = new LobbyPage(hostPage);
    const guestLobby = new LobbyPage(guestPage);
    const hostRoom = new RoomPage(hostPage);
    const guestRoom = new RoomPage(guestPage);
    const hostBoard = new GameBoardPage(hostPage);
    const guestBoard = new GameBoardPage(guestPage);

    let roomId = "";

    await test.step("both players sign in", async () => {
      await hostLogin.login(TEST_USERS.host);
      await guestLogin.login(TEST_USERS.guest);
      await hostLobby.expectVisible();
      await guestLobby.expectVisible();
    });

    await test.step("host creates a room", async () => {
      roomId = await hostLobby.createRoom("e2e-match");
      expect(roomId).toBeTruthy();
      await hostRoom.expectPlayerCount(1);
    });

    await test.step("guest joins the same room", async () => {
      await guestRoom.gotoById(roomId);
      await Promise.all([
        hostRoom.expectPlayerCount(2),
        guestRoom.expectPlayerCount(2),
      ]);
    });

    await test.step("host starts the match -- both navigate to the board", async () => {
      await hostRoom.startMatch();
      await guestPage.waitForURL(/\/games\/[^/]+$/);
      await hostBoard.expectVisible();
      await guestBoard.expectVisible();
    });

    await test.step("turn 1: host adds, attacks, moves, ends turn", async () => {
      await hostBoard.expectCurrentPlayer(TEST_USERS.host.displayName);
      await hostBoard.expectPhase("ADD_TROOPS");

      // TODO(spec 002): use deterministic dice / mock starting countries so
      // the test can hard-code valid country names and attack outcomes.
      await hostBoard.endPhase(); // ADD_TROOPS -> ATTACK
      await hostBoard.expectPhase("ATTACK");

      await hostBoard.endPhase(); // ATTACK -> MOVE (no-op attack for now)
      await hostBoard.expectPhase("MOVE");

      await hostBoard.endTurn();

      // Guest's UI must reflect the turn change via the STOMP push.
      await guestBoard.expectCurrentPlayer(TEST_USERS.guest.displayName);
    });

    await test.step("turn 2: guest plays one full turn", async () => {
      await guestBoard.expectPhase("ADD_TROOPS");
      await guestBoard.endPhase();
      await guestBoard.endPhase();
      await guestBoard.endTurn();

      // Back to host.
      await hostBoard.expectCurrentPlayer(TEST_USERS.host.displayName);
    });
  });
});
