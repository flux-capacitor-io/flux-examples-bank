package io.fluxcapacitor.clientapp.common;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Security;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import static io.fluxcapacitor.common.ObjectUtils.memoize;
import static java.lang.System.getProperty;
import static org.apache.http.util.TextUtils.isBlank;

@Slf4j
public class PropertyUtils {
    private static final Supplier<SecretKey> encryptionKey = memoize(PropertyUtils::getEncryptionKey);

    public static void loadApplicationProperties() {
        String configDir = getProperty("config.dir");
        if (configDir != null) {
            log.info("Loading application properties from {}", configDir + "application.properties");
            loadSystemPropertiesFromFile(configDir + "application.properties");
            String environment = getProperty("environment", "test");
            if (!isBlank(environment)) {
                log.info("Loading environment properties for environment {}", environment);
                loadSystemPropertiesFromFile(configDir + "env." + environment + ".properties");
            }
            loadSecurityPropertiesFromFile(configDir + "java.security");
        } else {
            log.info("Not loading application properties. Java property `-Dconfig.dir` is not set.");
        }
        if (isDevMode()) {
            System.setProperty("spring.profiles.active", "devMode");
        }
        decryptSystemProperties();
    }

    public static String requireProperty(String name) {
        String property = getProperty(name);
        if (isBlank(property)) {
            throw new IllegalArgumentException(String.format("System property for %s is missing or empty", name));
        }
        return property;
    }

    public static boolean getBooleanProperty(String name) {
        String property = getProperty(name);
        if (isBlank(property)) {
            return false;
        }
        return property.equalsIgnoreCase("true");
    }

    /*
        Encrypt new properties
     */

    @SneakyThrows
    public static String encryptProperty(String value) {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey.get());
        return "encrypted|" + new String(Base64.getEncoder().encode(cipher.doFinal(value.getBytes())));
    }

    /*
        Environment stuff
     */

    public static boolean isDevMode() {
        return Optional.ofNullable(getProperty("devMode")).map(Boolean::valueOf).orElse(false);
    }

    public static Environment getEnvironment() {
        return Environment.valueOf(System.getProperty("environment", "test"));
    }

    /*
        Helper methods
     */

    private static SecretKey getEncryptionKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(requireProperty("encryption_key")), "AES");
    }

    @SneakyThrows
    private static void loadSystemPropertiesFromFile(String fileUrl) {
        Properties p = new Properties();
        File file = new File(fileUrl);
        if (file.exists()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                p.load(inputStream);
            }
            p.stringPropertyNames().forEach(name -> System.setProperty(name, p.getProperty(name)));
        } else {
            log.warn("Could not find file for system properties at {}", file);
        }
    }

    @SneakyThrows
    private static void loadSecurityPropertiesFromFile(String fileUrl) {
        Properties p = new Properties();
        File file = new File(fileUrl);
        if (file.exists()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                p.load(inputStream);
            }
            p.stringPropertyNames().forEach(name -> Security.setProperty(name, p.getProperty(name)));
        } else {
            log.warn("Could not find file for security properties at {}", file);
        }
    }

    @SneakyThrows
    private static void decryptSystemProperties() {
        Cipher cipher = null;
        for (Map.Entry<Object, Object> entry : new HashSet<>(System.getProperties().entrySet())) {
            if (entry.getValue() instanceof String && ((String) entry.getValue()).startsWith("encrypted|")) {
                String encryptedValue = ((String) entry.getValue()).split("encrypted\\|")[1];
                if (cipher == null) {
                    cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, encryptionKey.get());
                }
                String value = new String(cipher.doFinal(Base64.getDecoder().decode(encryptedValue)));
                System.setProperty((String) entry.getKey(), value);
            }
        }
    }
}
