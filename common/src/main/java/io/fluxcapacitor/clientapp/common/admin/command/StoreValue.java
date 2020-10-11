package io.fluxcapacitor.clientapp.common.admin.command;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;

@Value
public class StoreValue extends RequiresAdmin {
    @NotBlank String key;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = WRAPPER_OBJECT)
    @NotNull Object value;
}
