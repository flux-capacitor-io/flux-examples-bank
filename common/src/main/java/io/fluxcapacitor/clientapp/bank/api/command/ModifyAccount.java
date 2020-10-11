package io.fluxcapacitor.clientapp.bank.api.command;

import io.fluxcapacitor.clientapp.bank.api.BankAccount;
import io.fluxcapacitor.clientapp.common.IllegalCommandException;
import io.fluxcapacitor.javaclient.modeling.AssertLegal;
import io.fluxcapacitor.javaclient.publishing.routing.RoutingKey;

import javax.validation.constraints.NotBlank;

import static io.fluxcapacitor.javaclient.modeling.AssertLegal.HIGHEST_PRIORITY;

public interface ModifyAccount extends AccountCommand {
    @RoutingKey
    @NotBlank
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
