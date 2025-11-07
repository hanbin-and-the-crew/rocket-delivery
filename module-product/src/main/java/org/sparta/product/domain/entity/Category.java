package org.sparta.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;

import java.util.UUID;

/**
 * 상품 카테고리 엔티티
 * 예: 건어물, 냉동식품, 신선식품
 */
@Entity
@Getter
@Table(name = "p_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String categoryName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean isActive;

    private Category(String categoryName, String description, Boolean isActive) {
        this.categoryName = categoryName;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
    }
}