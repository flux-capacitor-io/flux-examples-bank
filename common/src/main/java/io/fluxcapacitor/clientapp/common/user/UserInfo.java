package io.fluxcapacitor.clientapp.common.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class UserInfo {
    @NotBlank String firstName;
    @NotBlank String lastName;
    @NotBlank @Email String email;
    @Valid Address address;
    String telephoneNumber;
    boolean publicProfile;
}
