package com.example.flux.bank.core;

import io.fluxcapacitor.clientapp.common.bank.command.DepositTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.RollBackTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.TransferMoney;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.Consumer;
import io.fluxcapacitor.javaclient.tracking.ForeverRetryingErrorHandler;
import io.fluxcapacitor.javaclient.tracking.handling.HandleError;
import io.fluxcapacitor.javaclient.tracking.handling.HandleEvent;
import io.fluxcapacitor.javaclient.tracking.handling.Trigger;
import org.springframework.stereotype.Component;

import static io.fluxcapacitor.common.MessageType.COMMAND;

@Component
@Consumer(name = "transfers-consumer", errorHandler = ForeverRetryingErrorHandler.class)
public class TransferHandler {
    @HandleEvent
    void handle(TransferMoney event) {
        FluxCapacitor.sendAndForgetCommand(
                new DepositTransfer(event.getDestinationAccountId(), event.getAccountId(),
                                    event.getAmount()));
    }

    @HandleError
    void handle(@Trigger(messageType = COMMAND) DepositTransfer trigger, Throwable exception) {
        FluxCapacitor.sendAndForgetCommand(new RollBackTransfer(trigger.getSourceAccountId(), trigger.getAmount()));
    }
}
