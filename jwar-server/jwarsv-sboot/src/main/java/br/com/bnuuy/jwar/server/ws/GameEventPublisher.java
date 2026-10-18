package br.com.bnuuy.jwar.server.ws;

import br.com.bnuuy.jwar.server.dto.GameEvent;
import br.com.bnuuy.jwar.server.dto.GameEventType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Convenience facade for emitting STOMP events. Engine code never touches this —
 * it is invoked from the controller/service layer after a successful core mutation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMatchEvent(UUID matchId, GameEventType type, Object payload) {
        GameEvent event = GameEvent.of(type, matchId, payload);
        String destination = "/topic/matches/" + matchId;
        try {
            messagingTemplate.convertAndSend(destination, event);
        } catch (Exception ex) {
            log.warn("Failed to publish match event {} on {}: {}", type, destination, ex.getMessage());
        }
    }

    public void publishRoomEvent(UUID roomId, GameEventType type, Object payload) {
        GameEvent event = GameEvent.of(type, roomId, payload);
        String destination = "/topic/rooms/" + roomId;
        try {
            messagingTemplate.convertAndSend(destination, event);
        } catch (Exception ex) {
            log.warn("Failed to publish room event {} on {}: {}", type, destination, ex.getMessage());
        }
    }

    public void publishPrivateCardsEvent(UUID userId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/cards", payload);
        } catch (Exception ex) {
            log.warn("Failed to publish private card event for user {}: {}", userId, ex.getMessage());
        }
    }
}
