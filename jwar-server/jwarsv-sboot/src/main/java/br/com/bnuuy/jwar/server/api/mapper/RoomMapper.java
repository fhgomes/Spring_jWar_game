package br.com.bnuuy.jwar.server.api.mapper;

import br.com.bnuuy.jwar.server.domain.Room;
import br.com.bnuuy.jwar.server.domain.RoomMember;
import br.com.bnuuy.jwar.server.domain.User;
import br.com.bnuuy.jwar.server.dto.RoomDetail;
import br.com.bnuuy.jwar.server.dto.RoomMemberDto;
import br.com.bnuuy.jwar.server.dto.RoomMessageDto;
import br.com.bnuuy.jwar.server.dto.RoomSummary;
import br.com.bnuuy.jwar.server.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomMapper {

    private final UserRepository userRepository;

    public RoomSummary toSummary(Room room) {
        String hostNick = userRepository.findById(room.getHostUserId())
            .map(User::getDisplayName)
            .orElse(null);
        return new RoomSummary(
            room.getId(),
            room.getName(),
            hostNick,
            room.getMembers().size(),
            room.getMaxPlayers(),
            room.getStatus(),
            room.hasPassword(),
            room.getCreatedAt()
        );
    }

    public RoomDetail toDetail(Room room) {
        List<UUID> memberIds = room.getMembers().stream().map(RoomMember::getUserId).toList();
        Map<UUID, User> usersById = userRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        List<RoomMemberDto> members = room.getMembers().stream()
            .map(m -> new RoomMemberDto(
                m.getUserId(),
                usersById.containsKey(m.getUserId()) ? usersById.get(m.getUserId()).getDisplayName() : null,
                m.getColor(),
                m.isHost(),
                m.getJoinedAt()
            ))
            .toList();
        return new RoomDetail(
            room.getId(),
            room.getName(),
            room.getHostUserId(),
            room.getStatus(),
            room.getMaxPlayers(),
            members,
            room.hasPassword()
        );
    }

    public RoomMessageDto toMessageDto(br.com.bnuuy.jwar.server.domain.RoomMessage message, String senderName) {
        return new RoomMessageDto(
            message.getId(),
            message.getSenderUserId(),
            senderName,
            message.getText(),
            message.getSentAt()
        );
    }
}
