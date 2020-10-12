package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Value;

@Value
public class CloseAccount extends CustomerCommand implements ModifyAccount {
    String accountId;

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().closed(true).build();
    }
}
