package io.fluxcapacitor.clientapp.adapter.documentation;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class SwaggerReader implements OpenApiReader {

    private final Reader delegate = new Reader();

    @Override
    public void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
        delegate.setConfiguration(openApiConfiguration);
    }

    @Override
    public OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        OpenAPI result = delegate.read(classes, resources);
        Paths paths = result.getPaths();

        Paths sortedPaths = new Paths();
        sortedPaths.extensions(paths.getExtensions());
        result.paths(sortedByOperationId(paths));
        return result;
    }

    private Paths sortedByOperationId(Paths original) {
        Paths result = new Paths();
        result.extensions(original.getExtensions());
        List<Map.Entry<String, PathItem>> entries = new ArrayList<>(original.entrySet());
        entries.sort(comparing(e -> getOperation(e.getValue()).getOperationId()));
        entries.forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    private Operation getOperation(PathItem item) {
        return Stream.of(
                item.getPost(), item.getPatch(), item.getPut(), item.getGet(), item.getDelete(), item.getOptions(),
                item.getHead(), item.getTrace())
                .filter(Objects::nonNull).findFirst().orElseThrow();
    }


}
