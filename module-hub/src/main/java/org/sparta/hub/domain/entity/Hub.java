package org.sparta.hub.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.hub.domain.model.HubStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 허브 엔티티
 * 고유 식별자는 UUID 기반으로 생성
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hub {
//extends BaseEntity
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID hubId;
    
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;

    private LocalDateTime deletedAt;
    private String deletedBy;

    @Enumerated(EnumType.STRING)
    private HubStatus status = HubStatus.ACTIVE;

    @Version
    private Long version;

    private Hub(String name, String address, Double latitude, Double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    //허브 생성
    public static Hub create(String name, String address, Double latitude, Double longitude) {
        return new Hub(name, address, latitude, longitude);
    }

    //허브 수정
    public void update(String address, Double latitude, Double longitude, HubStatus status) {
        if(address != null) this.address = address;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (status != null) this.status = status;
    }

    //허브 삭제
    public void markDeleted(String deletedBy) {
        this.status = HubStatus.INACTIVE;
        this.deletedBy = deletedBy;
        this.deletedAt = LocalDateTime.now();
    }

    //허브 삭제 여부
    public boolean isDeleted() {
        return this.status == HubStatus.INACTIVE;
    }

}
