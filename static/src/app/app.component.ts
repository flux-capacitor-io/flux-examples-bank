import {AfterViewInit, Component} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {CookieService} from 'ngx-cookie';
import tokens from "../../utils/jwt-generator/src/tokens";
import {sendQuery} from './common/utils';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements AfterViewInit {
  title = 'flux-bank';

  constructor(private httpClient: HttpClient, cookieService: CookieService) {
    cookieService.put("jwt", tokens["admin@flux-bank.com"]);
  }

  ngAfterViewInit(): void {
    this.httpClient.get("/api/health", {responseType: 'text'})
      .subscribe(() => console.log("server healthy"));

    sendQuery("io.fluxcapacitor.clientapp.common.bank.query.GetAccount", {accountId: "unknown"})
      .subscribe(r => console.log(r));
  }


}
