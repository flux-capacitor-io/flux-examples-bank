package io.fluxcapacitor.clientapp.common.admin.query;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class GetStoredValue extends RequiresAdmin {
    @NotBlank String key;
}
