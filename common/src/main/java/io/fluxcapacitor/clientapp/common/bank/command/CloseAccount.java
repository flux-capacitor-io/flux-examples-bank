package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.bank.AccountId;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class CloseAccount implements ModifyAccount, CustomerCommand {
    AccountId accountId;
    @NotBlank String reason;

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().closed(true).build();
    }
}
