package com.example.flux.auditlog;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import static io.fluxcapacitor.common.FileUtils.loadFile;
import static io.fluxcapacitor.javaclient.common.serialization.jackson.JacksonSerializer.defaultObjectMapper;
import static io.fluxcapacitor.javaclient.configuration.ApplicationProperties.getProperty;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

;

@Slf4j
public class OpenSearch {

    private static final Executor asyncResultExecutor = Executors.newFixedThreadPool(16);
    private static final Template bulkTemplate = Mustache.compiler().withEscaper(raw -> raw).compile(
            loadFile(OpenSearch.class, "/com/example/flux/auditlog/bulk.mustache"));
    private static final String endPoint = getProperty("OPENSEARCH_URL");

    private static final ObjectMapper objectMapper =
            defaultObjectMapper.copy().setSerializationInclusion(JsonInclude.Include.ALWAYS);
    private static final ObjectMapper objectMapperNoNullValues =
            defaultObjectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final HttpClient client = HttpClient.newBuilder()
            .authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(getProperty("AUDITLOG_USER"),
                                                      getProperty("AUDITLOG_PASSWORD").toCharArray());
                }
            })
            .version(HttpClient.Version.HTTP_1_1)
            .executor(asyncResultExecutor)
            .build();

    private static final HttpResponse.BodyHandler<InputStream> gzipAwareBodyHandler = responseInfo -> {
        if (responseInfo.headers().firstValue("Content-Encoding").map("gzip"::equals).orElse(false)) {
            return BodySubscribers.mapping(BodySubscribers.ofInputStream(), OpenSearch::wrapWithGzipStream);
        } else {
            return BodySubscribers.ofInputStream();
        }
    };
    private static final Set<Integer> NON_RETRY_STATUS_CODES = Set.of(400, 413);

    @SneakyThrows
    private static GZIPInputStream wrapWithGzipStream(InputStream in) {
        return new GZIPInputStream(in);
    }

    private CompletableFuture<Void> handleBacklogBatch(List<CompletableOperation> completableOperations) {
        //only perform the last modification to each document
        List<CompletableOperation> filtered = new ArrayList<>(completableOperations.stream()
                                                                      .collect(toMap(CompletableOperation::identify,
                                                                                     identity(), (a, b) -> b,
                                                                                     LinkedHashMap::new))
                                                                      .values());
        List<Operation.OperationFailure> results = bulk(filtered.stream()
                                                                .map(CompletableOperation::get)
                                                                .collect(toList()));

        //send result back
        return CompletableFuture.runAsync(() -> {
            IntStream.range(0, results.size())
                    .forEach(i -> {
                                 CompletableFuture<Void> future = filtered.get(i).getResult();
                                 ofNullable(results.get(i))
                                         .ifPresentOrElse(
                                                 future::completeExceptionally,
                                                 () -> future.complete(null)
                                         );
                             }
                    );
            completableOperations.stream()
                    .map(CompletableOperation::getResult)
                    .filter(r -> !r.isDone())
                    .forEach(r -> r.complete(null));
        }, asyncResultExecutor);
    }

    /*
        Querying
     */

    private static HttpRequest.Builder requestBuilder(String uri) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Accept-Encoding", "gzip")
                .header("Content-type", "application/json");
    }

    private static boolean isSuccessful(HttpResponse<?> response) {
        return response.statusCode() / 100 == 2;
    }

    @SneakyThrows
    public void initialize() {
        var putLogIndexTemplate = requestBuilder(endPoint + "/_index_template/log")
                .PUT(BodyPublishers.ofString(loadFile("/com/example/flux/auditlog/index-template-log.json")))
                .build();
        try (var ignored = retryRequest(putLogIndexTemplate)) {
            log.info("Created log index template");
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class ElasticsearchDeleteResult {
        Integer deleted;
        Integer failures;
        Integer duration;
        Integer total;
    }

    @SneakyThrows
    public List<Operation.OperationFailure> bulk(Collection<Operation> values) {
        var operations = List.copyOf(values);
        if (operations.isEmpty()) {
            return List.of();
        }
        String payload = getBulkPayload(operations);
        HttpRequest request = requestBuilder(endPoint + "/_bulk").POST(BodyPublishers.ofString(payload)).build();

        InputStream response = retryRequest(request);
        BulkResult result = objectMapper.readValue(response, BulkResult.class);
        if (result.errors) {
            return IntStream.range(0, result.items.size()).mapToObj(i -> {
                Operation operation = operations.get(i);
                BulkResult.BulkResultItem.BulkResultItemContent item = result.items.get(i).result;
                boolean error = item.error != null && item.status != 404 && item.status != 409;
                return error ? new Operation.OperationFailure(format("Failed to perform operation #%s: %s. Error: %s",
                                                                     i, operation, item), operation) : null;
            }).collect(toList());
        }
        return IntStream.range(0, operations.size()).mapToObj(i -> (Operation.OperationFailure) null).collect(toList());
    }

    public InputStream retryRequest(HttpRequest request) throws InterruptedException {
        while (true) {
            try {
                return client.sendAsync(request, gzipAwareBodyHandler)
                        .thenComposeAsync(resp -> tryResend(request, resp))
                        .get().body();
            } catch (RuntimeException | ExecutionException e) {
                log.info("Request failed, retrying in 5 seconds", e);
                Thread.sleep(5000L);
            }
        }
    }

    private CompletableFuture<HttpResponse<InputStream>> tryResend(HttpRequest request, HttpResponse<InputStream> response) {
        if (isSuccessful(response)) {
            return CompletableFuture.completedFuture(response);
        }

        String responseBody;
        try (InputStream in = response.body()) {
            responseBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.info("Unable to read response body of failed request", e);
            responseBody = "<unreadable>";
        }
        log.info("Request failed with status code '%d' response: '%s'".formatted(response.statusCode(), responseBody));

        if (NON_RETRY_STATUS_CODES.contains(response.statusCode())) {
            log.info("Not retrying invalid request");
            return CompletableFuture.completedFuture(response);
        }

        return client.sendAsync(request, gzipAwareBodyHandler).thenComposeAsync(r -> tryResend(request, response));
    }

    public static String getBulkPayload(List<Operation> operations) {
        return bulkTemplate.execute(new BulkOperation(operations));
    }

    public static String getAliasOrIndex(Object index) {
        if (index instanceof EsIndex esIndex) {
            return Optional.ofNullable(esIndex.getAlias()).orElse(esIndex.getIndex());
        }
        return index.toString();
    }

    @SneakyThrows
    private static String asJson(Object value) {
        return value instanceof String ? (String) value : objectMapperNoNullValues.writeValueAsString(value);
    }

    @Value
    private static class BulkOperation {
        List<Operation> operations;
    }

    @Value
    @ToString(exclude = "json")
    public static class Operation {
        @NotNull Action action;
        @NotBlank String index;
        String id;
        String json;

        @JsonProperty("size")
        public long size() {
            return json.getBytes().length;
        }

        public Operation(Action action, Object index, String id, Object object) {
            this(action, index, id, asJson(object));
        }

        public Operation(Action action, Object index, String id, String json) {
            this.action = action;
            this.index = getAliasOrIndex(index);
            this.id = id;
            this.json = json;
        }

        public String getJson() {
            return action == Action.delete ? null : json;
        }

        public enum Action {
            create, update, delete, index
        }

        @Getter
        public static class OperationFailure extends Exception {
            private final Operation operation;

            public OperationFailure(String message, Operation operation) {
                super(message);
                this.operation = operation;
            }
        }
    }

    @AllArgsConstructor
    @Getter
    @ToString
    private static class CompletableOperation {

        private final Operation.Action action;
        private final Object index;
        private final String id;
        @ToString.Exclude
        private final Supplier<String> payload;
        private final String currentMessage;


        @ToString.Exclude
        private final CompletableFuture<Void> result;

        @JsonProperty("payloadLength")
        public long getPayloadLength() {
            return payload.get().getBytes().length;
        }

        public String identify() {
            return getAliasOrIndex(index) + id + action;
        }

        public Operation get() {
            return new Operation(action, index, id, payload.get());
        }


    }

    @Value
    private static class BulkResult {
        boolean errors;
        List<BulkResultItem> items;

        @Value
        private static class BulkResultItem {

            @JsonAlias({"create", "update", "delete", "index"})
            BulkResultItemContent result;

            @Value
            private static class BulkResultItemContent {
                int status;
                Object error;
            }
        }
    }

    @Value
    static class Search {
        private static final Pattern operatorRegex = Pattern.compile("([+-=><!(){}\\[\\]^\"~?:/\\\\]|[&|]{2})");

        Integer maxHits;
        List<String> fields;
        String term;
        List<Constraint> constraints;
        List<String> includedFields;
        List<String> excludedFields;

        //escape json string
        protected Mustache.Lambda escape = ((frag, out) -> {
            String raw = frag.execute();
            String output = objectMapper.writeValueAsString(raw);
            out.write(output.substring(1, output.length() - 1));
        });

        //escape reserved characters that function as query operators:
        //https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters
        protected Mustache.Lambda escapeQueryString = ((frag, out) -> {
            String raw = frag.execute();
            String output = operatorRegex.matcher(raw).replaceAll("\\\\$1");
            output = objectMapper.writeValueAsString(output);
            out.write(output.substring(1, output.length() - 1));
        });

        @Value
        static class Constraint {
            String key;
            List<Object> values;
            boolean range;
        }
    }
}
