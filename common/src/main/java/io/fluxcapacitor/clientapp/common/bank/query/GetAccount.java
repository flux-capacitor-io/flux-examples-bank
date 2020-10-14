package io.fluxcapacitor.clientapp.common.bank.query;

import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.javaclient.publishing.routing.RoutingKey;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@RequiresAppRole(Role.customer)
public class GetAccount {
    @RoutingKey
    @NotBlank String accountId;
}
