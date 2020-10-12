package io.fluxcapacitor.clientapp.web.bank;

import io.fluxcapacitor.clientapp.common.authentication.AppUser;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.clientapp.common.bank.query.GetAccount;
import io.fluxcapacitor.javaclient.tracking.handling.HandleQuery;
import io.fluxcapacitor.javaclient.tracking.handling.LocalHandler;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class BankQueryHandler {
    @HandleQuery
    @LocalHandler
    BankAccount handle(GetAccount query, AppUser user) {
        BankAccount account = BankAccount.load(query.getAccountId());
        if (account != null && !user.isAdmin() && !Objects.equals(user.getName(), account.getUserId())) {
            throw new UnauthorizedException("User is unauthorized to access account: " + user);
        }
        return account;
    }
}
