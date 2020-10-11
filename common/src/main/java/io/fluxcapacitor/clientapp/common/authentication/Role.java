package io.fluxcapacitor.clientapp.common.authentication;

import lombok.Getter;

import java.util.EnumSet;

@Getter
public enum Role {
    customer,
    system,
    admin {
        @Override
        public Role[] getAssumedRoles() {
            return EnumSet.complementOf(EnumSet.of(admin)).toArray(new Role[0]);
        }
    };

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

    private boolean matches(Role role) {
        if (this == role) {
            return true;
        }
        for (Role assumedRole : role.getAssumedRoles()) {
            if (assumedRole.matches(this)) {
                return true;
            }
        }
        return false;
    }
}
