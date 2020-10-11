package io.fluxcapacitor.clientapp.common.admin.command;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Value
public class RetryEvents extends RequiresAdmin {
    @NotNull List<@NotBlank String> indexes;
}
