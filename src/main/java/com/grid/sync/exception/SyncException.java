package com.grid.sync.exception;

public class SyncException extends RuntimeException {
    public SyncException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
