package de.clouwds.library_api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;

public class InvalidTokenException extends AuthenticationException {

    private static final Logger logger = LoggerFactory.getLogger(InvalidTokenException.class);

    public InvalidTokenException(String message) {
        super(message);
        logger.warn(message);
    }
}
