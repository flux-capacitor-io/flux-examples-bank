import {AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {AlertLevel} from './alert';

declare var $: any;

@Component({
  selector: 'app-status-alert',
  templateUrl: './status-alert.component.html',
  styleUrls: ['./status-alert.component.css']
})
export class StatusAlertComponent implements OnInit, AfterViewInit {
  @Input() type: AlertLevel;
  @Input() msShowTime: number;
  @Output() close = new EventEmitter();

  constructor(private elementRef: ElementRef) {
  }

  ngOnInit() {
    if (!this.type) {
      throw new Error('Attribute type is required for app-status-alert component');
    }
  }

  ngAfterViewInit(): void {
    const element = $(this.elementRef.nativeElement.querySelector(".alert"));
    element.on('closed.bs.alert', () => {
      this.close.emit();
    });
    if (this.msShowTime) {
      setTimeout(() => element.alert('close'), this.msShowTime);
    }
  }
}
