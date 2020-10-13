package io.fluxcapacitor.clientapp.app.bank;

import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.command.AccountCommand;
import io.fluxcapacitor.clientapp.common.bank.command.DepositTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.TransferFailed;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountCommandHandler {
    @HandleCommand
    void handle(AccountCommand command) {
        FluxCapacitor.loadAggregate(command.getAccountId(), BankAccount.class).assertLegal(command).apply(command);
    }

    @HandleCommand
    void handleDeposit(DepositTransfer command) {
        try {
            handle(command);
        } catch (Exception e) {
            throw new TransferFailed("Failed to complete transfer", command);
        }
    }
}
