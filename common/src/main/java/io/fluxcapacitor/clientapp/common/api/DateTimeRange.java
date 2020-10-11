package io.fluxcapacitor.clientapp.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.NonFinal;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
@NonFinal
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DateTimeRange {
    @NotNull Instant start, end;

    @AssertTrue(message = "Start datetime cannot be after end datetime.")
    public boolean hasCorrectOrder(){
        return !start.isAfter(end);
    }

    public boolean contains(@NonNull Instant timestamp) {
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }

    public boolean contains(Instant start, Instant end) {
        return start == null || contains(start) || end == null || contains(end);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return start == null && end == null;
    }
}
