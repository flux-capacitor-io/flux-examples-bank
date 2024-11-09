package io.fluxcapacitor.clientapp.common.bank.query;

import io.fluxcapacitor.clientapp.common.authentication.RequiresRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleQuery;
import io.fluxcapacitor.javaclient.tracking.handling.Request;
import lombok.Value;

import java.util.List;

@Value
@RequiresRole(Role.admin)
public class FindAccounts implements Request<List<BankAccount>> {
    String term;

    @HandleQuery
    List<BankAccount> handle() {
        return FluxCapacitor.search(BankAccount.class).lookAhead(term).fetch(100);
    }
}
