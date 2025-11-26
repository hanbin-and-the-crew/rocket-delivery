-- Product Database 초기화 스크립트
-- Database: product_db
-- User: product_user

-- 기본 설정
SET timezone = 'Asia/Seoul';

-- 카테고리 테이블
CREATE TABLE IF NOT EXISTS p_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- 상품 테이블
CREATE TABLE IF NOT EXISTS p_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_name VARCHAR(200) NOT NULL,
    price BIGINT NOT NULL,
    category_id UUID NOT NULL,
    company_id UUID NOT NULL,
    hub_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES p_categories(id)
);

-- 재고 테이블
CREATE TABLE IF NOT EXISTS p_stocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    company_id UUID NOT NULL,
    hub_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_STOCK',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT chk_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_reserved_quantity CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_status CHECK (status IN ('IN_STOCK', 'OUT_OF_STOCK', 'RESERVED_ONLY', 'UNAVAILABLE'))
);

-- 이벤트 처리 이력 테이블 (중복 방지)
CREATE TABLE IF NOT EXISTS p_processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- 주석
COMMENT ON TABLE p_categories IS '상품 카테고리 테이블';
COMMENT ON TABLE p_products IS '상품 메타데이터 테이블';
COMMENT ON TABLE p_stocks IS '재고 관리 테이블 (낙관적 락 적용)';
COMMENT ON TABLE p_processed_events IS 'Kafka 이벤트 중복 처리 방지 테이블';

COMMENT ON COLUMN p_stocks.quantity IS '실물 총 재고량';
COMMENT ON COLUMN p_stocks.reserved_quantity IS '주문으로 예약된 재고량';
COMMENT ON COLUMN p_stocks.version IS '낙관적 락 버전';