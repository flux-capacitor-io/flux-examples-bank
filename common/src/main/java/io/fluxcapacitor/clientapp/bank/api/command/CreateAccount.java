package io.fluxcapacitor.clientapp.bank.api.command;

import io.fluxcapacitor.clientapp.bank.api.BankAccount;
import io.fluxcapacitor.clientapp.common.IllegalCommandException;
import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Value;

import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Value
@RequiresAppRole(Role.customer)
public class CreateAccount implements AccountCommand {
    String accountId;

    @PositiveOrZero BigDecimal maxOverdraft;

    @AssertLegal
    void assertNotExists(BankAccount account) {
        if (account != null) {
            throw new IllegalCommandException("Bank account already exists");
        }
    }

    @Apply
    BankAccount apply() {
        return BankAccount.builder().accountId(accountId).build();
    }
}
