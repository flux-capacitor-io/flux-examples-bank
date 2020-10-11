package io.fluxcapacitor.clientapp.adapter.subscription;

import lombok.Value;

@Value
public class Update {
    String routingKey;
    Object value;
}
