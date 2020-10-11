package io.fluxcapacitor.clientapp.adapter.authentication;

import io.fluxcapacitor.clientapp.adapter.authentication.tokenfilters.BearerTokenFilter;
import io.fluxcapacitor.clientapp.adapter.authentication.tokenfilters.CompositeTokenFilter;
import io.fluxcapacitor.clientapp.adapter.authentication.tokenfilters.CookieFilter;
import io.fluxcapacitor.clientapp.common.authentication.AppUser;
import io.fluxcapacitor.clientapp.common.authentication.AuthenticationToken;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthenticatedException;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Priority;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static io.fluxcapacitor.clientapp.adapter.authentication.AuthenticationUtils.createUser;

@Provider
@Priority(Priorities.AUTHENTICATION)
@AuthenticateClient
@Slf4j
public class AuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter, Filter {

    private final TokenFilter tokenFilter = new CompositeTokenFilter(new BearerTokenFilter(), new CookieFilter());

    @Override
    public void filter(ContainerRequestContext request) {
        try {
            AuthenticationToken token = tokenFilter.obtainToken(request.getHeaders()).orElse(null);
            User user = createUser(token);
            User.current.set(user);
        } catch (Exception e) {
            throw new UnauthenticatedException("Invalid authorization header");
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            Optional.<AppUser>ofNullable(User.getCurrent()).map(AppUser::getAuthenticationToken)
                    .ifPresent(token -> token.getResponseCookies()
                            .forEach(cookie -> responseContext.getHeaders().add("Set-Cookie", cookie)));
        } finally {
            User.current.remove();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            AuthenticationToken token = tokenFilter.obtainToken(httpRequest).orElse(null);
            if (token != null) {
                request = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public Map<String, String[]> getParameterMap() {
                        return Collections.singletonMap("jwt", new String[]{token.getValue()});
                    }
                };
            }
        } catch (Exception ignored) {
        }
        chain.doFilter(request, response);
    }
}
