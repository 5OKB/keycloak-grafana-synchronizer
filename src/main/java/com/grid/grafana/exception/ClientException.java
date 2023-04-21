package com.grid.grafana.exception;

public class ClientException extends RuntimeException {

    public ClientException(String errorMessage) {
        super(errorMessage);
    }

    public ClientException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
