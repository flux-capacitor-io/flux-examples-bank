package io.fluxcapacitor.clientapp.common.user;

import io.fluxcapacitor.javaclient.modeling.Id;

public final class UserId extends Id<UserProfile> {
    public UserId(String functionalId) {
        super(functionalId, UserProfile.class, false);
    }
}
