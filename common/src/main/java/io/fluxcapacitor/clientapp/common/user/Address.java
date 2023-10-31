package io.fluxcapacitor.clientapp.common.user;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Address {
    String street, number, zipCode, city, country;
}
