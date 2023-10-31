package io.fluxcapacitor.clientapp.common.bank.command;

import io.fluxcapacitor.clientapp.common.bank.BankAccount;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.tracking.handling.IllegalCommandException;
import jakarta.annotation.Nullable;

import static io.fluxcapacitor.javaclient.modeling.AssertLegal.HIGHEST_PRIORITY;

public interface ModifyAccount extends AccountCommand {

    @AssertLegal(priority = HIGHEST_PRIORITY)
    default void assertExists(@Nullable BankAccount account) {
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
