import {Component} from '@angular/core';
import {AppContext} from '../../app-context';

@Component({
  selector: 'app-alerting',
  templateUrl: './alerting.component.html',
  styleUrls: ['./alerting.component.scss']
})
export class AlertingComponent {
  appContext = AppContext;
  maxAlertCount = 2;
}
