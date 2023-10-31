package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.bank.AccountId;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import io.fluxcapacitor.javaclient.tracking.handling.IllegalCommandException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CreateAccount implements AccountCommand, CustomerCommand {
    AccountId accountId;
    @NotBlank String userId;

    @PositiveOrZero BigDecimal maxOverdraft;

    @AssertLegal
    void assertNotExists(BankAccount account) {
        throw new IllegalCommandException("Bank account already exists");
    }

    @Apply
    BankAccount apply() {
        return BankAccount.builder().accountId(accountId).maxOverdraft(
                maxOverdraft == null ? BigDecimal.ZERO : maxOverdraft).userId(userId).build();
    }
}
