package com.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_seat_id", nullable = false, unique = true) private BookingSeat bookingSeat;
    @Column(name = "ticket_code", nullable = false, unique = true, length = 30) private String ticketCode;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "checked_in_at") private Instant checkedInAt;

    @PrePersist void onCreate() { if (issuedAt == null) issuedAt = Instant.now(); }
}
