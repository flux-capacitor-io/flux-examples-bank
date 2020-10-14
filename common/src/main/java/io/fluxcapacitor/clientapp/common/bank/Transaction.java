package io.fluxcapacitor.clientapp.common.bank;

import io.fluxcapacitor.javaclient.common.serialization.DeserializingMessage;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Value
@Builder
public class Transaction {
    @NotNull Instant timestamp;
    @NotBlank String description;

    public static Transaction create(String description) {
        DeserializingMessage message = DeserializingMessage.getCurrent();
        return Transaction.builder().description(description).timestamp(
                message == null ? Instant.now() : Instant.ofEpochMilli(message.getSerializedObject().getTimestamp()))
                .build();
    }
}
