package io.fluxcapacitor.clientapp.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.fluxcapacitor.javaclient.test.TestFixture;
import lombok.SneakyThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public static void assertJsonEquals(String expected, String actual) {
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        assertEquals(writer.writeValueAsString(objectMapper.readTree(expected)),
                writer.writeValueAsString(objectMapper.readTree(actual)));
    }

    public static TestFixture createTestFixture(Object... handlers) {
        return TestFixture.create(handlers);
    }

}
