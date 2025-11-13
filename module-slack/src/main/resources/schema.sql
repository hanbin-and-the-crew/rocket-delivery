CREATE TABLE IF NOT EXISTS p_company_delivery_routes (
    id UUID PRIMARY KEY,
    delivery_id UUID NOT NULL,
    scheduled_date DATE NOT NULL,
    origin_hub_id UUID NOT NULL,
    origin_hub_name VARCHAR(200),
    origin_address VARCHAR(1000),
    destination_company_id UUID NOT NULL,
    destination_company_name VARCHAR(200) NOT NULL,
    destination_address VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    delivery_manager_id UUID,
    delivery_manager_name VARCHAR(100),
    delivery_manager_slack_id VARCHAR(200),
    delivery_order INTEGER,
    expected_distance_meters BIGINT,
    expected_duration_minutes INTEGER,
    actual_distance_meters BIGINT,
    actual_duration_minutes INTEGER,
    route_summary TEXT,
    ai_reason TEXT,
    naver_route_link VARCHAR(1000),
    dispatch_message_id UUID,
    dispatched_at TIMESTAMP,
    assigned_at TIMESTAMP,
    scheduled_dispatch_time TIME,
    failure_reason VARCHAR(1000),
    waypoints_payload TEXT,
    stops_payload TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_route_delivery UNIQUE (delivery_id)
);

CREATE INDEX IF NOT EXISTS idx_route_schedule_status
    ON p_company_delivery_routes (scheduled_date, status);

CREATE INDEX IF NOT EXISTS idx_route_manager_schedule
    ON p_company_delivery_routes (delivery_manager_id, scheduled_date);
