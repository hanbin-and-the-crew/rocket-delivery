package org.sparta.company.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.company.application.CompanyService;
import org.sparta.company.domain.entity.Company;
import org.sparta.company.presentation.dto.request.CompanyRequest;
import org.sparta.company.presentation.dto.response.CompanyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> create(@Valid @RequestBody CompanyRequest req) {
        Company saved = companyService.createCompany(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CompanyResponse.from(saved)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> list() {
        List<CompanyResponse> result = companyService.getAllActive()
                .stream().map(CompanyResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> get(@PathVariable UUID id) {
        Company found = companyService.getCompany(id);
        return ResponseEntity.ok(ApiResponse.success(CompanyResponse.from(found)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> update(@PathVariable UUID id,
                                                               @Valid @RequestBody CompanyRequest req) {
        Company updated = companyService.updateCompany(id, req);
        return ResponseEntity.ok(ApiResponse.success(CompanyResponse.from(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        companyService.deleteCompany(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
