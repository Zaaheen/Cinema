package com.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seat_prices")
@Getter
@Setter
@NoArgsConstructor
public class SeatPrice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "seat_type", nullable = false) private String seatType;
    @Column(name = "movie_version") private String movieVersion;
    @Column(name = "movie_version_key") private String movieVersionKey;
    @Column(nullable = false) private BigDecimal price;
    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Column(name = "valid_to") private LocalDate validTo;
    @Column(nullable = false) private String status = "INACTIVE";
    @Column(nullable = false) private String origin = "LEGACY_UNREVIEWED";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id") private User createdByUser;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by_user_id") private User updatedByUser;
    @Version @Column(name = "row_version", nullable = false) private Long rowVersion;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = "INACTIVE";
        if (origin == null) origin = "LEGACY_UNREVIEWED";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
