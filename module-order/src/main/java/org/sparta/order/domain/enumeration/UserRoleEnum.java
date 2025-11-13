package org.sparta.order.domain.enumeration;

public enum UserRoleEnum {
    MASTER(Authority.MASTER),                       // 마스터 관리자 권한
    HUB_MANAGER(Authority.HUB_MANAGER),             // 허브 관리자 권한
    DELIVERY_MANAGER(Authority.DELIVERY_MANAGER),   // 배송 담당자 권한
    COMPANY_MANAGER(Authority.COMPANY_MANAGER);     // 업체 담당자 권한

    private final String authority;

    UserRoleEnum(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return this.authority;
    }

    public static class Authority {
        public static final String MASTER = "ROLE_MASTER";
        public static final String HUB_MANAGER = "ROLE_HUB_MANAGER";
        public static final String DELIVERY_MANAGER = "ROLE_DELIVERY_MANAGER";
        public static final String COMPANY_MANAGER = "ROLE_COMPANY_MANAGER";
    }
}
