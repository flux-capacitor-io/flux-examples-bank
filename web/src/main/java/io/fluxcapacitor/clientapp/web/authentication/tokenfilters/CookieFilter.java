package io.fluxcapacitor.clientapp.web.authentication.tokenfilters;

import io.fluxcapacitor.clientapp.common.authentication.AuthenticationToken;
import io.fluxcapacitor.clientapp.web.authentication.TokenFilter;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.NewCookie;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

@Slf4j
public class CookieFilter implements TokenFilter {
    @Override
    public Optional<AuthenticationToken> obtainToken(Map<String, List<String>> headers) {
        return getCookieValue("jwt", headers).map(this::createToken);
    }

    private AuthenticationToken createToken(String jwtToken) {
        return AuthenticationToken.builder().value(jwtToken).responseCooky(new NewCookie(
                "jwt", jwtToken, "/", null, null,
                DEFAULT_MAX_AGE, true, true)).build();
    }
}
