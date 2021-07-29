package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.IllegalCommandException;
import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;

import static io.fluxcapacitor.javaclient.modeling.AssertLegal.HIGHEST_PRIORITY;

public interface ModifyAccount extends AccountCommand {
    String getAccountId();

    @AssertLegal(priority = HIGHEST_PRIORITY)
    default void assertExists(BankAccount account) {
        if (account == null) {
            throw new IllegalCommandException("Bank account not found");
        }
    }

    @AssertLegal
    default void assertNotClosed(BankAccount account) {
        if (account.isClosed()) {
            throw new IllegalCommandException("Bank account is closed");
        }
    }
}
