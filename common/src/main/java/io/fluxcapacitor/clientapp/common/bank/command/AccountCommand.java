package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.bank.AccountId;
import io.fluxcapacitor.javaclient.publishing.routing.RoutingKey;
import jakarta.validation.constraints.NotNull;

public interface AccountCommand {
    @RoutingKey
    @NotNull
    AccountId getAccountId();
}
