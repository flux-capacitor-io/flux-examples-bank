package io.fluxcapacitor.clientapp.app.bank;

import io.fluxcapacitor.clientapp.common.authentication.AppUser;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.query.FindAccounts;
import io.fluxcapacitor.clientapp.common.bank.query.GetAccount;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleQuery;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class BankQueryHandler {
    @HandleQuery
    BankAccount handle(GetAccount query, AppUser user) {
        BankAccount account = FluxCapacitor.loadAggregate(query.getAccountId(), BankAccount.class).get();
        if (account != null && !user.isAdmin() && !Objects.equals(user.getName(), account.getUserId())) {
            throw new UnauthorizedException("User is unauthorized to access account: " + user);
        }
        return account;
    }

    @HandleQuery
    List<BankAccount> handle(FindAccounts query) {
        return FluxCapacitor.search(BankAccount.class.getSimpleName()).lookAhead(query.getTerm()).fetch(100);
    }
}
