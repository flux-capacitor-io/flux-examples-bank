package io.fluxcapacitor.clientapp.app.bank;

import io.fluxcapacitor.clientapp.common.bank.command.DepositTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.RevertTransfer;
import io.fluxcapacitor.clientapp.common.bank.command.TransferFailed;
import io.fluxcapacitor.clientapp.common.bank.command.TransferMoney;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.tracking.handling.HandleError;
import io.fluxcapacitor.javaclient.tracking.handling.HandleEvent;
import org.springframework.stereotype.Component;

@Component
public class TransferEventHandler {
    @HandleEvent
    void handle(TransferMoney command) {
        FluxCapacitor.sendAndForgetCommand(
                new DepositTransfer(command.getDestinationAccountId(), command.getAccountId(),
                                    command.getAmount()));
    }

    @HandleError
    void handle(TransferFailed error) {
        FluxCapacitor.sendAndForgetCommand(new RevertTransfer(error.getFailedCommand().getSourceAccountId(),
                                                              error.getFailedCommand().getAmount()));
    }
}
