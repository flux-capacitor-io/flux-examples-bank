package io.fluxcapacitor.clientapp.bank.api.command;

import io.fluxcapacitor.clientapp.bank.api.BankAccount;
import io.fluxcapacitor.clientapp.common.IllegalCommandException;
import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Value
@RequiresAppRole(Role.customer)
public class WithdrawMoney implements ModifyAccount {
    String accountId;
    @NotNull @Positive BigDecimal amount;

    @AssertLegal
    void assertSufficientBalance(BankAccount account) {
        if (account.getBalance().subtract(amount).compareTo(account.getMaxOverdraft().negate()) < 0) {
            throw new IllegalCommandException("Insufficient balance");
        }
    }

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().balance(account.getBalance().subtract(amount)).build();
    }
}
