import {Alert, AlertLevel} from './common/status-alert/alert';
import {HttpErrorResponse} from '@angular/common/http';
import {sendQuery} from './common/query-gateway';

export class AppContext {

  static refreshAccount(account) {
    sendQuery("io.fluxcapacitor.clientapp.common.bank.query.GetAccount", {accountId: account.accountId}, {caching: false})
      .subscribe(r => {
        if (r) {
          Object.assign(account, r);
        }
      });
  }

  static alerts: Alert[] = [];

  static registerError(error: any, level:AlertLevel = 'danger'): Alert {
    this.alerts = this.alerts.filter(a => a.level === level);
    if (error instanceof HttpErrorResponse) {
      if (String(error.status).startsWith('4') && error.error.error) {
        error = error.error.error;
        const splitErrors = (<string>error).split('\n');
        if (splitErrors.length > 1) {
          splitErrors.forEach(e => this.registerError(e));
          return;
        }
      } else {
        error = 'An unexpected error occurred (' + error.status + '): please contact service desk';
      }
    } else if (error instanceof Error) {
      error = error.message;
    } else if (typeof error !== 'string') {
      error = JSON.stringify(error);
    }
    const alert = <Alert>{content: error, level: level};
    this.addAlert(alert);
    return alert;
  }

  static hasErrors() {
    return this.alerts.filter(a => a.level === 'danger').length > 0;
  }

  static registerSuccess(success: string) {
    const alert = <Alert>{content: success, level: 'success', msShowTime: 2000};
    this.addAlert(alert);
  }

  static addAlert(alert: Alert) {
    if (this.alerts.filter(value => value.content === alert.content).length === 0) {
      this.alerts.push(alert);
    }
  }

  static closeAlerts(...alerts: Alert[]) {
    alerts.forEach(alert => {
      if (AppContext.alerts.indexOf(alert) >= 0) {
        AppContext.alerts.splice(AppContext.alerts.indexOf(alert), 1);
      }
    });
  }

  static clearAlerts() {
    this.alerts = [];
  }
}
