import {InjectorProvider} from './injector-provider';
import {Observable, Subscriber} from 'rxjs';
import {QueryGateway} from './query-gateway';
import {CommandGateway} from './command-gateway';

export function sendQuery(type: string, payload: any, options ?: any): Observable<any> {
  if (!InjectorProvider.injector) {
    return new Observable((subscriber: Subscriber<any>) => {
      InjectorProvider.injector.get(QueryGateway).send(type, payload, options).subscribe(subscriber);
    });
  }
  return InjectorProvider.injector.get(QueryGateway).send(type, payload, options);
}

export function sendCommand(type: string, payload: any, successHandler?: (value: any) => void, errorHandler?: (error: any) => void) {
  return InjectorProvider.injector.get(CommandGateway).send(type, payload)
    .subscribe(successHandler ? successHandler : () => {},
      errorHandler ? errorHandler : (error) => console.error(error));
}

export function refreshAccount(account) {
  sendQuery("io.fluxcapacitor.clientapp.common.bank.query.GetAccount", {accountId: account.accountId}, {caching: false})
    .subscribe(r => {
      if (r) {
        Object.assign(account, r);
      }
    });
}
