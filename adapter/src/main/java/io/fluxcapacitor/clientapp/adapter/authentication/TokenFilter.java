package io.fluxcapacitor.clientapp.adapter.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.fluxcapacitor.clientapp.common.authentication.AuthenticationToken;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Cookies;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.fluxcapacitor.clientapp.common.PropertyUtils.isDevMode;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.HttpHeaders.COOKIE;

@FunctionalInterface
public interface TokenFilter {
    Optional<AuthenticationToken> obtainToken(Map<String, List<String>> headers);

    default Optional<AuthenticationToken> obtainToken(HttpServletRequest request) {
        return obtainToken(Collections.list(request.getHeaderNames()).stream().collect(
                toMap(identity(), name -> Collections.list(request.getHeaders(name)))));
    }

    default Optional<String> getCookieValue(String name, Map<String, List<String>> headers) {
        List<String> cookies = headers.get(COOKIE);
        if (cookies == null) {
            return Optional.empty();
        }
        Map<String, Cookie> cookieMap = Cookies.parseRequestCookies(200, false, cookies);
        return Optional.ofNullable(cookieMap.get(name)).map(Cookie::getValue);
    }

    default boolean isValidJwtToken(String jwtToken) {
        try {
            DecodedJWT decodedJWT = JWT.decode(jwtToken);
            return isDevMode() || Optional.ofNullable(decodedJWT.getExpiresAt())
                    .map(d -> d.toInstant().isAfter(Instant.now())).orElse(false);
        } catch (Exception e) {
            return false;
        }
    }
}
