package io.fluxcapacitor.clientapp.web.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import io.fluxcapacitor.clientapp.common.authentication.AppUser;
import io.fluxcapacitor.clientapp.common.authentication.AuthenticationToken;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthenticatedException;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Suppliers.memoize;
import static io.fluxcapacitor.clientapp.common.ApplicationUtils.isDevMode;
import static java.lang.System.getProperty;

@Slf4j
public class AuthenticationUtils {
    private static final Supplier<Optional<Algorithm>> jwtAlgorithm = memoize(AuthenticationUtils::createJwtAlgorithm);
    private static final Function<AuthenticationToken, User> tokenVerifier = createTokenVerifier();

    public static User createUser(AuthenticationToken jwtToken) {
        return tokenVerifier.apply(jwtToken);
    }

    @SneakyThrows
    private static Function<AuthenticationToken, User> createTokenVerifier() {
        if (isDevMode()) {
            return token -> createUser(decode(token), token);
        }
        JWTVerifier jwtVerifier = getJwtVerifier();
        return token -> createUser(jwtVerifier.verify(decode(token)), token);
    }

    private static DecodedJWT decode(AuthenticationToken token) {
        if (token == null || token.getValue() == null) {
            throw new UnauthenticatedException("Authorization header missing");
        }
        return JWT.decode(token.getValue());
    }

    private static User createUser(DecodedJWT decodedJWT, AuthenticationToken token) {
        List<String> roles = decodedJWT.getClaims().get("roles").asList(String.class);
        return AppUser.builder().name(decodedJWT.getSubject()).roles(roles).authenticationToken(token).build();
    }

    @SneakyThrows
    private static JWTVerifier getJwtVerifier() {
        List<JWTVerifier> verifiers = new ArrayList<>();
        jwtAlgorithm.get().ifPresentOrElse(algorithm -> verifiers.add(JWT.require(algorithm).build()), () -> {
            throw new IllegalStateException("JWT algorithm missing");
        });
        return new CompositeJwtVerifier(verifiers);
    }

    @SneakyThrows
    private static Optional<Algorithm> createJwtAlgorithm() {
        String privateKeyString = getProperty("jwt.private"), publicKeyString = getProperty("jwt.public");
        if (privateKeyString == null || publicKeyString == null) {
            return Optional.empty();
        }
        var keyFact = KeyFactory.getInstance("RSA");
        var decoder = Base64.getDecoder();
        var publicKey = keyFact.generatePublic(new X509EncodedKeySpec(decoder.decode(publicKeyString)));
        var privateKey = keyFact.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString.getBytes())));
        return Optional.of(Algorithm.RSA512((RSAPublicKey) publicKey, (RSAPrivateKey) privateKey));
    }

}
