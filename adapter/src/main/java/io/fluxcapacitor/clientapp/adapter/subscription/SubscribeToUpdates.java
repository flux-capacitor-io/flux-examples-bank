package io.fluxcapacitor.clientapp.adapter.subscription;

import io.fluxcapacitor.clientapp.common.authentication.RequiresAppRole;
import lombok.Value;

import java.util.function.Consumer;

@Value
@RequiresAppRole
public class SubscribeToUpdates {
    Consumer<Update> consumerFunction;
}
