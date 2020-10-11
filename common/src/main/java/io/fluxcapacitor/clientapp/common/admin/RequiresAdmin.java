package io.fluxcapacitor.clientapp.common.admin;

import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import io.fluxcapacitor.clientapp.common.authentication.Role;

@RequiresAppRole(Role.admin)
public abstract class RequiresAdmin {
}
