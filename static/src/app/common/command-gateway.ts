import {RequestGateway} from './request-gateway';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';

@Injectable()
export class
CommandGateway extends RequestGateway {
  constructor(http: HttpClient) {
    super(http, "/api/command");
  }
}
