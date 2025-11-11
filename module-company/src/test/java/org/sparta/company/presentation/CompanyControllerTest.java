package org.sparta.company.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sparta.common.api.ApiResponse;
import org.sparta.company.application.CompanyService;
import org.sparta.company.domain.entity.Company;
import org.sparta.company.domain.model.CompanyType;
import org.sparta.company.presentation.dto.request.CompanyRequest;
import org.sparta.company.presentation.dto.response.CompanyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CompanyController.class)
@DisplayName("CompanyController API 테스트")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyService companyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/companies - 신규 업체 등록 성공")
    void createCompany_success() throws Exception {
        // given
        UUID hubId = UUID.randomUUID();
        CompanyRequest req = new CompanyRequest("테스트상사", CompanyType.SUPPLIER, hubId, "서울특별시");
        Company saved = Company.create(req.name(), req.type(), req.hubId(), req.address());

        given(companyService.createCompany(any())).willReturn(saved);

        // when & then
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("테스트상사"))
                .andExpect(jsonPath("$.data.type").value("SUPPLIER"));
    }

    @Test
    @DisplayName("GET /api/companies/{id} - 단일 업체 조회 성공")
    void getCompany_success() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        Company company = Company.create("스파르타로지스틱스", CompanyType.RECEIVER, UUID.randomUUID(), "부산광역시");
        given(companyService.getCompany(eq(id))).willReturn(company);

        // when & then
        mockMvc.perform(get("/api/companies/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("스파르타로지스틱스"))
                .andExpect(jsonPath("$.data.type").value("RECEIVER"));
    }

    @Test
    @DisplayName("GET /api/companies - 활성화된 업체 목록 반환")
    void getAllActive_success() throws Exception {
        // given
        List<Company> mockList = List.of(
                Company.create("A업체", CompanyType.SUPPLIER, UUID.randomUUID(), "서울"),
                Company.create("B업체", CompanyType.RECEIVER, UUID.randomUUID(), "부산")
        );
        given(companyService.getAllActive()).willReturn(mockList);

        // when & then
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("A업체"))
                .andExpect(jsonPath("$.data[1].name").value("B업체"));
    }

    @Test
    @DisplayName("PUT /api/companies/{id} - 업체 정보 수정 성공")
    void updateCompany_success() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        CompanyRequest req = new CompanyRequest("수정상사", CompanyType.RECEIVER, UUID.randomUUID(), "대전광역시");
        Company updated = Company.create(req.name(), req.type(), req.hubId(), req.address());

        given(companyService.updateCompany(eq(id), any())).willReturn(updated);

        // when & then
        mockMvc.perform(put("/api/companies/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("수정상사"))
                .andExpect(jsonPath("$.data.type").value("RECEIVER"));
    }

    @Test
    @DisplayName("DELETE /api/companies/{id} - 업체 삭제 성공")
    void deleteCompany_success() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        Mockito.doNothing().when(companyService).deleteCompany(eq(id));

        // when & then
        mockMvc.perform(delete("/api/companies/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"));
    }
}
