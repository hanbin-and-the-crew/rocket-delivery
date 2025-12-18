package org.sparta.product.domain.util;

import java.util.UUID;

/**
 * reservationKey 파생 규칙
 *
 * 외부 계약 키(externalReservationKey)는 변경하지 않는다.
 * Product 내부 멱등/충돌 방지용 키(internalReservationKey)를 파생해 사용한다.
 */
public final class ReservationKeyUtil {

    private static final String SEP = ":";

    private ReservationKeyUtil() {
    }

    public static String internalKey(String externalReservationKey, UUID productId) {
        if (externalReservationKey == null || externalReservationKey.isBlank()) {
            throw new IllegalArgumentException("externalReservationKey must not be blank");
        }
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        return externalReservationKey + SEP + productId;
    }

    public static boolean looksLikeInternalKey(String key) {
        return key != null && key.contains(SEP);
    }
}
