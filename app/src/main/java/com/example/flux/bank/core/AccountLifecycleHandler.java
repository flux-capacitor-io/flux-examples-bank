package com.example.flux.bank.core;

import io.fluxcapacitor.clientapp.common.bank.command.CloseAccount;
import io.fluxcapacitor.clientapp.common.bank.command.CreateAccount;
import io.fluxcapacitor.clientapp.common.bank.command.ModifyAccount;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleEvent;
import io.fluxcapacitor.javaclient.tracking.handling.HandleSchedule;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AccountLifecycleHandler {
    final static Duration MAX_INACTIVITY = Duration.ofDays(60);

    @HandleEvent
    void handle(CreateAccount event) {
        FluxCapacitor.schedule(
                new CloseAccount(event.getAccountId(), "Your account was closed automatically due to inactivity"),
                "closeAccount:" + event.getAccountId(), MAX_INACTIVITY);
    }

    @HandleEvent
    void handle(ModifyAccount event) {
        FluxCapacitor.cancelSchedule("closeAccount:" + event.getAccountId());
    }

    @HandleSchedule
    void handle(CloseAccount schedule) {
        FluxCapacitor.sendAndForgetCommand(schedule);
    }
}
