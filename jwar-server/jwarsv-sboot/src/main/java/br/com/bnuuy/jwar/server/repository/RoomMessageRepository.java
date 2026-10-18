package br.com.bnuuy.jwar.server.repository;

import br.com.bnuuy.jwar.server.domain.RoomMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomMessageRepository extends JpaRepository<RoomMessage, UUID> {

    List<RoomMessage> findByRoomIdOrderBySentAtAsc(UUID roomId);
}
