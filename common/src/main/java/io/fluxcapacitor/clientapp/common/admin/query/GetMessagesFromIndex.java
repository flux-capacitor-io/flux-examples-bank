package io.fluxcapacitor.clientapp.common.admin.query;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import io.fluxcapacitor.common.MessageType;
import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Value
public class GetMessagesFromIndex extends RequiresAdmin {
    @NotNull MessageType messageType;
    @PositiveOrZero long minIndex;
    @Positive int maxSize;
}
