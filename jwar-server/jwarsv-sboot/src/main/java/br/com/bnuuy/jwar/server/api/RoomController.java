package br.com.bnuuy.jwar.server.api;

import br.com.bnuuy.jwar.server.auth.CurrentUser;
import br.com.bnuuy.jwar.server.domain.RoomStatus;
import br.com.bnuuy.jwar.server.dto.CreateRoomRequest;
import br.com.bnuuy.jwar.server.dto.JoinRoomRequest;
import br.com.bnuuy.jwar.server.dto.RoomDetail;
import br.com.bnuuy.jwar.server.dto.RoomMessageDto;
import br.com.bnuuy.jwar.server.dto.RoomSummary;
import br.com.bnuuy.jwar.server.dto.SendMessageRequest;
import br.com.bnuuy.jwar.server.dto.StartMatchResponse;
import br.com.bnuuy.jwar.server.dto.UpdateMemberColorRequest;
import br.com.bnuuy.jwar.server.service.MatchService;
import br.com.bnuuy.jwar.server.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Game lobbies / matchmaking rooms")
public class RoomController {

    private final RoomService roomService;
    private final MatchService matchService;
    private final CurrentUser currentUser;

    @Operation(summary = "Create a new room")
    @PostMapping
    public ResponseEntity<RoomDetail> create(@Valid @RequestBody CreateRoomRequest request) {
        RoomDetail detail = roomService.createRoom(currentUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }

    @Operation(summary = "List rooms filtered by status (default: OPEN)")
    @GetMapping
    public List<RoomSummary> list(@RequestParam(name = "status", required = false) String status) {
        RoomStatus filter = status != null ? RoomStatus.valueOf(status.toUpperCase()) : RoomStatus.OPEN;
        return roomService.listRooms(filter);
    }

    @Operation(summary = "Get a room with members and host")
    @GetMapping("/{id}")
    public RoomDetail get(@PathVariable UUID id) {
        return roomService.getRoom(id, currentUser.userId());
    }

    @Operation(summary = "Join a room")
    @PostMapping("/{id}/join")
    public RoomDetail join(@PathVariable UUID id, @RequestBody(required = false) JoinRoomRequest request) {
        return roomService.joinRoom(id, currentUser.userId(), request);
    }

    @Operation(summary = "Leave a room")
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable UUID id) {
        roomService.leaveRoom(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Change the current user's color in the room")
    @PatchMapping("/{id}/members/me")
    public RoomDetail updateColor(@PathVariable UUID id,
                                  @Valid @RequestBody UpdateMemberColorRequest request) {
        return roomService.updateMyColor(id, currentUser.userId(), request.color());
    }

    @Operation(summary = "Post a chat message to the room")
    @PostMapping("/{id}/messages")
    public ResponseEntity<RoomMessageDto> postMessage(@PathVariable UUID id,
                                                     @Valid @RequestBody SendMessageRequest request) {
        RoomMessageDto dto = roomService.postMessage(id, currentUser.userId(), request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Host-only: start the match")
    @PostMapping("/{id}/start")
    public StartMatchResponse start(@PathVariable UUID id) {
        return new StartMatchResponse(matchService.startMatch(id, currentUser.userId()).getId());
    }

    @SuppressWarnings("unused")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaceholder(@PathVariable UUID id) {
        // Reserved for future host-initiated room close. Not implemented in v1.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
