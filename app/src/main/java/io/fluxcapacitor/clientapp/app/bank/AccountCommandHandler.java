package io.fluxcapacitor.clientapp.app.bank;

import io.fluxcapacitor.clientapp.bank.api.BankAccount;
import io.fluxcapacitor.clientapp.bank.api.command.AccountCommand;
import io.fluxcapacitor.common.api.Metadata;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleCommand;
import org.springframework.stereotype.Component;

@Component
public class AccountCommandHandler {
    @HandleCommand
    void handle(AccountCommand command, Metadata metadata) {
        FluxCapacitor.loadAggregate(command.getAccountId(), BankAccount.class)
                .assertLegal(command).apply(command, metadata);
    }
}
