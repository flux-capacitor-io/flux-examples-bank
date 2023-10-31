package com.example.flux.bank.core;

import io.fluxcapacitor.clientapp.common.bank.AccountId;
import io.fluxcapacitor.clientapp.common.bank.command.CloseAccount;
import io.fluxcapacitor.clientapp.common.bank.command.CreateAccount;
import io.fluxcapacitor.clientapp.common.bank.command.DepositMoney;
import io.fluxcapacitor.clientapp.common.bank.command.DepositTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.RollBackTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.TransferMoney;
import io.fluxcapacitor.clientapp.common.bank.query.FindAccounts;
import io.fluxcapacitor.javaclient.test.TestFixture;
import io.fluxcapacitor.javaclient.tracking.handling.IllegalCommandException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

class BankAccountTest {
    private static final CreateAccount createAccount = CreateAccount.builder().accountId(new AccountId("a")).userId("user1").build();
    private static final CreateAccount createAnotherAccount = CreateAccount.builder().accountId(new AccountId("b")).userId("user2").build();
    private static final DepositMoney depositMoney = new DepositMoney(new AccountId("a"), new BigDecimal(100));
    private static final TransferMoney transferMoney = new TransferMoney(new AccountId("a"), new AccountId("b"), BigDecimal.TEN);
    private static final DepositTransfer depositTransfer = new DepositTransfer(new AccountId("b"), new AccountId("a"), BigDecimal.TEN);
    private static final RollBackTransfer transferRollback = new RollBackTransfer(new AccountId("a"), BigDecimal.TEN);

    final TestFixture testFixture = TestFixture.create(
            new CoreHandler(), new TransferHandler(), new AccountLifecycleHandler());

    @Test
    void testCreateAccount() {
        testFixture.whenCommand(createAccount).expectEvents(createAccount);
    }

    @Test
    void testCreateAccountTwiceNotAllowed() {
        testFixture.givenCommands(createAccount).whenCommand(createAccount).expectError(IllegalCommandException.class);
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

    @Nested
    class TransferTests {

        final TestFixture testFixture = TestFixture.createAsync(
                new CoreHandler(), new TransferHandler(), new AccountLifecycleHandler());

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
            testFixture
                    .givenCommands(createAccount, depositMoney)
                    .whenCommand(transferMoney).expectEvents(transferRollback);
        }

    }
}
