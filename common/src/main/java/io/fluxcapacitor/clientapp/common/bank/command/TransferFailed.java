package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.javaclient.common.exception.FunctionalException;
import lombok.Value;

import java.beans.ConstructorProperties;

@Value
public class TransferFailed extends FunctionalException {
    DepositTransfer failedCommand;

    @ConstructorProperties({"message", "failedCommand"})
    public TransferFailed(String message, DepositTransfer failedCommand) {
        super(message);
        this.failedCommand = failedCommand;
    }
}
