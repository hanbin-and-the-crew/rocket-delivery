package org.sparta.company.presentation;

import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.company.application.CompanyService;
import org.sparta.company.presentation.dto.response.CompanyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> list() {
        var result = companyService.getAllActive().stream()
                .map(CompanyResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> get(@PathVariable UUID id) {
        var found = companyService.getCompany(id);
        return ResponseEntity.ok(ApiResponse.success(CompanyResponse.from(found)));
    }
}
