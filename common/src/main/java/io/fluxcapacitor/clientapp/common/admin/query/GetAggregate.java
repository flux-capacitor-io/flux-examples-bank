package io.fluxcapacitor.clientapp.common.admin.query;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class GetAggregate extends RequiresAdmin {
    @NotBlank String aggregateId;
    @NotBlank String className;
}
