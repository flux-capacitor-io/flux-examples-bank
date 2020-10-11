package io.fluxcapacitor.clientapp.common.admin.command;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import lombok.Value;

import javax.validation.constraints.Positive;

@Value
public class RetryEvent extends RequiresAdmin {
    @Positive long index;
}
