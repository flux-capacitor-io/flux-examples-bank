package io.fluxcapacitor.clientapp.bank.api.command;

import io.fluxcapacitor.clientapp.bank.api.BankAccount;
import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.Apply;
import lombok.Value;

@Value
@RequiresAppRole(Role.admin)
public class CloseAccount implements ModifyAccount {
    String accountId;

    @Apply
    BankAccount apply(BankAccount account) {
        return account.toBuilder().closed(true).build();
    }
}
