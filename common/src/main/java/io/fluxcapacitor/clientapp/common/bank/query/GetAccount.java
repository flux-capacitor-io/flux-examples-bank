package io.fluxcapacitor.clientapp.common.bank.query;

import io.fluxcapacitor.clientapp.common.authentication.RequiresRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.clientapp.common.authentication.Sender;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.publishing.routing.RoutingKey;
import io.fluxcapacitor.javaclient.tracking.handling.HandleSelf;
import io.fluxcapacitor.javaclient.tracking.handling.Request;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthorizedException;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

import java.util.Objects;

@Value
@RequiresRole(Role.customer)
public class GetAccount implements Request<BankAccount> {
    @RoutingKey
    @NotBlank String accountId;

    @HandleSelf
    BankAccount handle(Sender user) {
        BankAccount account = FluxCapacitor.loadAggregate(accountId, BankAccount.class).get();
        if (account != null && !user.isAdmin() && !Objects.equals(user.getName(), account.getUserId())) {
            throw new UnauthorizedException("User is unauthorized to access account: " + user);
        }
        return account;
    }
}
