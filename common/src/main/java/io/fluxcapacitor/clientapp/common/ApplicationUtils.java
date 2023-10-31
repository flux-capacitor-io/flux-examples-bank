package io.fluxcapacitor.clientapp.common;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Slf4j
public class ApplicationUtils {

    public static void startSpringApplication() {
        ClassPathScanningCandidateComponentProvider scanner
                = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));
        Class<?>[] configurations = scanner.findCandidateComponents("io.fluxcapacitor.clientapp").stream()
                .map(BeanDefinition::getBeanClassName).map(ApplicationUtils::toClass).toArray(Class<?>[]::new);

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configurations);
        context.registerShutdownHook();

        log.info("Loaded Spring configurations: {}",
                 stream(configurations).map(Class::getSimpleName).collect(toList()));
    }

    @SneakyThrows
    private static Class<?> toClass(String className) {
        return Class.forName(className);
    }
}
