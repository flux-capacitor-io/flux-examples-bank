package io.fluxcapacitor.clientapp.common.admin.command;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import io.fluxcapacitor.common.MessageType;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Value
public class ResetTrackingPosition extends RequiresAdmin {
    @NotBlank String consumer;
    @NotNull MessageType messageType;
    long lastIndex;
}
