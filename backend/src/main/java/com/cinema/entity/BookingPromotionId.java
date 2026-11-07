package com.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class BookingPromotionId implements Serializable {
    @Column(name = "booking_id") private Long bookingId;
    @Column(name = "promotion_id") private Long promotionId;
}
