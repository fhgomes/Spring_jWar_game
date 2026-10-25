package br.com.bnuuy.jwar.server.service;

import br.com.bnuuy.jwar.core.game.map.EGameColors;
import br.com.bnuuy.jwar.server.api.mapper.RoomMapper;
import br.com.bnuuy.jwar.server.domain.Room;
import br.com.bnuuy.jwar.server.domain.RoomMember;
import br.com.bnuuy.jwar.server.domain.RoomMember.RoomMemberId;
import br.com.bnuuy.jwar.server.domain.RoomMessage;
import br.com.bnuuy.jwar.server.domain.RoomStatus;
import br.com.bnuuy.jwar.server.domain.User;
import br.com.bnuuy.jwar.server.dto.CreateRoomRequest;
import br.com.bnuuy.jwar.server.dto.GameEventType;
import br.com.bnuuy.jwar.server.dto.JoinRoomRequest;
import br.com.bnuuy.jwar.server.dto.RoomDetail;
import br.com.bnuuy.jwar.server.dto.RoomMessageDto;
import br.com.bnuuy.jwar.server.dto.RoomSummary;
import br.com.bnuuy.jwar.server.exception.BadRequestException;
import br.com.bnuuy.jwar.server.exception.ConflictException;
import br.com.bnuuy.jwar.server.exception.ForbiddenException;
import br.com.bnuuy.jwar.server.exception.NotFoundException;
import br.com.bnuuy.jwar.server.exception.UnauthorizedException;
import br.com.bnuuy.jwar.server.repository.RoomMessageRepository;
import br.com.bnuuy.jwar.server.repository.RoomRepository;
import br.com.bnuuy.jwar.server.repository.UserRepository;
import br.com.bnuuy.jwar.server.ws.GameEventPublisher;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMessageRepository roomMessageRepository;
    private final UserRepository userRepository;
    private final RoomMapper roomMapper;
    private final PasswordEncoder passwordEncoder;
    private final GameEventPublisher eventPublisher;

    @Transactional
    public RoomDetail createRoom(UUID hostUserId, CreateRoomRequest request) {
        User host = userRepository.findById(hostUserId)
            .orElseThrow(() -> new UnauthorizedException("Usuário não autenticado"));
        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setName(request.name().trim());
        room.setHostUserId(hostUserId);
        room.setStatus(RoomStatus.OPEN);
        room.setMaxPlayers(request.maxPlayers());
        if (request.password() != null && !request.password().isBlank()) {
            room.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        Instant now = Instant.now();
        room.setCreatedAt(now);
        room.setUpdatedAt(now);

        RoomMember hostMember = new RoomMember();
        hostMember.setId(new RoomMemberId(room.getId(), host.getId()));
        hostMember.setRoom(room);
        hostMember.setColor(firstAvailableColor(Set.of()));
        hostMember.setHost(true);
        hostMember.setJoinedAt(now);
        room.getMembers().add(hostMember);

        Room saved = roomRepository.save(room);
        log.info("Room created id={} host={}", saved.getId(), hostUserId);
        return roomMapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomSummary> listRooms(RoomStatus status) {
        List<Room> rooms = status == null
            ? roomRepository.findAllByOrderByCreatedAtDesc()
            : roomRepository.findByStatusOrderByCreatedAtDesc(status);
        return rooms.stream().map(roomMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public RoomDetail getRoom(UUID roomId, UUID requesterUserId) {
        Room room = loadRoom(roomId);
        ensureMember(room, requesterUserId);
        return roomMapper.toDetail(room);
    }

    @Transactional(readOnly = true)
    public RoomDetail getRoomPublic(UUID roomId) {
        Room room = loadRoom(roomId);
        return roomMapper.toDetail(room);
    }

    @Transactional
    public RoomDetail joinRoom(UUID roomId, UUID userId, JoinRoomRequest request) {
        Room room = loadRoom(roomId);
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new BadRequestException("Sala não está aberta para entrada", "ROOM_NOT_JOINABLE");
        }
        if (room.getMembers().size() >= room.getMaxPlayers()) {
            room.setStatus(RoomStatus.FULL);
            roomRepository.save(room);
            throw new BadRequestException("Não é póssível entrar, a sala está cheia", "ROOM_FULL");
        }
        boolean alreadyMember = room.getMembers().stream().anyMatch(m -> m.getUserId().equals(userId));
        if (alreadyMember) {
            throw new BadRequestException("Você já é membro desta sala", "ALREADY_MEMBER");
        }
        if (room.hasPassword()) {
            if (request == null || request.password() == null
                || !passwordEncoder.matches(request.password(), room.getPasswordHash())) {
                throw new UnauthorizedException("Senha da sala incorreta", "BAD_ROOM_PASSWORD");
            }
        }
        Set<String> takenColors = new HashSet<>();
        room.getMembers().forEach(m -> takenColors.add(m.getColor()));
        String desiredColor = request != null ? request.color() : null;
        String color;
        if (desiredColor != null && !desiredColor.isBlank()) {
            String normalized = desiredColor.trim().toUpperCase();
            if (!isValidColor(normalized)) {
                throw new BadRequestException("Cor inválida", "INVALID_COLOR");
            }
            if (takenColors.contains(normalized)) {
                throw new BadRequestException("Cor já em uso", "COLOR_TAKEN");
            }
            color = normalized;
        } else {
            color = firstAvailableColor(takenColors);
            if (color == null) {
                throw new ConflictException("Sem cores disponíveis", "NO_COLORS_AVAILABLE");
            }
        }

        RoomMember member = new RoomMember();
        member.setId(new RoomMemberId(room.getId(), userId));
        member.setRoom(room);
        member.setColor(color);
        member.setHost(false);
        member.setJoinedAt(Instant.now());
        room.getMembers().add(member);
        if (room.getMembers().size() == room.getMaxPlayers()) {
            room.setStatus(RoomStatus.FULL);
        }
        room.setUpdatedAt(Instant.now());
        Room saved = roomRepository.save(room);
        eventPublisher.publishRoomEvent(roomId, GameEventType.ROOM_MEMBER_JOINED,
            roomMapper.toDetail(saved));
        return roomMapper.toDetail(saved);
    }

    @Transactional
    public void leaveRoom(UUID roomId, UUID userId) {
        Room room = loadRoom(roomId);
        if (room.getStatus() == RoomStatus.IN_PROGRESS) {
            throw new BadRequestException("Partida em andamento — não é possível sair da sala", "MATCH_IN_PROGRESS");
        }
        RoomMember toRemove = room.getMembers().stream()
            .filter(m -> m.getUserId().equals(userId))
            .findFirst()
            .orElse(null);
        if (toRemove == null) {
            return; // idempotent
        }
        boolean wasHost = toRemove.isHost();
        room.getMembers().remove(toRemove);
        if (room.getMembers().isEmpty()) {
            room.setStatus(RoomStatus.CLOSED);
            room.setEndedAt(Instant.now());
        } else if (wasHost) {
            RoomMember newHost = room.getMembers().get(0); // OrderBy joinedAt ASC
            newHost.setHost(true);
            room.setHostUserId(newHost.getUserId());
        }
        if (room.getStatus() == RoomStatus.FULL) {
            room.setStatus(RoomStatus.OPEN);
        }
        room.setUpdatedAt(Instant.now());
        roomRepository.save(room);
        eventPublisher.publishRoomEvent(roomId, GameEventType.ROOM_MEMBER_LEFT,
            roomMapper.toDetail(room));
    }

    @Transactional
    public RoomDetail updateMyColor(UUID roomId, UUID userId, String newColor) {
        Room room = loadRoom(roomId);
        ensureMember(room, userId);
        if (room.getStatus() != RoomStatus.OPEN && room.getStatus() != RoomStatus.FULL) {
            throw new BadRequestException("Não é possível mudar de cor após o início da partida",
                "ROOM_NOT_EDITABLE");
        }
        String normalized = newColor != null ? newColor.trim().toUpperCase() : null;
        if (normalized == null || !isValidColor(normalized)) {
            throw new BadRequestException("Cor inválida", "INVALID_COLOR");
        }
        boolean taken = room.getMembers().stream()
            .anyMatch(m -> !m.getUserId().equals(userId) && m.getColor().equals(normalized));
        if (taken) {
            throw new BadRequestException("Cor já em uso", "COLOR_TAKEN");
        }
        RoomMember me = room.getMembers().stream()
            .filter(m -> m.getUserId().equals(userId))
            .findFirst()
            .orElseThrow();
        me.setColor(normalized);
        room.setUpdatedAt(Instant.now());
        Room saved = roomRepository.save(room);
        return roomMapper.toDetail(saved);
    }

    @Transactional
    public RoomMessageDto postMessage(UUID roomId, UUID userId, String text) {
        Room room = loadRoom(roomId);
        ensureMember(room, userId);
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Mensagem não pode ser vazia", "EMPTY_MESSAGE");
        }
        RoomMessage message = new RoomMessage();
        message.setId(UUID.randomUUID());
        message.setRoomId(roomId);
        message.setSenderUserId(userId);
        message.setText(trimmed);
        message.setSentAt(Instant.now());
        RoomMessage saved = roomMessageRepository.save(message);
        String senderName = userRepository.findById(userId).map(User::getDisplayName).orElse(null);
        RoomMessageDto dto = roomMapper.toMessageDto(saved, senderName);
        eventPublisher.publishRoomEvent(roomId, GameEventType.ROOM_MESSAGE, dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public Room loadRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Sala não encontrada"));
        // Force member initialisation while still in tx.
        room.getMembers().size();
        return room;
    }

    public void ensureMember(Room room, UUID userId) {
        boolean isMember = room.getMembers().stream().anyMatch(m -> m.getUserId().equals(userId));
        if (!isMember) {
            throw new ForbiddenException("Você não é membro desta sala");
        }
    }

    public void ensureHost(Room room, UUID userId) {
        if (!userId.equals(room.getHostUserId())) {
            throw new ForbiddenException("Você não é o host desta sala");
        }
    }

    public String firstAvailableColor(Set<String> taken) {
        return Arrays.stream(EGameColors.values())
            .map(Enum::name)
            .filter(name -> !taken.contains(name))
            .findFirst()
            .orElse(null);
    }

    private boolean isValidColor(String name) {
        try {
            EGameColors.valueOf(name);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
