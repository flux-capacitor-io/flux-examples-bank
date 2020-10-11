package io.fluxcapacitor.clientapp.common.admin.query;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import io.fluxcapacitor.javaclient.publishing.routing.RoutingKey;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Value
public class GetEventBatch extends RequiresAdmin {
    @RoutingKey
    @NotBlank String aggregateId;
    @Positive Long lastSequenceNumber;
    @Positive Integer batchSize;
}
