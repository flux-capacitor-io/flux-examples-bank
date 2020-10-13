import {BrowserModule} from '@angular/platform-browser';
import {Injector, NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {HttpClientModule} from '@angular/common/http';
import {CookieModule} from 'ngx-cookie';
import {CommandGateway} from './common/command-gateway';
import {QueryGateway} from './common/query-gateway';
import {InjectorProvider} from './common/injector-provider';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
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
