package org.sparta.hub.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.hub.domain.repository.HubRepository;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HubRepository hubRepository;

    @Test
    @DisplayName("허브 생성 요청이 성공하면 201 상태코드와 응답 데이터를 반환한다")
    void createHub_success() throws Exception {
        HubCreateRequest request = new HubCreateRequest(
                "테스트 허브",
                "서울시 강남구 테스트로 1",
                37.55,
                127.01
        );

        mockMvc.perform(post("/api/hubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("테스트 허브"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("허브 이름이 중복될 경우 409 상태코드와 오류 메시지를 반환한다")
    void createHub_duplicate_fail() throws Exception {
        hubRepository.save(org.sparta.hub.domain.entity.Hub.create(
                "중복 허브", "서울시", 37.55, 127.01
        ));

        HubCreateRequest request = new HubCreateRequest(
                "중복 허브", "서울시", 37.55, 127.01
        );

        mockMvc.perform(post("/api/hubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate hub name"));
    }

    @Test
    @DisplayName("허브 이름이 비어있으면 400 상태코드를 반환한다")
    void createHub_validation_fail() throws Exception {
        HubCreateRequest request = new HubCreateRequest(
                "", // invalid name
                "서울시 강남구",
                37.55,
                127.01
        );

        mockMvc.perform(post("/api/hubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }
}
