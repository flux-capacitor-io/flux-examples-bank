package io.fluxcapacitor.clientapp.app;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPairGenerator;
import java.util.Base64;

@Slf4j
public class JwtFactory {

    @SneakyThrows
    public static void main(String args[])  {
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.genKeyPair();
        Base64.Encoder base64Encoder = Base64.getEncoder();
        var privateKeyString = new String(base64Encoder.encode(keyPair.getPrivate().getEncoded()));
        var publicKeyString = new String(base64Encoder.encode(keyPair.getPublic().getEncoded()));
        log.info("\nPrivate key:\n{}\nPublic key:\n{}", privateKeyString, publicKeyString);
    }
}
