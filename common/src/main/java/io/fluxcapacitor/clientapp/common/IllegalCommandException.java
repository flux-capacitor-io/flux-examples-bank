package io.fluxcapacitor.clientapp.common;

import io.fluxcapacitor.javaclient.common.exception.FunctionalException;

public class IllegalCommandException extends FunctionalException {
    public IllegalCommandException(String message) {
        super(message);
    }
}
