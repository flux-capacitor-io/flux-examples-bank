package io.fluxcapacitor.clientapp.common.admin.command;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import io.fluxcapacitor.common.MessageType;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Value
public class DisconnectTracker extends RequiresAdmin {
    @NotNull MessageType messageType;
    @NotBlank String consumer, trackingId;
    @NotNull Boolean sendFinalEmptyBatch;
}
