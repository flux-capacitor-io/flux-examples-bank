package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class CloseAccount extends CustomerCommand implements ModifyAccount {
    String accountId;
    @NotBlank String reason;

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().closed(true).build();
    }
}
