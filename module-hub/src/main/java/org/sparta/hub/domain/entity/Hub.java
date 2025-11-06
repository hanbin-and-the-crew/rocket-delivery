package org.sparta.hub.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.hub.domain.model.HubStatus;
//import org.sparta.common.entity.BaseEntity;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hub  {
//extends BaseEntity
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID hubId;

    private String name;
    private String address;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private HubStatus status = HubStatus.ACTIVE;

    private Hub(String name, String address, Double latitude, Double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Hub create(String name, String address, Double latitude, Double longitude) {
        return new Hub(name, address, latitude, longitude);
    }
}
