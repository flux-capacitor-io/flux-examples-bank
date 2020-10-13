import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';

export abstract class RequestGateway {
  private handlers = [];

  protected constructor(private http: HttpClient, private httpEndpoint: string) {
  }

  send(type: string, message: any, options?): Observable<any> {
    console.debug("sending request", type);
    const handler = this.handlers.find(h => h[type]);
    if (handler) {
      try {
        const result = handler[type](getPayload(message));
        return result instanceof Observable ? result : of(result);
      } catch (e) {
        return new Observable(subscriber => subscriber.error(e));
      }
    } else {
      message = asMessage(message, type);
      return this.http.post<any>(this.httpEndpoint, message, options);
    }
  }

  registerLocalHandler(handler) {
    this.handlers.push(handler);
    return () => this.handlers = this.handlers.filter(h => h !== handler);
  }

  registerLocalHandlers(...handlers) {
    const subscriptions: any[] = handlers.map(h => this.registerLocalHandler(h));
    return () => subscriptions.forEach(s => s());
  }
}

function getPayload(message: any) {
  return message['@class'] === 'io.fluxcapacitor.javaclient.common.Message' ? message.payload : message;
}

function asMessage(payload: any, type: string) {
  //payload is already a message
  if (payload['@class'] === 'io.fluxcapacitor.javaclient.common.Message') {
    payload.metadata = payload.metadata || {};
    payload.payload["@class"] = type;
    return payload;
  }

  //payload should be converted to a message
  payload["@class"] = type;
  return {
    "@class": 'io.fluxcapacitor.javaclient.common.Message',
    payload: payload,
    metadata: {}
  }
}
