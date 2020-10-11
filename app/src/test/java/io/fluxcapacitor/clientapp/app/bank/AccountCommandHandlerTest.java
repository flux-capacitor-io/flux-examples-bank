package io.fluxcapacitor.clientapp.app.bank;

import io.fluxcapacitor.clientapp.bank.api.command.CreateAccount;
import io.fluxcapacitor.clientapp.common.IllegalCommandException;
import io.fluxcapacitor.javaclient.test.TestFixture;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class AccountCommandHandlerTest {
    private final TestFixture testFixture = TestFixture.create(new AccountCommandHandler());

    @Test
    void testCreateAccount() {
        CreateAccount command = new CreateAccount("a", BigDecimal.ZERO);
        testFixture.whenCommand(command).expectEvents(command);
    }

    @Test
    void testCreateAccountTwiceNotAllowed() {
        CreateAccount command = new CreateAccount("a", BigDecimal.ZERO);
        testFixture.givenCommands(command).whenCommand(command).expectException(IllegalCommandException.class);
    }
}
