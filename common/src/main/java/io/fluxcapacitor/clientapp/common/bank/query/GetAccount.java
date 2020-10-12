package io.fluxcapacitor.clientapp.common.bank.query;

import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;
import lombok.Value;

@Value
@RequiresAppRole(Role.customer)
public class GetAccount {
    String accountId;
}
