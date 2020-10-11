package io.fluxcapacitor.clientapp.common.authentication;

import io.fluxcapacitor.javaclient.tracking.handling.authentication.AbstractUserProvider;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import org.springframework.stereotype.Component;

@Component
public class AppUserProvider extends AbstractUserProvider {
    public static final User adminSender = AppUser.builder().name("admin").role(Role.admin.name()).build();

    public AppUserProvider() {
        super(AppUser.metadataKey, AppUser.class);
    }

    @Override
    public User getActiveUser() {
        return User.current.get();
    }

    @Override
    public User getSystemUser() {
        return adminSender;
    }
}
