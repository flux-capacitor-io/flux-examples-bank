package io.fluxcapacitor.clientapp.adapter.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthenticatedException;
import lombok.AllArgsConstructor;

import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@AllArgsConstructor
public class CompositeJwtVerifier implements JWTVerifier {

    private final List<JWTVerifier> delegates;

    @Override
    public DecodedJWT verify(String token) throws JWTVerificationException {
        if (isNullOrEmpty(token)) {
            throw new UnauthenticatedException("Authorization header missing");
        }
        return verify(JWT.decode(token));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public DecodedJWT verify(DecodedJWT jwt) throws JWTVerificationException {
        JWTVerificationException error = null;
        for (JWTVerifier delegate : delegates) {
            try {
                return delegate.verify(jwt);
            } catch (JWTVerificationException e) {
                error = e;
            }
        }
        throw error;
    }
}
