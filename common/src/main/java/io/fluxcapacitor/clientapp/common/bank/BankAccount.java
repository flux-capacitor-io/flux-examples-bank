package io.fluxcapacitor.clientapp.common.bank;

import io.fluxcapacitor.javaclient.modeling.Aggregate;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Aggregate(searchable = true)
@Value
@Builder(toBuilder = true)
public class BankAccount {
    AccountId accountId;
    String userId;
    BigDecimal maxOverdraft;
    boolean closed;

    @Builder.Default
    BigDecimal balance = BigDecimal.ZERO;

    @Singular
    List<Transaction> transactions;
}
