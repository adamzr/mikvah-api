package org.lamikvah.website.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AlreadyMemberException extends RuntimeException {

    private static final long serialVersionUID = 4708940871725242411L;

    public AlreadyMemberException() {
        super();
    }

    public AlreadyMemberException(String message) {
        super(message);
    }

    public AlreadyMemberException(Throwable cause) {
        super(cause);
    }

    public AlreadyMemberException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyMemberException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
