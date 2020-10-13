package io.fluxcapacitor.clientapp.app.bank;

import io.fluxcapacitor.clientapp.common.bank.command.DepositTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.RevertTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.TransferMoney;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleEvent;
import org.springframework.stereotype.Component;

@Component
public class TransferEventHandler {
    @HandleEvent
    void handle(TransferMoney command) {
        try {
            FluxCapacitor.sendCommandAndWait(
                    new DepositTransfer(command.getDestinationAccountId(), command.getAccountId(),
                                        command.getAmount()));
        } catch (Exception e) {
            FluxCapacitor.sendAndForgetCommand(new RevertTransfer(command.getAccountId(), command.getAmount()));
            throw e;
        }
    }
}
