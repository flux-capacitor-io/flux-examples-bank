package com.example.flux.bank.core;

import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.command.AccountCommand;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleCommand;
import org.springframework.stereotype.Component;

@Component
public class AccountCommandHandler {
    @HandleCommand
    void handle(AccountCommand command) {
        FluxCapacitor.loadAggregate(command.getAccountId(), BankAccount.class).assertAndApply(command);
    }
}
