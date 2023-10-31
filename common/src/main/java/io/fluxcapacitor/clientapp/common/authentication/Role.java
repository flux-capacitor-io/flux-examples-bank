package io.fluxcapacitor.clientapp.common.authentication;

import lombok.Getter;

@Getter
public enum Role {
    customer,
    admin(customer),
    system(admin);

    private final Role[] assumedRoles;

    Role(Role... assumedRoles) {
        this.assumedRoles = assumedRoles;
    }

    public boolean matches(String userRole) {
        Role role;
        try {
            role = Role.valueOf(userRole);
        } catch (Exception e) {
            return false;
        }
        return matches(role);
    }

    public boolean matches(Role userRole) {
        if (userRole == null) {
            return false;
        }
        if (this == userRole) {
            return true;
        }
        for (Role assumedRole : userRole.getAssumedRoles()) {
            if (matches(assumedRole)) {
                return true;
            }
        }
        return false;
    }
}
