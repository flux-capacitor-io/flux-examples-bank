package io.fluxcapacitor.clientapp.common.bank;

import io.fluxcapacitor.javaclient.modeling.Id;

public class AccountId extends Id<BankAccount> {
    public AccountId(String accountId) {
        super(accountId, BankAccount.class);
    }
}
