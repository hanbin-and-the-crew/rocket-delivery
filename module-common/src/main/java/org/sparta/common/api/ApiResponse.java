package org.sparta.common.api;

/**
 *REST API의 모든 응답을 일관된 구조로 감싸주는 표준 응답 포맷
 */
public record ApiResponse<T>(Metadata meta, T data) {

    public static ApiResponse<Object> success() {
        return new ApiResponse<>(Metadata.success(), null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Metadata.success(), data);
    }

    public static ApiResponse<Object> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(
                Metadata.fail(errorCode, errorMessage),
                null
        );
    }

    public record Metadata(Result result, String errorCode, String message) {
        public enum Result {
            SUCCESS, FAIL
        }

        public static Metadata success() {
            return new Metadata(Result.SUCCESS, null, null);
        }

        public static Metadata fail(String errorCode, String errorMessage) {
            return new Metadata(Result.FAIL, errorCode, errorMessage);
        }
    }

}
