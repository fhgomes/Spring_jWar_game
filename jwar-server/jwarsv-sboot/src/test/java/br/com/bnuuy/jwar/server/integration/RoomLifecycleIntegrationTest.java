package br.com.bnuuy.jwar.server.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Integration coverage for the room lifecycle (spec 005) and the STOMP
 * push surface (spec 006).
 *
 * <p>Acceptance scenarios this class targets (from spec 012 US2):</p>
 * <ul>
 *   <li>{@code POST /api/rooms} returns 201 and persists a row.</li>
 *   <li>{@code POST /api/rooms/&#123;id&#125;/join} returns 200 and adds
 *       a {@code room_memberships} row.</li>
 *   <li>{@code POST /api/rooms/&#123;id&#125;/start} registers the
 *       in-memory match with the engine.</li>
 *   <li>STOMP subscribers on {@code /topic/rooms/&#123;id&#125;}
 *       receive a "player joined" event within 5 seconds.</li>
 * </ul>
 *
 * <p>Disabled until specs 005 + 006 land. The skeleton documents the
 * intended structure so the implementing agent can flesh it out
 * without rediscovering the conventions.</p>
 */
@Import(FirebaseTestConfig.class)
@Disabled("Enable when specs 005 (rooms) + 006 (realtime) land")
class RoomLifecycleIntegrationTest extends AbstractIntegrationTest {

	@SuppressWarnings("unused")
	@Autowired
	private TestRestTemplate restTemplate;

	@SuppressWarnings("unused")
	@LocalServerPort
	private int port;

	@Test
	@DisplayName("POST /api/rooms returns 201 and persists a row")
	void createRoomReturns201() {
		// TODO(spec 005):
		//   HttpHeaders h = new HttpHeaders();
		//   h.setBearerAuth("test:host-uid");
		//   ResponseEntity<RoomDto> resp = restTemplate.exchange(
		//       "/api/rooms",
		//       HttpMethod.POST,
		//       new HttpEntity<>(new CreateRoomRequest("my-room"), h),
		//       RoomDto.class);
		//   assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		//   assertThat(resp.getBody().id()).isNotNull();
		//   // Then assert against the JdbcTemplate that the row exists.
	}

	@Test
	@DisplayName("POST /api/rooms/{id}/join returns 200 and adds a membership row")
	void joinRoomAddsMembership() {
		// TODO(spec 005): see createRoomReturns201 above. Use a second
		// deterministic UID ("test:guest-uid") for the join.
	}

	@Test
	@DisplayName("POST /api/rooms/{id}/start registers the match with the engine")
	void startMatchRegistersWithEngine() {
		// TODO(spec 005 + spec 006): after start, assert that GET
		// /api/games/{id} returns the in-progress match.
	}

	@Test
	@DisplayName("STOMP subscriber on /topic/rooms/{id} receives a join event")
	void stompSubscriberReceivesJoinEvent() {
		// TODO(spec 006): use WebSocketStompClient to connect to
		// ws://localhost:{port}/ws, subscribe to /topic/rooms/{id},
		// trigger a join from a second client, await the frame on a
		// CompletableFuture<RoomEvent> with a 5s timeout.
	}
}
