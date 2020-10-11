package io.fluxcapacitor.clientapp.common.elasticsearch;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.fluxcapacitor.clientapp.common.FileUtils;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Delegate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.fluxcapacitor.clientapp.common.elasticsearch.ElasticsearchQuery.Search.Constraint.asConstraints;
import static io.fluxcapacitor.javaclient.common.serialization.jackson.JacksonSerializer.defaultObjectMapper;
import static java.util.stream.Collectors.toList;

@Value
@Builder(toBuilder = true)
public class ElasticsearchQuery {
    private static final Template searchTemplate = Mustache.compiler().withEscaper(raw -> raw).compile(
            FileUtils.loadFile(Elasticsearch.class, "es.search.json.mustache"));

    String term;
    @Singular
    Map<String, Object> constraints;
    @Singular
    Map<String, Object> mustNotConstraints;
    @Default
    int from = 0;
    @Default
    int maxHits = 10_000;
    @Default
    boolean scroll = false;
    @Singular
    List<String> fields;
    @Singular("sortBy")
    List<String> sorting;
    @Singular
    List<String> includedFields;
    @Singular
    List<String> excludedFields;

    public String getSearchPayload() {
        return searchTemplate.execute(new Search(this, asConstraints(constraints), asConstraints(mustNotConstraints)));
    }

    @Value
    static class Search {
        private static final Pattern operatorRegex = Pattern.compile("([+-=><!(){}\\[\\]^\"~?:/\\\\]|[&|]{2})");

        @Delegate
        ElasticsearchQuery query;
        List<Constraint> constraintList;
        List<Constraint> mustNotConstraintList;

        protected boolean paging() {
            return getFrom() > 0;
        }

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
