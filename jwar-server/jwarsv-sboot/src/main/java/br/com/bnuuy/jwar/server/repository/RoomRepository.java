package br.com.bnuuy.jwar.server.repository;

import br.com.bnuuy.jwar.server.domain.Room;
import br.com.bnuuy.jwar.server.domain.RoomStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByStatusOrderByCreatedAtDesc(RoomStatus status);

    List<Room> findAllByOrderByCreatedAtDesc();
}
