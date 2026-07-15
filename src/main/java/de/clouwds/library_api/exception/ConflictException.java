package de.clouwds.library_api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConflictException extends RuntimeException{

    Logger logger = LoggerFactory.getLogger(ConflictException.class);

    public ConflictException() {
        super();
    }

    public ConflictException(String message) {
        super(message);
        logger.error(message);
    }
}
