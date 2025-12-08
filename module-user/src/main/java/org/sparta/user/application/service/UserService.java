package org.sparta.user.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.user.application.command.UserCommand;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.*;
import org.sparta.user.domain.error.UserErrorType;
import org.sparta.user.domain.repository.UserRepository;
import org.sparta.user.infrastructure.event.publisher.UserCreatedEvent;
import org.sparta.user.infrastructure.event.publisher.UserDeletedEvent;
import org.sparta.user.infrastructure.event.publisher.UserRoleChangedEvent;
import org.sparta.user.infrastructure.event.publisher.UserUpdatedEvent;
import org.sparta.user.infrastructure.security.CustomUserDetails;
import org.sparta.user.infrastructure.security.CustomUserDetailsService;
import org.sparta.user.presentation.dto.response.UserResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final EventPublisher eventPublisher; // Spring 이벤트 퍼블리셔

    /**
     * POST users/signup
     */
    @Transactional
    @CacheEvict(value = "userListCache", allEntries = true)
    public UserResponse.SignUpUser signup(UserCommand.SignUpUser request) {

        // 중복 체크: userName, email
        String userName = request.userName();
        String email = request.email();
        validateDuplicateUser(userName, email);

        // 비밀번호 인코딩
        String password = passwordEncoder.encode(request.password());

        // 사용자 생성 및 저장
        User user = User.create(
                userName,
                password,
                request.slackId(),
                request.realName(),
                request.userPhoneNumber(),
                email,
                request.role(),
                request.hubId(),
                request.deliveryRole()
        );

        user = userRepository.save(user);

        // 유저 생성 이벤트 발행
        eventPublisher.publishExternal(UserCreatedEvent.of(user));

        return UserResponse.SignUpUser.from(user);
    }

    /**
     * GET /users/me
     */
    public UserResponse.GetUser getUserInfo(CustomUserDetails userDetailsInfo) {
        User user = userRepository.findByUserId(userDetailsInfo.getId())
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND));
        return UserResponse.GetUser.from(user);
    }

    /**
     * PATCH /users/me
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#user.id"),
            @CacheEvict(value = "userListCache", allEntries = true)
    })
    public UserResponse.UpdateUser updateSelf(CustomUserDetails user, UserCommand.UpdateUser request) {

        User userInfo = userRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND, "수정할 유저 정보가 없습니다."));

        User updated = updateUserInfo(userInfo, request);

        // 업데이트된 사용자 정보를 SecurityContext에 반영
        CustomUserDetails updatedUserDetails = customUserDetailsService.loadUserByUsername(updated.getUserName());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(updateAuthentication(authentication, updatedUserDetails));
        SecurityContextHolder.setContext(context);

        // 이벤트 발행
        eventPublisher.publishExternal(UserUpdatedEvent.of(updated));

        return UserResponse.UpdateUser.from(updated);
    }

    /**
     * DELETE /User/{userId}
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#userDetails.id"),
            @CacheEvict(value = "userListCache", allEntries = true)
    })
    public void deleteSelf(CustomUserDetails userDetails) {
        User userInfo = userRepository.findByUserId(userDetails.getId())
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND));

        User deletedUser = softDeleteUser(userInfo);

        // 로그인 세션 완전 초기화
        SecurityContextHolder.clearContext();

        eventPublisher.publishExternal(UserDeletedEvent.of(deletedUser));
    }

    /**
     * POST /User/id-find
     */
    @Transactional
    public UserResponse.FindUserId findUserId(UserCommand.FindUserId request) {
        String email = request.email();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException(UserErrorType.USER_NOT_FOUND, "해당 이메일로 가입된 사용자가 없습니다.")
                );

        return UserResponse.FindUserId.from(user);
    }

    /**
     * GET /users/bos/{userId}
     */
    @Cacheable(value = "userCache", key = "#userId")
    public UserResponse.GetUser getSpecificUserInfo(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND));
        return UserResponse.GetUser.from(user);
    }

    /**
     * PATCH users/bos/{userId}
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#userId"),
            @CacheEvict(value = "userListCache", allEntries = true)
    })
    public UserResponse.UpdateUser updateUser(UUID userId, UserCommand.UpdateUser request) {

        User userInfo = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND, "수정할 유저 정보가 없습니다."));

        User updated = updateUserInfo(userInfo, request);

        eventPublisher.publishExternal(UserUpdatedEvent.of(updated));

        return UserResponse.UpdateUser.from(updated);
    }

    /**
     * DELETE /users/bos/{userId}
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#userId"),
            @CacheEvict(value = "userListCache", allEntries = true)
    })
    public void deleteUser(UUID userId) {
        User userInfo = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND));

        User deletedUser = softDeleteUser(userInfo);

        eventPublisher.publishExternal(UserDeletedEvent.of(deletedUser));
    }

    /**
     * GET /users/bos
     */
    @Cacheable(value = "userListCache", key = "'allUsers'")
    public List<UserResponse.GetUser> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserResponse.GetUser::from)
                .toList();
    }

    /**
     * POST /users/bos/{userId}/APPROVE
     * POST /users/bos/{userId}/REJECTED
     */
    @Transactional
    @CacheEvict(value = "userListCache", allEntries = true)
    public void updateUserStatus(UUID userId, UserStatusEnum newStatus) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND));

        if (user.getStatus() != UserStatusEnum.PENDING) {
            throw new BusinessException(UserErrorType.INVALID_STATUS_CHANGE, "대기중인 회원만 상태를 변경할 수 있습니다.");
        }

        user.updateStatus(newStatus);

        // 유저 권한 변경 이벤트 발행
        eventPublisher.publishExternal(UserRoleChangedEvent.of(user));
    }

    /**
     * 공통 메서드
     */
    private void validateDuplicateUser(String userName, String email) {
        userRepository.findByUserName(userName).ifPresent(u -> {
            throw new BusinessException(UserErrorType.VALIDATION_FAILED, "중복된 사용자 ID가 존재합니다.");
        });

        userRepository.findByEmail(email).ifPresent(u -> {
            throw new BusinessException(UserErrorType.VALIDATION_FAILED, "중복된 Email 입니다.");
        });
    }

    private User updateUserInfo(User userInfo, UserCommand.UpdateUser request) {

        if (userInfo.getDeletedAt() != null) {
            throw new BusinessException(UserErrorType.USER_ALREADY_WITHDRAW);
        }

        String newPassword = request.newPassword() != null && !request.newPassword().isBlank()
                ? passwordEncoder.encode(request.newPassword().trim())
                : null;

        // 이름
        if (request.userName() != null && !request.userName().isBlank()) {
            userInfo.updateUserName(request.userName().trim());
        }

        // 비밀번호
        if (request.oldPassword() != null && newPassword != null) {
            if (!passwordEncoder.matches(request.oldPassword(), userInfo.getPassword())) {
                throw new BusinessException(UserErrorType.VALIDATION_FAILED, "기존 비밀번호가 일치하지 않습니다.");
            }
            userInfo.updatePassword(newPassword);
        }

        // 이메일
        if (request.email() != null && !request.email().isBlank()) {
            userInfo.updateEmail(request.email().trim());
        }

        // 전화번호
        if (request.userPhoneNumber() != null && !request.userPhoneNumber().isBlank()) {
            userInfo.updatePhoneNumber(request.userPhoneNumber().trim());
        }

        return userRepository.save(userInfo);
    }

    private User softDeleteUser(User user) {
        if (user.getDeletedAt() != null) {
            throw new BusinessException(UserErrorType.USER_ALREADY_WITHDRAW);
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = userRepository.softDeleteByUserId(user.getUserId(), now);

        if (updated == 0) {
            throw new BusinessException(UserErrorType.USER_NOT_FOUND);
        }

        return userRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND));
    }

    // 업데이트 인증처리에 대한 추가적인 메서드를 작성하여 기존 인증값과 현재 업데이트 된 값을 가져와서 업데이트 처리
    private Authentication updateAuthentication(Authentication authentication, CustomUserDetails userDetails) {
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                userDetails, authentication.getCredentials(), userDetails.getAuthorities());
        newAuth.setDetails(authentication.getDetails());

        return newAuth;
    }
}
