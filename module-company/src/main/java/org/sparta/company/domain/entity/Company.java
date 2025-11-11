package org.sparta.company.domain.entity;


import jakarta.persistence.*;
import lombok.*;
import org.sparta.company.domain.model.CompanyType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID companyId;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType type; // SUPPLIER or RECEIVER

    @Column(nullable = false)
    private UUID hubId; // 소속 허브

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String name, CompanyType type, String address) {
        if (name != null && !name.isBlank()) this.name = name;
        if (type != null) this.type = type;
        if (address != null && !address.isBlank()) this.address = address;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsDeleted() {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.active = true;
        this.deletedAt = null;
    }

    public boolean isActive() {
        return this.active;
    }
}
