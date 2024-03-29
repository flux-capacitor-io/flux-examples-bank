import {Component, Input} from '@angular/core';
import {sendCommand} from '../common/command-gateway';
import {AppContext} from '../app-context';

@Component({
  selector: 'app-transfer-money',
  templateUrl: './transfer-money.component.html',
  styleUrls: ['./transfer-money.component.scss']
})
export class TransferMoneyComponent {
  @Input() account;
  destinationAccountId: string;
  amount: number;

  transfer() {
    sendCommand("io.fluxcapacitor.clientapp.common.bank.command.TransferMoney",
      {"accountId" : this.account.accountId, "destinationAccountId": this.destinationAccountId, "amount" : this.amount},
      () => {
        this.amount = null;
        this.destinationAccountId = null;
        AppContext.refreshAccount(this.account);
      });
  }

}
