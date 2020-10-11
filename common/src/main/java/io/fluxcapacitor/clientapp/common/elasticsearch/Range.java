package io.fluxcapacitor.clientapp.common.elasticsearch;

import io.fluxcapacitor.clientapp.common.api.DateRange;
import io.fluxcapacitor.clientapp.common.api.DateTimeRange;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Range {
    Object gte, lte;

    public static Range fromDateTimeRange(DateTimeRange range) {
        return Range.builder().gte(range.getStart()).lte(range.getEnd()).build();
    }

    public static Range fromDateRange(DateRange range) {
        return Range.builder().gte(range.getStart()).lte(range.getEnd()).build();
    }
}
