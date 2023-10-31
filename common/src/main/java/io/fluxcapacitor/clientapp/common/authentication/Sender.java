package io.fluxcapacitor.clientapp.common.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fluxcapacitor.clientapp.common.user.UserId;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthenticatedException;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthorizedException;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Sender implements User {

    public static Sender getCurrent() {
        return User.getCurrent();
    }

    public static Sender requireSender() {
        Sender result = User.getCurrent();
        if (result == null) {
            throw new UnauthenticatedException("Not authenticated");
        }
        return result;
    }

    @NonNull UserId userId;
    Role userRole;

    @Override
    @JsonIgnore
    public String getName() {
        return userId.getFunctionalId();
    }

    @Override
    public boolean hasRole(String role) {
        return Role.valueOf(role).matches(userRole);
    }

    public boolean isAdmin() {
        return Role.admin.matches(userRole);
    }

    public void assertAuthorisedFor(Role role) {
        if (!role.matches(userRole)) {
            throw new UnauthorizedException("Not authorised for role " + role);
        }
    }
}
