package org.sparta.user.domain.enums;

public enum UserRoleEnum {
    MASTER_ADMIN(Authority.MASTER_ADMIN),  // 사용자 권한
    HUB_ADMIN(Authority.HUB_ADMIN),  // 관리자 권한
    DELIVERY_PERSON(Authority.DELIVERY_PERSON),
    COMPANY_PERSON(Authority.COMPANY_PERSON);

    private final String authority;

    UserRoleEnum(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return this.authority;
    }

    public static class Authority {
        public static final String MASTER_ADMIN = "ROLE_MASTER";
        public static final String HUB_ADMIN = "ROLE_HUB_ADMIN";
        public static final String DELIVERY_PERSON = "ROLE_DELIVERY_PERSON";
        public static final String COMPANY_PERSON = "ROLE_COMPANY_PERSON";
    }
}
