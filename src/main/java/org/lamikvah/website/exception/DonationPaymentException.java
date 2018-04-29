package org.lamikvah.website.exception;

public class DonationPaymentException extends RuntimeException {

    private static final long serialVersionUID = 4396356234683412513L;

    public DonationPaymentException() {
        super();
    }

    public DonationPaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public DonationPaymentException(String message) {
        super(message);
    }

    public DonationPaymentException(Throwable cause) {
        super(cause);
    }


}
