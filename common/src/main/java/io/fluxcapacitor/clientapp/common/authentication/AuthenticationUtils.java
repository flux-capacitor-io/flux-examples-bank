package io.fluxcapacitor.clientapp.common.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import io.fluxcapacitor.clientapp.common.user.UserId;
import io.fluxcapacitor.clientapp.common.user.UserProfile;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.configuration.ApplicationProperties;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthenticatedException;
import io.fluxcapacitor.javaclient.web.WebRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpCookie;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Slf4j
public class AuthenticationUtils {

    private static final Algorithm algorithm = createAlgorithm();
    private static final Function<HttpCookie, Sender> verifier = createCookieVerifier();
    private static final String cookieName = "fluxjwt";

    public static Sender getSender(WebRequest request) {
        return request.getCookie(cookieName).map(verifier).orElse(null);
    }

    public static HttpCookie loginCookie(UserProfile user) {
        Duration duration = Duration.ofHours(6);
        String jwt = createJwtToken(user, duration);
        var result = new HttpCookie(cookieName, jwt);
        result.setHttpOnly(true);
        result.setMaxAge(duration.getSeconds());
        result.setPath("/");
        return result;
    }

    public static HttpCookie logoutCookie() {
        var result = new HttpCookie(cookieName, "");
        result.setHttpOnly(true);
        result.setMaxAge(0);
        result.setPath("/");
        result.setDiscard(true);
        return result;
    }

    public static String createJwtToken(UserProfile user, Duration duration) {
        var now = FluxCapacitor.currentClock().instant();
        return JWT.create()
                .withKeyId("fluxjwt")
                .withSubject(user.getUserId().getFunctionalId())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(duration)))
                .sign(algorithm);
    }

    @SneakyThrows
    private static Function<HttpCookie, Sender> createCookieVerifier() {
        JWTVerifier v = createVerifier();
        return token -> createSender(v.verify(decode(token)));
    }

    private static Sender createSender(DecodedJWT decodedJWT) {
        UserId userId = new UserId(decodedJWT.getSubject());
        UserProfile userProfile = FluxCapacitor.loadAggregate(userId).get();
        if (userProfile == null) {
            throw new UnauthenticatedException("User does not exist");
        }
        return Sender.builder().userId(userId).userRole(userProfile.getUserRole()).build();
    }

    private static DecodedJWT decode(HttpCookie cookie) {
        if (cookie == null) {
            throw new UnauthenticatedException("Authorization header missing");
        }
        return JWT.decode(cookie.getValue());
    }

    @SneakyThrows
    private static JWTVerifier createVerifier() {
        return JWT.require(algorithm).acceptLeeway(7 * 60).build();
    }

    @SneakyThrows
    private static Algorithm createAlgorithm() {
        String privateKeyString = ApplicationProperties.getProperty("jwt.private"),
                publicKeyString = ApplicationProperties.getProperty("jwt.public");
        if (privateKeyString == null) {
            log.warn("Please provide a JWT algorithm! Continuing without algorithm for now.");
            return Algorithm.none();
        }
        var keyFact = KeyFactory.getInstance("RSA");
        var decoder = Base64.getDecoder();
        var publicKey = keyFact.generatePublic(new X509EncodedKeySpec(decoder.decode(publicKeyString)));
        var privateKey = keyFact.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString.getBytes())));
        return Algorithm.RSA512((RSAPublicKey) publicKey, (RSAPrivateKey) privateKey);
    }

}
