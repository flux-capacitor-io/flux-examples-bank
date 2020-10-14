package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.IllegalCommandException;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.Transaction;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Value
public class TransferMoney extends CustomerCommand implements ModifyAccount {
    String accountId;
    @NotBlank String destinationAccountId;
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
                .transaction(Transaction.create("Transfer of €" + amount.toPlainString() + " to " + destinationAccountId))
                .build();
    }

}
