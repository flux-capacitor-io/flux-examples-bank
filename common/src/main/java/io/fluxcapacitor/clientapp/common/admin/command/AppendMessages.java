package io.fluxcapacitor.clientapp.common.admin.command;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import io.fluxcapacitor.common.MessageType;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Value
public class AppendMessages extends RequiresAdmin {
    @NotNull MessageType type;
    @NotNull @NotEmpty List<Object> messages;
}
