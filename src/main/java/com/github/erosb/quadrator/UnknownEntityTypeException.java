package com.github.erosb.quadrator;

public class UnknownEntityTypeException extends RuntimeException {

    public UnknownEntityTypeException(Class<?> type) {
        super("no mapping configuration provided for type " + type.getSimpleName());
    }
}
