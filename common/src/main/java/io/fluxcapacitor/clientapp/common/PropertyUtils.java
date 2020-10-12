package io.fluxcapacitor.clientapp.common;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;

import static java.lang.System.getProperty;
import static org.apache.http.util.TextUtils.isBlank;

@Slf4j
public class PropertyUtils {

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
        } else {
            log.info("Not loading application properties. Java property `-Dconfig.dir` is not set.");
        }
        if (isDevMode()) {
            System.setProperty("spring.profiles.active", "devMode");
        }
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
}
