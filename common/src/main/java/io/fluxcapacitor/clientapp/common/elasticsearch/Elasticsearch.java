package io.fluxcapacitor.clientapp.common.elasticsearch;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.fluxcapacitor.clientapp.common.FileUtils;
import io.fluxcapacitor.clientapp.common.HttpUtils;
import io.fluxcapacitor.clientapp.common.elasticsearch.Elasticsearch.Operation.Action;
import io.fluxcapacitor.clientapp.common.elasticsearch.Elasticsearch.Operation.OperationFailure;
import io.fluxcapacitor.common.Awaitable;
import io.fluxcapacitor.common.Backlog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.fluxcapacitor.javaclient.common.serialization.jackson.JacksonSerializer.defaultObjectMapper;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;

@Slf4j
public class Elasticsearch {
    private static final Template bulkTemplate = Mustache.compiler().withEscaper(raw -> raw).compile(
            FileUtils.loadFile(Elasticsearch.class, "es.bulk.json.mustache"));
    private static final ConcurrentHashMap<Object, Object> indexes = new ConcurrentHashMap<>();

    private static final String endPoint = format("%s%s", Optional.ofNullable(getProperty("endpoint.elastic"))
                                                          .filter(endPoint -> endPoint.matches("^(http|https).*"))
                                                          .map(endPoint -> "").orElse("https://"),
                                                  getProperty("endpoint.elastic"));

    private static final ObjectReader reader = defaultObjectMapper.reader();

    private static final Client client = HttpUtils.httpClient();

    private final Backlog<CompletableOperation> backlog = new Backlog<>(completableOperations -> {
        //only perform the last modification to each document
        List<CompletableOperation> filtered = new ArrayList<>(completableOperations.stream().collect(
                toMap(CompletableOperation::identify, identity(), (a, b) -> b, LinkedHashMap::new)).values());
        List<OperationFailure> results = bulk(filtered.stream().map(CompletableOperation::get).collect(toList()));

        //send result back
        commonPool().submit(() -> {
            IntStream.range(0, results.size()).forEach(i -> {
                CompletableFuture<Void> future = filtered.get(i).getResult();
                Optional.ofNullable(results.get(i)).ifPresentOrElse(future::completeExceptionally,
                                                                    () -> future.complete(null));
            });
            completableOperations.stream().map(CompletableOperation::getResult).filter(r -> !r.isDone())
                    .forEach(r -> r.complete(null));
        });
        return Awaitable.ready();
    });

    /*
        Querying
     */

    public <T> List<T> search(ElasticsearchQuery query, Object index, Class<T> type) {
        return searchRaw(index, type, query.getSearchPayload(), query.isScroll());
    }

    public <T> List<T> search(String term, Object index, Class<?> type, Map<String, Object> constraints,
                              int maxHits, String... fields) {
        return searchRaw(index, type, ElasticsearchQuery.builder().term(term).constraints(constraints).maxHits(maxHits)
                .fields(Arrays.asList(fields)).build().getSearchPayload());
    }

    public <T> List<T> search(String term, Object index, Class<T> type, Map<String, Object> constraints,
                              String... fields) {
        return search(term, index, type, constraints, 10_000, fields);
    }

    public <T> List<T> match(Object index, Class<? extends T> type, Map<String, Object> constraints) {
        return search(null, index, type, constraints, 10_000);
    }

    public <T> List<T> match(Object index, Class<T> type, Map<String, Object> constraints, Comparator<? super T> sorter) {
        List<T> search = search(null, index, type, constraints, 10_000);
        return search.stream().sorted(sorter).collect(toList());
    }
    public <T> List<T> match(Object index, Class<T> type, Map<String, Object> constraints, int maxHits) {
        return search(null, index, type, constraints, maxHits);
    }

    public <T> Optional<T> findFirst(Object index, Class<T> type, Map<String, Object> constraints) {
        return match(index, type, constraints, 1).stream().findFirst();
    }

    public <T> List<T> matchAll(Object index, Class<?> type) {
        return searchRaw(index, type, "{\"size\": 10000, \"query\": {\"match_all\": {} } }", true);
    }
    public <T> List<T> searchRaw(Object index, Class<?> type, String searchPayload) {
        return searchRaw(index, type, searchPayload, false);
    }

