import {RequestGateway} from './request-gateway';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable, of, Subscriber} from 'rxjs';
import {tap} from 'rxjs/operators';
import {InjectorProvider} from './injector-provider';

@Injectable()
export class QueryGateway extends RequestGateway {
  private cache: Map<string, any> = new Map();

  constructor(http: HttpClient) {
    super(http, '/api/query');
  }

  send(type: string, payload: any, options?: QueryOptions): Observable<any> {
    options = options || {
      caching: true,
      showSpinner: false
    };
    let o: Observable<any>;
    if (options.caching) {
      const key = type + JSON.stringify(payload);
      const cachedValue = this.cache.get(key);
      if (cachedValue) {
        return of(cachedValue);
      }
      o = super.send(type, payload, options).pipe(tap(value => this.cache.set(key, value)));
    } else {
      o = super.send(type, payload, options);
    }
    return o;
  }

  removeFromCache(type: string, payload?: any) {
    let key = type;
    if (payload) {
      key += JSON.stringify(payload);
    }
    Array.from(this.cache.keys()).filter(k => k.startsWith(key)).forEach(k => this.cache.delete(k));
  }
}

export interface QueryOptions {
  caching?: boolean;
  showSpinner?: boolean;
  hideError?: boolean;
  responseType? : string;
}

export function sendQuery(type: string, payload: any, options ?: any): Observable<any> {
  if (!InjectorProvider.injector) {
    return new Observable((subscriber: Subscriber<any>) => {
      InjectorProvider.injector.get(QueryGateway).send(type, payload, options).subscribe(subscriber);
    });
  }
  return InjectorProvider.injector.get(QueryGateway).send(type, payload, options);
}
