package org.sparta.company.exception;

import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;

import java.util.UUID;

public class CompanyNotFoundException extends BusinessException {
    public CompanyNotFoundException(UUID id) {
        super(CommonErrorType.NOT_FOUND, "Company not found");
    }
}
