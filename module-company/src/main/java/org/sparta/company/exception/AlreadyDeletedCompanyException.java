package org.sparta.company.exception;

import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;

public class AlreadyDeletedCompanyException extends BusinessException {
    public AlreadyDeletedCompanyException() {
        super(CommonErrorType.CONFLICT, "Company already deleted");
    }
}
