import {Component} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {CookieService} from 'ngx-cookie';
import tokens from "../../utils/jwt-generator/src/tokens";
import {environment} from '../environments/environment';
import {sendQuery} from './common/query-gateway';
import {sendCommand} from './common/command-gateway';


declare var $: any;

export const userId = "admin@flux-bank.com";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  account;

  constructor(private httpClient: HttpClient, cookieService: CookieService) {
    if (environment.connected) {
      cookieService.put("jwt", tokens[userId]);

      httpClient.get("/api/health", {responseType: 'text'})
        .subscribe(() => console.log("server healthy"));
    }
  }

  createAccount(createCommand, modal) {
    createCommand.userId = userId;
    sendCommand("io.fluxcapacitor.clientapp.common.bank.command.CreateAccount", createCommand,
      () => {
        this.account = createCommand;
        $(modal).modal('hide');
      });
  }

  login(accountId: string, modal) {
    sendQuery("io.fluxcapacitor.clientapp.common.bank.query.GetAccount", {accountId: accountId}, {caching: false})
      .subscribe(r => {
        if (r) {
          this.account = r;
          $(modal).modal('hide');
        }
      });
  }

  logout() {
    this.account = null;
  }
}
