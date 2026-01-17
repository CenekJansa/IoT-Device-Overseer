package cz.muni.fi.pv217.devicemanagementservice.domain;

// src/main/java/com/quarkiot/device/domain/Rule.java

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rules")
public class Rule extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "rule_name" ,nullable = false)
    public String ruleName;

    @Column(name = "from_value", nullable = false)
    public Integer fromValue; // Using 'fromValue' to avoid SQL keyword 'from'

    @Column(name = "to_value", nullable = false)
    public Integer toValue;   // Using 'toValue' to avoid potential conflicts

    @Column(name = "description")
    public String description; // Optional field

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    // --- Relationship ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    public Device device;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}