    public <T> List<T> searchRaw(Object index, Class<?> type, String searchPayload, boolean scroll) {
        createIndexIfNotExists(index);
        WebTarget target = getSearchTarget(index);
        if (scroll) {
            target = target.queryParam("scroll", "1m");
        }
        try (Response response = target.request().header("Accept-Encoding", "gzip").post(json(searchPayload))) {
            return ElasticsearchResult.extractResults(response, type);
        }
    }

    /*
        Modifications
     */
    public boolean deleteIndex(Object index) {
        createIndexIfNotExists(index);
        WebTarget target = client.target(
                format("%s/%s", endPoint, getAliasOrIndex(index)));
        try (Response response = target.request().header("Accept-Encoding", "gzip").delete()) {
            return response.getStatusInfo().getFamily() == Family.SUCCESSFUL;
        }
    }

    public boolean createIndex(Object index) {
        String template = index instanceof EsIndex ? ((EsIndex) index).getTemplate() : EsIndex.stringAsKeyword((String)index).getTemplate();
        String alias = index instanceof EsIndex ? ((EsIndex) index).getAlias() : null;

        if (aliasExists(alias)) {
            return false;
        }

        index = index instanceof EsIndex ? ((EsIndex) index).getIndex() : (String) index;
        WebTarget target = client.target(format("%s/%s", endPoint, index));
        try (Response response = target.request().header("Accept-Encoding", "gzip").put(json(template))) {
            boolean success = response.getStatusInfo().getFamily() == SUCCESSFUL;
            if (success) {
                log.info("Created new elasticsearch index {}", index);
                if (alias != null) {
                    createAlias((String) index, alias);
                }
            } else {
                log.info("Did not create elasticsearch index {}. Reason: {}", index, readEntity(response));
            }
            return success;
        }
    }

    public void createAlias(String index, String alias) {
        WebTarget target = client.target(format("%s/%s/_alias/%s", endPoint, index, alias));
        try (Response response = target.request().header("Accept-Encoding", "gzip").put(json("{}"))) {
            boolean success = response.getStatusInfo().getFamily() == SUCCESSFUL;
            if (success) {
                log.info("Created new elasticsearch alias {} for index {}", alias, index);
            } else {
                log.error("Did not create elasticsearch alias {} for index {}. Reason: {}", alias, index,
                          readEntity(response));
            }
        }
    }

