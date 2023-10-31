package com.example.flux.auditlog;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;

@Value
@AllArgsConstructor
@ToString(exclude = "template")
public class EsIndex {
    String index;
    String alias;
    String template;

    public EsIndex(String index, String alias){
        this.index = index;
        this.alias = alias;
        this.template = "{}";
    }
}
