package com.banksecurity.backend.model;

import com.banksecurity.backend.model.enums.ZoneType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zones",
        indexes = {
                @Index(name = "idx_zones_type", columnList = "type"),
                @Index(name = "idx_zones_camera", columnList = "camera_id")
        })
@EntityListeners(AuditingEntityListener.class)
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_id", nullable = false)
    private Camera camera;

    @Column(columnDefinition = "TEXT")
    private String points; // JSON string of polygon points

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ZoneType type;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sensitivity")
    private Integer sensitivity = 50; // 0-100

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL)
    private Set<Rule> rules = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (sensitivity == null) sensitivity = 50;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addRule(Rule rule) {
        rules.add(rule);
        rule.setZone(this);
    }

    public void removeRule(Rule rule) {
        rules.remove(rule);
        rule.setZone(null);
    }
}