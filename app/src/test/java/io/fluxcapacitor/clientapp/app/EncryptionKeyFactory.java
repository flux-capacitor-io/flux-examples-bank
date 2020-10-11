package io.fluxcapacitor.clientapp.app;

import lombok.SneakyThrows;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

public class EncryptionKeyFactory {
    @SneakyThrows
    public static void main(String args[]) {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();
        String keyString = new String(Base64.getEncoder().encode(secretKey.getEncoded()));
        System.out.println(keyString);
    }
}
