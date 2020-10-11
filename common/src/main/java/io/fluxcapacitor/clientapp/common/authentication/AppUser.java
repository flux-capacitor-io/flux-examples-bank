package io.fluxcapacitor.clientapp.common.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fluxcapacitor.common.api.Metadata;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@Value
@Builder
@EqualsAndHashCode(exclude = "authenticationToken")
@ToString(exclude = "authenticationToken")
public class AppUser implements User {

    public static String metadataKey = "$user";

    @NonNull String name;
    @Singular List<String> roles;

    @JsonIgnore
    AuthenticationToken authenticationToken;

    public Metadata asMetadata() {
        return addTo(Metadata.empty());
    }

    public Metadata addTo(Metadata metadata) {
        metadata.put(metadataKey, this);
        return metadata;
    }

    @JsonIgnore
    public boolean isAdmin() {
        return roles.stream().anyMatch(Role.admin::matches);
    }

    @Override
    public boolean hasRole(String role) {
        return hasRole(Role.valueOf(role));
    }

    public boolean hasRole(Role role) {
        return roles.stream().anyMatch(role::matches);
    }
}
