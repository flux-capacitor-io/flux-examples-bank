package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.javaclient.common.exception.FunctionalException;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;

@Value
public class TransferFailed extends FunctionalException {
    @NotNull @Valid DepositTransfer failedCommand;

    @ConstructorProperties({"message", "failedCommand"})
    public TransferFailed(String message, DepositTransfer failedCommand) {
        super(message);
        this.failedCommand = failedCommand;
    }
}
