package io.fluxcapacitor.clientapp.common.admin.command;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import io.fluxcapacitor.common.api.Metadata;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Value
public class StoreDomainEvent extends RequiresAdmin {
    @PositiveOrZero long sequenceNumber;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @NotNull Object event;
    @NotBlank String domain;
    @NotBlank String aggregateId;
    @NotNull Metadata metadata;
}
