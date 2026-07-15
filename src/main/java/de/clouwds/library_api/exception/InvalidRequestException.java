package de.clouwds.library_api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidRequestException extends RuntimeException {

    Logger logger = LoggerFactory.getLogger(InvalidRequestException.class);

    public InvalidRequestException() {
        super();
    }

    public InvalidRequestException(String message) {
        super(message);
        logger.error(message);
    }
}
