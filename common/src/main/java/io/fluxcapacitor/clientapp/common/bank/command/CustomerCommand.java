package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.authentication.AppUser;
import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthorizedException;

import java.util.Objects;

@RequiresAppRole(Role.customer)
public abstract class CustomerCommand {

    @AssertLegal
    public void assertUserIsAccountHolder(BankAccount account, AppUser user) {
        if (account != null && !user.isAdmin() && !Objects.equals(user.getName(), account.getUserId())) {
            throw new UnauthorizedException("User is unauthorized to access account: " + user);
        }
    }

}
