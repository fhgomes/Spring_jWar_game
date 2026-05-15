package br.com.bnuuy.jwar.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_members")
@Getter
@Setter
@NoArgsConstructor
public class RoomMember {

    @EmbeddedId
    private RoomMemberId id;

    @MapsId("roomId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "color", nullable = false, length = 20)
    private String color;

    @Column(name = "is_host", nullable = false)
    private boolean host;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public UUID getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public UUID getRoomId() {
        return id != null ? id.getRoomId() : null;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class RoomMemberId implements Serializable {
        @Column(name = "room_id", nullable = false)
        private UUID roomId;

        @Column(name = "user_id", nullable = false)
        private UUID userId;
    }
}
