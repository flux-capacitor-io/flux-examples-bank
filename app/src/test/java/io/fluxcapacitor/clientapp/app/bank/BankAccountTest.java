package io.fluxcapacitor.clientapp.app.bank;

import io.fluxcapacitor.clientapp.common.IllegalCommandException;
import io.fluxcapacitor.clientapp.common.bank.command.CloseAccount;
import io.fluxcapacitor.clientapp.common.bank.command.CreateAccount;
import io.fluxcapacitor.clientapp.common.bank.command.DepositMoney;
import io.fluxcapacitor.clientapp.common.bank.command.DepositTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.RevertTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.TransferMoney;
import io.fluxcapacitor.clientapp.common.bank.query.FindAccounts;
import io.fluxcapacitor.javaclient.test.TestFixture;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

class BankAccountTest {
    private static final CreateAccount createAccount = CreateAccount.builder().accountId("a").userId("user1").build();
    private static final CreateAccount createAnotherAccount = CreateAccount.builder().accountId("b").userId("user2").build();
    private static final DepositMoney depositMoney = new DepositMoney("a", new BigDecimal(100));
    private static final TransferMoney transferMoney = new TransferMoney("a", "b", BigDecimal.TEN);
    private static final DepositTransfer depositTransfer = new DepositTransfer("b", "a", BigDecimal.TEN);
    private static final RevertTransfer revertTransfer = new RevertTransfer("a", BigDecimal.TEN);

    private final TestFixture testFixture = TestFixture.create(new AccountCommandHandler(), new TransferEventHandler(),
                                                               new AccountLifecycleHandler(), new BankQueryHandler());

    @Test
    void testCreateAccount() {
        testFixture.whenCommand(createAccount).expectEvents(createAccount);
    }

    @Test
    void testCreateAccountTwiceNotAllowed() {
        testFixture.givenCommands(createAccount).whenCommand(createAccount).expectError(IllegalCommandException.class);
    }

    @Test
    void testMoneyTransfer() {
        testFixture.givenCommands(createAccount, depositMoney, createAnotherAccount)
                .whenCommand(transferMoney).expectEvents(transferMoney, depositTransfer);
    }

    @Test
    void testTransferNotAllowedWithInsufficientFunds() {
        testFixture.givenCommands(createAccount, createAnotherAccount)
                .whenCommand(transferMoney).expectError(IllegalCommandException.class).expectNoEvents();
    }

    @Test
    void testTransferNotAllowedIfOtherAccountDoesNotExist() {
        testFixture.givenCommands(createAccount, depositMoney)
                .whenCommand(transferMoney).expectEvents(revertTransfer);
    }

    @Test
    void testNewAccountIsClosedAfterInactivity() {
        testFixture.givenCommands(createAccount)
                .whenTimeElapses(AccountLifecycleHandler.MAX_INACTIVITY)
                .expectEvents((Predicate<Object>) e -> e instanceof CloseAccount);
    }

    @Test
    void testAccountIsNotClosedIfTheresActivity() {
        testFixture.givenCommands(createAccount, depositMoney)
                .whenTimeElapses(AccountLifecycleHandler.MAX_INACTIVITY)
                .expectNoEvents();
    }

    @Test
    void testFindAccounts() {
        testFixture.givenCommands(createAccount, depositMoney)
                .whenQuery(new FindAccounts("user1")).<List<?>>expectResult(r -> r.size() == 1);
    }
}