    @SneakyThrows
    public void insertIfNotExists(Object value, Object index, String id) {
        createIndexIfNotExists(index);
        String json = asJson(value);
        WebTarget target = getCreateTarget(index, id);
        try (Response response = target.request().header("Accept-Encoding", "gzip").put(json(json))) {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL && response.getStatus() != 409) {
                throw new IllegalStateException(
                        format("Failed to put value %s into index %s (code %s): %s", json, index, response.getStatus(),
                               readEntity(response)));
            }
        }
    }

    public CompletableFuture<Void> upsert(Supplier<?> valueSupplier, Object index, String id) {
        createIndexIfNotExists(index);
        return submitBulkOperation(Action.index, index, id, () -> asJson(valueSupplier.get()));
    }

    public CompletableFuture<Void> upsert(Object value, Object index, String id) {
        createIndexIfNotExists(index);
        return submitBulkOperation(Action.index, index, id, () -> asJson(value));
    }

    public void upsertAndWait(Object value, Object index, String id) {
        createIndexIfNotExists(index);
        String json = asJson(value);
        WebTarget target = getEntryTarget(index, id);
        try (Response response = target.request().header("Accept-Encoding", "gzip").post(json(json))) {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new IllegalStateException(
                        format("Failed to post value %s into index %s (code %s): %s", json, index, response.getStatus(),
                               readEntity(response)));
            }
        }
    }

    public CompletableFuture<Void> delete(Object index, String id) {
        createIndexIfNotExists(index);
        return submitBulkOperation(Action.delete, index, id, () -> "{}");
    }

    public void deleteAndWait(Object index, String id) {
        createIndexIfNotExists(index);
        WebTarget target = getEntryTarget(index, id);
        try (Response response = target.request().header("Accept-Encoding", "gzip").delete()) {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL && response.getStatus() != 404) {
                throw new IllegalStateException(
                        format("Failed to delete value with id %s of index %s (code %s): %s", id, index,
                               response.getStatus(),
                               readEntity(response)));
            }
        }
    }

    @SneakyThrows
    public int deleteByQuery(Object index, Map<String, Object> constraints) {
        createIndexIfNotExists(index);
        WebTarget target = getDeleteByQueryTarget(index);
        String payload = ElasticsearchQuery.builder().constraints(constraints).build().getSearchPayload();
        try (Response response = target.request().header("Accept-Encoding", "gzip").post(json(payload))) {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new IllegalStateException(
                        format("Failed to perform delete by query (code %s): %s", response.getStatus(),
                               readEntity(response)));
            }
            JsonNode result = reader.readTree(response.readEntity(InputStream.class));
            if (result.get("failures").size() == 0) {
                return result.get("deleted").intValue();
            }
            throw new IllegalStateException(format("Failed to perform delete: %s", result));
        }
    }

    @SneakyThrows
    public List<OperationFailure> bulk(List<Operation> operations) {
        operations.stream().map(Operation::getIndex).distinct().forEach(this::createIndexIfNotExists);
        String payload = getBulkPayload(operations);
        WebTarget target = client.target(format("%s/_bulk", endPoint));
        try (Response response = target.request().header("Accept-Encoding", "gzip").post(json(payload))) {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new IllegalStateException(
                        format("Failed to perform bulk operation (code %s): %s", response.getStatus(),
                               readEntity(response)));
            }
            BulkResult result = defaultObjectMapper.readValue(response.readEntity(byte[].class), BulkResult.class);
            if (result.errors) {
                return IntStream.range(0, result.items.size()).mapToObj(i -> {
                    Operation operation = operations.get(i);
                    BulkResult.BulkResultItem.BulkResultItemContent item = result.items.get(i).result;
                    boolean error = item.error != null || (familyOf(item.status) != SUCCESSFUL && item.status != 404);
                    return error ? new OperationFailure(format(
                            "Failed to perform operation #%s: %s. Error: %s", i, operation, item), operation) : null;
                }).collect(toList());
            }
            return IntStream.range(0, operations.size()).mapToObj(i -> (OperationFailure) null).collect(toList());
        }
    }


    /*
        Helper methods
     */

    @PreDestroy
    public void shutDown() {
        client.close();
    }

    private boolean aliasExists(String alias) {
        if (alias != null) {
            WebTarget target = client.target(format("%s/_alias/%s", endPoint, alias));
            try (Response response = target.request().header("Accept-Encoding", "gzip").get()) {
                return response.getStatusInfo().getFamily() == Family.SUCCESSFUL;
            }
        }
        return false;
    }

    private CompletableFuture<Void> submitBulkOperation(Action action, Object index, String id,
                                                        Supplier<String> payload) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        backlog.add(new CompletableOperation(action, index, id, payload, result));
        return result;
    }

    private void createIndexIfNotExists(Object index) {
        indexes.computeIfAbsent(index, i -> createIndex(index));
    }

    private static String getBulkPayload(List<Operation> operations) {
        return bulkTemplate.execute(new BulkOperation(operations));
    }

    static String getSearchPayload(ElasticsearchQuery query) {
        return query.getSearchPayload();
    }

    private WebTarget getEntryTarget(Object index, String id) {
        return client.target(format("%s/%s/_doc/%s", endPoint, getAliasOrIndex(index), id));
    }

    private WebTarget getCreateTarget(Object index, String id) {
        return client.target(format("%s/%s/_doc/%s/_create", endPoint, getAliasOrIndex(index), id));
    }

    private WebTarget getSearchTarget(Object index) {
        return client.target(format("%s/%s/_search", endPoint, getAliasOrIndex(index)));
    }

    private WebTarget getDeleteByQueryTarget(Object index) {
        return client.target(format("%s/%s/_delete_by_query", endPoint, getAliasOrIndex(index)));
    }

    private WebTarget getDeleteByQueryTarget(String index) {
        return client.target(format("%s/%s/_delete_by_query", endPoint, index));
    }

    private static String getAliasOrIndex(Object index) {
        return index instanceof EsIndex ? ((EsIndex) index).getAlias() : (String) index;
    }

    private static String readEntity(Response response) {
        try {
            return response.getEntity() == null ? null : response.readEntity(String.class);
        } catch (Exception ignored) {
            return response.getEntity().toString();
        }
    }

    @SneakyThrows
    private static String asJson(Object value) {
        return value instanceof String ? (String) value : defaultObjectMapper.writeValueAsString(value);
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
        String type;
        String id;
        String json;

        public Operation(Action action, Object index, String id, String json) {
            this(action, index, "_doc", id, json);
        }

        public Operation(Action action, Object index, String id, Object object) {
            this(action, index, "_doc", id, object);
        }

        public Operation(Action action, Object index, String type, String id, Object object) {
            this(action, index, type, id, asJson(object));
        }

        public Operation(Action action, Object index, String type, String id, String json) {
            this.action = action;
            this.index = getAliasOrIndex(index);
            this.type = type;
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
    private static class CompletableOperation {
        private final Action action;
        private final Object index;
        private final String id;
        private final Supplier<String> payload;
        private CompletableFuture<Void> result;

        public String identify() {
            return getAliasOrIndex(index) + id + action;
        }

        public Operation get() {
            return new Operation(action, index, id, payload.get());
        }
    }

    @Value
    public static class ElasticsearchResult<T> {
        ResultHits<T> hits;

        @Value
        public static class ResultHits<T> {
            List<ResultHit<T>> hits;

            @Value
            public static class ResultHit<T> {
                @JsonProperty("_source")
                T source;
            }
        }

        @SuppressWarnings({"unchecked"})
        @SneakyThrows
        public static <T> List<T> extractResults(Response response, Class<?> resultType) {
            if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
                JsonNode json = reader.readTree(response.readEntity(InputStream.class));
                ArrayNode hits = (ArrayNode) json.get("hits").get("hits");
                List<T> result = (List<T>) stream(hits.spliterator(), false).map(n -> n.get("_source")).map(
                        value -> defaultObjectMapper.convertValue(value, resultType)).collect(toList());
                if (!result.isEmpty() && json.get("_scroll_id") != null) {
                    result.addAll(extractResults(scroll(json.get("_scroll_id").asText()), resultType));
                }
                return result;
            }
            throw new IllegalStateException(
                    format("Failed to execute Elasticsearch query (code %s): %s", response.getStatus(),
                           readEntity(response)));
        }


        @SuppressWarnings({"unchecked"})
        @SneakyThrows
        public static <T> List<T> extractResults(String jsonString, Class<T> resultType) {
            JsonNode json = reader.readTree(jsonString);
            ArrayNode hits = (ArrayNode) json.get("hits").get("hits");
            return (List<T>) stream(hits.spliterator(), false).map(n -> n.get("_source")).map(
                    value -> defaultObjectMapper.convertValue(value, resultType)).collect(toList());
        }

        private static Response scroll(String scrollId) {
            WebTarget target = client.target(format("%s/_search/scroll", endPoint));
            return target.request().header("Accept-Encoding", "gzip")
                    .post(json(format("{\"scroll\" : \"1m\", \"scroll_id\" : \"%s\"}", scrollId)));
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
            String output = defaultObjectMapper.writeValueAsString(raw);
            out.write(output.substring(1, output.length() - 1));
        });

        //escape reserved characters that function as query operators:
        //https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters
        protected Mustache.Lambda escapeQueryString = ((frag, out) -> {
            String raw = frag.execute();
            String output = operatorRegex.matcher(raw).replaceAll("\\\\$1");
            output = defaultObjectMapper.writeValueAsString(output);
            out.write(output.substring(1, output.length() - 1));
        });

        @Value
        static class Constraint {
            String key;
            List<Object> values;
            boolean range;

            public static List<Constraint> asConstraints(Map<String, Object> constraints) {
                return constraints.entrySet().stream().map(e -> new Constraint(
                        e.getKey(), asList(e.getValue(), !e.getKey().endsWith(".keyword")),
                        e.getValue() instanceof Range)).collect(toList());
            }

            private static List<Object> asList(Object value, boolean stringToLowerCase) {
                return (value instanceof Collection<?> ? ((Collection<?>) value).stream() : Stream.of(value))
                        .map(v -> stringToLowerCase && v instanceof String ? ((String) v).toLowerCase() : v == null
                                ? false : v instanceof Range ? v
                                : stringToLowerCase ? v.toString().toLowerCase() : v.toString()).collect(toList());
            }
        }
    }
}
