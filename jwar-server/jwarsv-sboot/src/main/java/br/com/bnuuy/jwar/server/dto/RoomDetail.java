package br.com.bnuuy.jwar.server.dto;

import br.com.bnuuy.jwar.server.domain.RoomStatus;
import java.util.List;
import java.util.UUID;

public record RoomDetail(
    UUID id,
    String name,
    UUID hostUserId,
    RoomStatus status,
    int maxPlayers,
    List<RoomMemberDto> members,
    boolean hasPassword
) {}
