package io.fluxcapacitor.clientapp.common.admin.query;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import io.fluxcapacitor.javaclient.publishing.routing.RoutingKey;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class GetDeserializedEvents extends RequiresAdmin {
    @RoutingKey
    @NotBlank String aggregateId;
}
