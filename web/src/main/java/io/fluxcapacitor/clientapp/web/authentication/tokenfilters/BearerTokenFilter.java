package io.fluxcapacitor.clientapp.web.authentication.tokenfilters;

import io.fluxcapacitor.clientapp.common.authentication.AuthenticationToken;
import io.fluxcapacitor.clientapp.web.authentication.TokenFilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

public class BearerTokenFilter implements TokenFilter {
    @Override
    public Optional<AuthenticationToken> obtainToken(Map<String, List<String>> headers) {
        List<String> result = headers.get(AUTHORIZATION);
        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0).replaceFirst("Bearer ", ""))
                .map(v -> AuthenticationToken.builder().value(v).build());
    }
}
