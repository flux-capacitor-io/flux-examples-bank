import {BrowserModule} from '@angular/platform-browser';
import {Injector, NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {HttpClientModule} from '@angular/common/http';
import {CookieModule} from 'ngx-cookie';
import {CommandGateway} from './common/command-gateway';
import {QueryGateway} from './common/query-gateway';
import {InjectorProvider} from './common/injector-provider';
import {CreateAccountComponent} from './create-account/create-account.component';
import {FormsModule} from '@angular/forms';
import {DepositMoneyComponent} from './deposit-money/deposit-money.component';
import {LoginComponent} from './login/login.component';
import {TransferMoneyComponent} from './transfer-money/transfer-money.component';
import {DashboardComponent} from './dashboard/dashboard.component';
import {TransactionsComponent} from './transactions/transactions.component';
import {StatusAlertComponent} from './common/status-alert/status-alert.component';
import {AlertingComponent} from './common/alerting/alerting.component';

@NgModule({
  declarations: [
    AppComponent,
    CreateAccountComponent,
    DepositMoneyComponent,
    LoginComponent,
    TransferMoneyComponent,
    DashboardComponent,
    TransactionsComponent,
    StatusAlertComponent,
    AlertingComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    CookieModule.forRoot(),
    AppRoutingModule
  ],
  providers: [
    CommandGateway,
    QueryGateway,
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(injector: Injector) {
    InjectorProvider.injector = injector;
  }
}
