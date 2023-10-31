package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.authentication.RequiresRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.clientapp.common.bank.AccountId;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.Transaction;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Value;

import java.math.BigDecimal;

@Value
@RequiresRole(Role.system)
public class DepositTransfer implements ModifyAccount {
    AccountId accountId;
    @NotNull AccountId sourceAccountId;
    @NotNull @Positive BigDecimal amount;

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().balance(account.getBalance().add(amount))
                .transaction(Transaction.create("Transfer of €" + amount.toPlainString() + " from " + sourceAccountId))
                .build();
    }

}
