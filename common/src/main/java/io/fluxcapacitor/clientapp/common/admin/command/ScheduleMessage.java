package io.fluxcapacitor.clientapp.common.admin.command;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Value
public class ScheduleMessage extends RequiresAdmin {
    @NotBlank String scheduleId;
    @NotNull Instant deadline;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @NotNull Object message;
}
