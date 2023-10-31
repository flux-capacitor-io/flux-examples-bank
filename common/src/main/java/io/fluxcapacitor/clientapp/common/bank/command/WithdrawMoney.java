package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.bank.AccountId;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.Transaction;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import io.fluxcapacitor.javaclient.tracking.handling.IllegalCommandException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Value;

import java.math.BigDecimal;

@Value
public class WithdrawMoney implements ModifyAccount, CustomerCommand {
    AccountId accountId;
    @NotNull @Positive BigDecimal amount;

    @AssertLegal
    void assertSufficientBalance(BankAccount account) {
        if (account.getBalance().subtract(amount).compareTo(account.getMaxOverdraft().negate()) < 0) {
            throw new IllegalCommandException("Insufficient balance");
        }
    }

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().balance(account.getBalance().subtract(amount))
                .transaction(Transaction.create("Withdrawal of €" + amount.toPlainString()))
                .build();
    }

}
