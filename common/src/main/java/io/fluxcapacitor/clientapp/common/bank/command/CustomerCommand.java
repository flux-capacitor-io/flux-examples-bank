package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.authentication.RequiresRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.clientapp.common.authentication.Sender;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthorizedException;

import java.util.Objects;

@RequiresRole(Role.customer)
public interface CustomerCommand {

    @AssertLegal
    default void assertUserIsAccountHolder(BankAccount account, Sender user) {
        if (account != null && !user.isAdmin() && !Objects.equals(user.getName(), account.getUserId())) {
            throw new UnauthorizedException("User is unauthorized to access account: " + user);
        }
    }

}
