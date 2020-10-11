package io.fluxcapacitor.clientapp.common.admin.command;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class DeleteValue extends RequiresAdmin {
    @NotBlank String key;
}
