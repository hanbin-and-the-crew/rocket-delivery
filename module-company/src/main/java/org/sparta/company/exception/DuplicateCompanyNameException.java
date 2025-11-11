package org.sparta.company.exception;

import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;

public class DuplicateCompanyNameException extends BusinessException {
    public DuplicateCompanyNameException(String name) {
        super(CommonErrorType.CONFLICT, "\"Duplicate company name: " + name);
    }
}
