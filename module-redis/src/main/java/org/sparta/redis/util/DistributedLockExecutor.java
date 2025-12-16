package org.sparta.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 분산 락 실행 유틸리티
 * <p>
 * ECS 다중 태스크 환경에서 동시성 제어를 위한 Redis 분산 락
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockExecutor {

    private final RedissonClient redissonClient;

    private static final long DEFAULT_WAIT_TIME = 10L;  // 락 획득 대기 시간 (초)
    private static final long DEFAULT_LEASE_TIME = 5L;  // 락 자동 해제 시간 (초)

    /**
     * 분산 락을 획득하고 작업 실행
     *
     * @param lockKey   락 키 (예: "coupon:lock:123")
     * @param action    실행할 작업
     * @param <T>       반환 타입
     * @return 작업 실행 결과
     * @throws LockAcquisitionException 락 획득 실패 시
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, action);
    }

    /**
     * 분산 락을 획득하고 작업 실행 (타임아웃 커스터마이징)
     *
     * @param lockKey   락 키
     * @param waitTime  락 획득 대기 시간
     * @param leaseTime 락 자동 해제 시간
     * @param timeUnit  시간 단위
     * @param action    실행할 작업
     * @param <T>       반환 타입
     * @return 작업 실행 결과
     * @throws LockAcquisitionException 락 획득 실패 시
     */
    public <T> T executeWithLock(
            String lockKey,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Supplier<T> action
    ) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                log.warn("분산 락 획득 실패: lockKey={}", lockKey);
                throw new LockAcquisitionException("다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.debug("분산 락 획득 성공: lockKey={}", lockKey);
            return action.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("분산 락 획득 중 인터럽트 발생: lockKey={}", lockKey, e);
            throw new LockAcquisitionException("락 획득 중 오류가 발생했습니다.", e);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("분산 락 해제: lockKey={}", lockKey);
            }
        }
    }

    /**
     * 분산 락을 획득하고 작업 실행 (반환값 없음)
     *
     * @param lockKey 락 키
     * @param action  실행할 작업
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }
}