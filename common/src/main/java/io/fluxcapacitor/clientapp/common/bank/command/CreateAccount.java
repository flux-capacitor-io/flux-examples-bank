package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.IllegalCommandException;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Value
@Builder
public class CreateAccount extends CustomerCommand implements AccountCommand {
    String accountId;
    @NotBlank String userId;

    @PositiveOrZero BigDecimal maxOverdraft;

    @AssertLegal
    void assertNotExists(BankAccount account) {
        if (account != null) {
            throw new IllegalCommandException("Bank account already exists");
        }
    }

    @Apply
    BankAccount apply() {
        return BankAccount.builder().accountId(accountId).maxOverdraft(
                maxOverdraft == null ? BigDecimal.ZERO : maxOverdraft).userId(userId).build();
    }
}
