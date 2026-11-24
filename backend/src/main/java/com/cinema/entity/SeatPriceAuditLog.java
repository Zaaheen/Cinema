package com.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "seat_price_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class SeatPriceAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_price_id", nullable = false)
    private SeatPrice seatPrice;
    @Column(nullable = false) private long revision;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;
    @Column(nullable = false, length = 30) private String action;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", columnDefinition = "jsonb")
    private String beforeState;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", nullable = false, columnDefinition = "jsonb")
    private String afterState;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
}
