package de.clouwds.library_api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;

public class TokenAlreadyUsedException extends AuthenticationException {

    private static final Logger logger = LoggerFactory.getLogger(TokenAlreadyUsedException.class);

    public TokenAlreadyUsedException(String message) {
        super(message);
        logger.warn(message);
    }
}
