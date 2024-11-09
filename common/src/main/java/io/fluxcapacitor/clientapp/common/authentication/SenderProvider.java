package io.fluxcapacitor.clientapp.common.authentication;

import io.fluxcapacitor.clientapp.common.user.UserId;
import io.fluxcapacitor.common.MessageType;
import io.fluxcapacitor.javaclient.common.HasMessage;
import io.fluxcapacitor.javaclient.common.serialization.DeserializingMessage;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.AbstractUserProvider;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import io.fluxcapacitor.javaclient.web.WebRequest;

public class SenderProvider extends AbstractUserProvider {

    public static final Sender system = Sender.builder()
            .userId(new UserId("system")).userRole(Role.system).build();

    public SenderProvider() {
        super(Sender.class);
    }

    @Override
    public User fromMessage(HasMessage message) {
        if (message instanceof DeserializingMessage dm && dm.getMessageType() == MessageType.WEBREQUEST) {
            return AuthenticationUtils.getSender((WebRequest) dm.toMessage());
        }
        return super.fromMessage(message);
    }

    @Override
    public User getUserById(Object userId) {
        return userId == null ? null : Sender.builder().userId(
                userId instanceof UserId uId ? uId : new UserId(userId.toString())).build();
    }

    @Override
    public User getSystemUser() {
        return system;
    }
}
