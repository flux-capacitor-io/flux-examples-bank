package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.Transaction;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Value
@RequiresAppRole(Role.system)
public class DepositTransfer implements ModifyAccount {
    String accountId;
    @NotBlank String sourceAccountId;
    @NotNull @Positive BigDecimal amount;

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().balance(account.getBalance().add(amount))
                .transaction(Transaction.create("Transfer of €" + amount.toPlainString() + " from " + sourceAccountId))
                .build();
    }

}
