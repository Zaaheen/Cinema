package com.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "combos")
@Getter
@Setter
@NoArgsConstructor
public class Combo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "source_key", nullable = false, unique = true) private String sourceKey;
    @Column(nullable = false) private String name;
    private String description;
    @Column(nullable = false) private BigDecimal price;
    @Column(name = "image_url") private String imageUrl;
    @Column(name = "items_json", columnDefinition = "jsonb") private String itemsJson;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
