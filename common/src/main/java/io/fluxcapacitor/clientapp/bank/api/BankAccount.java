package io.fluxcapacitor.clientapp.bank.api;

import io.fluxcapacitor.javaclient.persisting.eventsourcing.EventSourced;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@EventSourced
@Value
@Builder(toBuilder = true)
public class BankAccount {
    @NotBlank String accountId;

    @Builder.Default
    @NotNull @PositiveOrZero BigDecimal maxOverdraft = BigDecimal.ZERO;

    @Builder.Default
    @NotNull BigDecimal balance = BigDecimal.ZERO;

    boolean closed;
}
