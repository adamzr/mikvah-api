package org.lamikvah.website.exception;

public class AppointmentCreationException extends RuntimeException {

    private static final long serialVersionUID = 4396356234683412513L;

    public AppointmentCreationException() {
        super();
    }

    public AppointmentCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppointmentCreationException(String message) {
        super(message);
    }

    public AppointmentCreationException(Throwable cause) {
        super(cause);
    }


}
