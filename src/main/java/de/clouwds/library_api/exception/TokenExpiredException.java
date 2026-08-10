package de.clouwds.library_api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;

public class TokenExpiredException extends AuthenticationException {

    private static final Logger logger = LoggerFactory.getLogger(TokenExpiredException.class);

    public TokenExpiredException(String message) {
        super(message);
        logger.warn(message);
    }
}
