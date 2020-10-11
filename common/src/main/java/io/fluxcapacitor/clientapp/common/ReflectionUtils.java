package io.fluxcapacitor.clientapp.common;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.fluxcapacitor.common.reflection.ReflectionUtils.getProperty;

public class ReflectionUtils {

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <T> Optional<T> getFieldValue(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            T value = (T) field.get(instance);
            return Optional.ofNullable(value);
        } catch (NoSuchFieldException e) {
            return Optional.empty();
        }
    }

    public static void setField(String path, Object target, Object value) {
        List<String> fields = Arrays.stream(path.split("\\.")).collect(Collectors.toList());
        String lastField = fields.remove(fields.size() - 1);
        for (String field : fields) {
            target = getProperty(FieldUtils.getField(target.getClass(), field, true), target);
        }
        io.fluxcapacitor.common.reflection.ReflectionUtils.setField(lastField, target, value);
    }

}
