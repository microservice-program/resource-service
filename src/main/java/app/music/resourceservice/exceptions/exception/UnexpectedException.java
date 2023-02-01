package app.music.resourceservice.exceptions.exception;

import org.springframework.http.HttpStatus;

public class UnexpectedException extends CommonException {
    public UnexpectedException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
