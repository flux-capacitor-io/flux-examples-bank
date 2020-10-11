package io.fluxcapacitor.clientapp.common.admin.command;

import io.fluxcapacitor.clientapp.common.admin.RequiresAdmin;
import lombok.Value;

@Value
public class EncryptValue extends RequiresAdmin {
    String value;
}
