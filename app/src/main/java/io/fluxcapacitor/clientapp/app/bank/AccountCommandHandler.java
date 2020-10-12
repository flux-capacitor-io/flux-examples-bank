package io.fluxcapacitor.clientapp.app.bank;

import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.command.AccountCommand;
import io.fluxcapacitor.clientapp.common.bank.command.DepositTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.RevertTransfer;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleCommand;
import org.springframework.stereotype.Component;

@Component
public class AccountCommandHandler {

    @HandleCommand
    void handle(AccountCommand command) {
        FluxCapacitor.loadAggregate(command.getAccountId(), BankAccount.class)
                .assertLegal(command).apply(command);
    }

    @HandleCommand
    void handle(DepositTransfer command) {
        try {
            handle((AccountCommand) command);
        } catch (Exception e) {
            FluxCapacitor.sendAndForgetCommand(new RevertTransfer(command.getAccountId(), command.getAmount()));
            throw e;
        }
    }
}
