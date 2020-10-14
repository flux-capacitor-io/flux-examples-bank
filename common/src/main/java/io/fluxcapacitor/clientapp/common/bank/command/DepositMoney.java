package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.Transaction;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Value
public class DepositMoney extends CustomerCommand implements ModifyAccount {
    String accountId;
    @NotNull @Positive BigDecimal amount;

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().balance(account.getBalance().add(amount))
                .transaction(Transaction.create("Deposit of €" + amount.toPlainString()))
                .build();
    }

}
