package io.fluxcapacitor.clientapp.common;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;

@Slf4j
public class FileUtils {

    public static String loadFile(String fileName) {
        return loadFile(getCallerClass(), fileName, UTF_8);
    }

    public static String loadFile(Class<?> referencePoint, String fileName) {
        return loadFile(referencePoint, fileName, UTF_8);
    }

    @SneakyThrows
    public static void writeFile(String fileName, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
        }
    }

    public static String loadFile(String fileName, Charset charset) {
        return loadFile(getCallerClass(), fileName, charset);
    }

    @SneakyThrows
    public static String loadFile(Class<?> referencePoint, String fileName, Charset charset) {
        try (InputStream inputStream = referencePoint.getResourceAsStream(fileName)) {
            return new Scanner(inputStream, charset.name()).useDelimiter("\\A").next();
        } catch (NullPointerException e) {
            log.error("File not found {}", fileName);
            throw e;
        }
    }

    @SneakyThrows
    public static String loadFile(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        } catch (Exception e) {
            log.error("File not found {}", file, e);
            throw e;
        }
    }

    private static Class<?> getCallerClass() {
        return stream(currentThread().getStackTrace())
                .map(StackTraceElement::getClassName)
                .filter(c -> c.startsWith("io.fluxcapacitor.clientapp")
                        && !c.startsWith("io.fluxcapacitor.clientapp.common"))
                .findFirst()
                .map(io.fluxcapacitor.clientapp.common.FileUtils::forName)
                .orElseThrow(() -> new IllegalStateException("Could not find caller class"));
    }

    @SneakyThrows
    private static Class<?> forName(String name) {
        return Class.forName(name);
    }
}
