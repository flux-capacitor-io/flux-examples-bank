package io.fluxcapacitor.clientapp.common.user;

import io.fluxcapacitor.clientapp.common.authentication.Role;
import io.fluxcapacitor.javaclient.modeling.Aggregate;
import io.fluxcapacitor.javaclient.modeling.EntityId;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.SortedSet;

import static io.fluxcapacitor.javaclient.modeling.EventPublication.IF_MODIFIED;

@Value
@Aggregate(searchable = true, collection = "users", eventPublication = IF_MODIFIED)
@Builder(toBuilder = true)
public class UserProfile {
    @EntityId
    UserId userId;
    UserInfo info;
    Role userRole;
    @Singular
    SortedSet<String> acceptedUserAgreements;
}
