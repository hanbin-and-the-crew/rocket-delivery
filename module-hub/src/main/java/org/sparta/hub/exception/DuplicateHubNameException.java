package org.sparta.hub.exception;

/**
 * 허브 이름이 중복될 때 발생하는 비즈니스 예외
 * 도메인 및 애플리케이션 계층에서 발생 가능
 */
public class DuplicateHubNameException extends RuntimeException {

    public DuplicateHubNameException(String name) {
        super("이미 존재하는 허브명입니다: " + name);
    }
}
