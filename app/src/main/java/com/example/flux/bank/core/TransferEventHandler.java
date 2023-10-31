package com.example.flux.bank.core;

import io.fluxcapacitor.clientapp.common.bank.command.DepositTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.RevertTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.TransferMoney;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleError;
import io.fluxcapacitor.javaclient.tracking.handling.HandleEvent;
import io.fluxcapacitor.javaclient.tracking.handling.Trigger;
import org.springframework.stereotype.Component;

import static io.fluxcapacitor.common.MessageType.COMMAND;

@Component
public class TransferEventHandler {
    @HandleEvent
    void handle(TransferMoney command) {
        FluxCapacitor.sendAndForgetCommand(
                new DepositTransfer(command.getDestinationAccountId(), command.getAccountId(),
                                    command.getAmount()));
    }

    @HandleError
    void handle(@Trigger(messageType = COMMAND) DepositTransfer trigger) {
        FluxCapacitor.sendAndForgetCommand(new RevertTransfer(trigger.getSourceAccountId(), trigger.getAmount()));
    }
}
