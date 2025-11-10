package org.sparta.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorType {

    HttpStatus getStatus();

    String getCode();

    String getMessage();

}
