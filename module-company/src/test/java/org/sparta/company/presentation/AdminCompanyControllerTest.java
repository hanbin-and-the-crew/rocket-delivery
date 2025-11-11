package org.sparta.company.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.company.application.CompanyService;
import org.sparta.company.domain.entity.Company;
import org.sparta.company.domain.model.CompanyType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminCompanyController.class)
@DisplayName("AdminCompanyController API 테스트")
class AdminCompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyService companyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/admin/companies - 전체 업체 목록 조회 성공")
    void listCompanies_success() throws Exception {
        // given
        List<Company> mockList = List.of(
                Company.create("스파르타상사", CompanyType.SUPPLIER, UUID.randomUUID(), "서울특별시"),
                Company.create("로지스틱스코리아", CompanyType.RECEIVER, UUID.randomUUID(), "부산광역시")
        );

        given(companyService.getAllActive()).willReturn(mockList);

        // when & then
        mockMvc.perform(get("/api/admin/companies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("스파르타상사"))
                .andExpect(jsonPath("$.data[1].name").value("로지스틱스코리아"));
    }

    @Test
    @DisplayName("GET /api/admin/companies/{id} - 단일 업체 조회 성공")
    void getCompany_success() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        Company company = Company.create("테스트상사", CompanyType.SUPPLIER, UUID.randomUUID(), "대전광역시");
        given(companyService.getCompany(eq(id))).willReturn(company);

        // when & then
        mockMvc.perform(get("/api/admin/companies/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("테스트상사"))
                .andExpect(jsonPath("$.data.type").value("SUPPLIER"))
                .andExpect(jsonPath("$.data.address").value("대전광역시"));
    }
}
