import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-action-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick($event)">
      <div class="modal-content">
        <div class="modal-header">
          <span class="modal-icon">{{ icon }}</span>
          <h2 class="modal-title">{{ title }}</h2>
        </div>
        <div class="modal-body">{{ message }}</div>
        <div class="modal-actions">
          <button class="btn btn-primary" (click)="close.emit()">Dismiss</button>
        </div>
      </div>
    </div>
  `
})
export class ActionModalComponent {
  @Input() icon: string = '📱';
  @Input() title: string = 'Confirmation';
  @Input() message: string = '';
  @Output() close = new EventEmitter<void>();

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close.emit();
    }
  }
}
