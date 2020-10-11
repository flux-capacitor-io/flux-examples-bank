package io.fluxcapacitor.clientapp.bank.api.command;

import io.fluxcapacitor.javaclient.common.exception.FunctionalException;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Value
public class TransferFailedException extends FunctionalException {
    @NotBlank String sourceAccountId, destinationAccountId;
    @NotNull @Positive BigDecimal amount;
}
