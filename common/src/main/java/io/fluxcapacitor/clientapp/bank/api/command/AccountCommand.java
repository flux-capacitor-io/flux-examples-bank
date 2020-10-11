package io.fluxcapacitor.clientapp.bank.api.command;

import io.fluxcapacitor.javaclient.publishing.routing.RoutingKey;

import javax.validation.constraints.NotBlank;

public interface AccountCommand {
    @RoutingKey
    @NotBlank
    String getAccountId();
}
