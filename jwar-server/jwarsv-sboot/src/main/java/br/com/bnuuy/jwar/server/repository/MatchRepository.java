package br.com.bnuuy.jwar.server.repository;

import br.com.bnuuy.jwar.server.domain.Match;
import br.com.bnuuy.jwar.server.domain.MatchStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByStatusOrderByStartedAtDesc(MatchStatus status);

    @Query("SELECT m FROM Match m WHERE m.roomId IN " +
        "(SELECT rm.id.roomId FROM RoomMember rm WHERE rm.id.userId = :userId) " +
        "ORDER BY m.startedAt DESC")
    List<Match> findByParticipantUserId(@Param("userId") UUID userId);
}
