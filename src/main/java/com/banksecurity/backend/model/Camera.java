package com.banksecurity.backend.model;

import com.banksecurity.backend.model.enums.CameraStatus;
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
@Table(name = "cameras",
        indexes = {
                @Index(name = "idx_cameras_status", columnList = "status"),
                @Index(name = "idx_cameras_location", columnList = "location")
        })
@EntityListeners(AuditingEntityListener.class)
public class Camera {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "rtsp_url", nullable = false, length = 500)
    private String rtspUrl;

    @Column(length = 255)
    private String location;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 50)
    private String model;

    @Column(length = 50)
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CameraStatus status = CameraStatus.INACTIVE;

    @Column(name = "resolution", length = 20)
    private String resolution = "1920x1080";

    @Column(name = "fps")
    private Integer fps = 15;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "is_recording")
    private Boolean isRecording = false;

    @Column(name = "is_analyzing")
    private Boolean isAnalyzing = false;

    @OneToMany(mappedBy = "camera", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Zone> zones = new HashSet<>();

    @OneToMany(mappedBy = "camera", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Alert> alerts = new HashSet<>();

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
        if (status == null) status = CameraStatus.INACTIVE;
        if (resolution == null) resolution = "1920x1080";
        if (fps == null) fps = 15;
        if (isRecording == null) isRecording = false;
        if (isAnalyzing == null) isAnalyzing = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addZone(Zone zone) {
        zones.add(zone);
        zone.setCamera(this);
    }

    public void removeZone(Zone zone) {
        zones.remove(zone);
        zone.setCamera(null);
    }

    public void addAlert(Alert alert) {
        alerts.add(alert);
        alert.setCamera(this);
    }

    public void removeAlert(Alert alert) {
        alerts.remove(alert);
        alert.setCamera(null);
    }
}