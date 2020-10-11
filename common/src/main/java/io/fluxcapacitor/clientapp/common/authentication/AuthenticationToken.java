package io.fluxcapacitor.clientapp.common.authentication;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import javax.ws.rs.core.NewCookie;
import java.util.List;

@Value
@Builder
public class AuthenticationToken {
    @NonNull String value;
    @Singular List<NewCookie> responseCookies;
}
