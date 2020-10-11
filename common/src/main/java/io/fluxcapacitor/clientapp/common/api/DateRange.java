package io.fluxcapacitor.clientapp.common.api;

import lombok.Value;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Value
public class DateRange {
    @NotNull LocalDate start, end;

    @AssertTrue(message = "Start date cannot be after end date.")
    public boolean hasCorrectOrder(){
        return !start.isAfter(end);
    }
}
