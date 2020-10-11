package io.fluxcapacitor.clientapp.common.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;

import static io.fluxcapacitor.clientapp.common.FileUtils.loadFile;

@Value
@AllArgsConstructor
@ToString(exclude = "template")
public class EsIndex {
    private static final String stringAsKeywordTemplate = loadFile(EsIndex.class, "string-as-keyword-index-template.json");

    String index;
    String alias;
    String template;

    public static EsIndex stringAsKeyword(String name) {
        return new EsIndex(name, null, stringAsKeywordTemplate);
    }
}
