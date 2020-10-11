package io.fluxcapacitor.clientapp.common.elasticsearch;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class Range {
    Instant gte, lte;
}
