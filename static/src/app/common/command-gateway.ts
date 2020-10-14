import {RequestGateway} from './request-gateway';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {InjectorProvider} from './injector-provider';

@Injectable()
export class
CommandGateway extends RequestGateway {
  constructor(http: HttpClient) {
    super(http, "/api/command");
  }
}

export function sendCommand(type: string, payload: any, successHandler?: (value: any) => void, errorHandler?: (error: any) => void) {
  return InjectorProvider.injector.get(CommandGateway).send(type, payload)
    .subscribe(successHandler ? successHandler : () => {},
      errorHandler ? errorHandler : (error) => console.error(error));
}
