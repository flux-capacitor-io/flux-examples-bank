import {Component, Input} from '@angular/core';
import {refreshAccount, sendCommand} from '../common/utils';

@Component({
  selector: 'app-deposit-money',
  templateUrl: './deposit-money.component.html',
  styleUrls: ['./deposit-money.component.scss']
})
export class DepositMoneyComponent {
  @Input() account;
  amount: number;

  deposit() {
    sendCommand("io.fluxcapacitor.clientapp.common.bank.command.DepositMoney",
      {"accountId" : this.account.accountId, "amount" : this.amount},
      () => {
        this.amount = null;
        refreshAccount(this.account);
      });
  }
}
