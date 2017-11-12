package org.lamikvah.website.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ServerErrorException extends RuntimeException{

    private static final long serialVersionUID = -7799725423703323917L;

    public ServerErrorException() {
        super();
    }

    public ServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerErrorException(String message) {
        super(message);
    }

    public ServerErrorException(Throwable cause) {
        super(cause);
    }

}
