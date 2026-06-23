import { Component, OnInit, OnDestroy, inject, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { WebSocketService } from './services/websocket.service';
import { LocationService, LocationState } from './services/location.service';
import { ActionModalComponent } from './components/action-modal.component';

export interface TimelineEvent {
  type: string;
  icon: string;
  title: string;
  detail: string;
}

const INCIDENT_META: { [key: string]: { label: string, icon: string, tone: string, emergency: boolean } } = {
  OUT_OF_FUEL:        { label: 'Out of Fuel',        icon: '⛽', tone: 'info',    emergency: false },
  FLAT_TYRE:          { label: 'Flat Tyre',           icon: '🔧', tone: 'warning', emergency: false },
  DEAD_BATTERY:       { label: 'Dead Battery',        icon: '🔋', tone: 'warning', emergency: false },
  VEHICLE_SMOKE:      { label: 'Vehicle Smoke',       icon: '💨', tone: 'danger',  emergency: true  },
  MEDICAL_EMERGENCY:  { label: 'Medical Emergency',   icon: '🏥', tone: 'danger',  emergency: true  },
  ACCIDENT:           { label: 'Accident',            icon: '🚗', tone: 'danger',  emergency: true  },
  NEARBY_MECHANIC:    { label: 'Find Mechanic',       icon: '🔩', tone: 'info',    emergency: false },
  TOW_REQUEST:        { label: 'Request Tow',         icon: '🚛', tone: 'warning', emergency: false },
  UNKNOWN:            { label: 'General Help',        icon: '❓', tone: 'info',    emergency: false },
};

const ACTION_LABELS: { [key: string]: string } = {
  WALK_TO_FUEL_STATION:     'Walk to Fuel Station',
  FUEL_DELIVERY:            'Fuel Delivery',
  TOW_TRUCK:                'Tow Truck',
  TYRE_REPAIR_SERVICE:      'Tyre Repair Service',
  TYRE_REPAIR:              'Tyre Repair Service',
  TOW_TO_MECHANIC:          'Tow to Mechanic',
  JUMP_START_SERVICE:       'Jump Start Service',
  JUMP_START:               'Jump Start Service',
  BATTERY_REPLACEMENT:      'Battery Replacement',
  EXIT_VEHICLE_IMMEDIATELY: '⚠️ Exit Vehicle Now',
  CALL_911:                 '🚨 Call 911',
  CALL_POLICE:              '🚔 Call Police',
  NEAREST_HOSPITAL:         '🏥 Nearest Hospital',
  DRIVE_TO_MECHANIC:        'Drive to Mechanic',
  UBER_RIDE:                'Book Uber Ride',
  CONTACT_SUPPORT:          'Contact Support',
  STAY_CALM:                'Stay Calm',
};

const ACTION_BUTTONS: { [key: string]: { btnText: string, icon: string, title: string, message: string } } = {
  UBER_RIDE: {
    btnText: '🚖 Book Uber Ride',
    icon: '🚖',
    title: 'Uber Booking Confirmed',
    message: 'An Uber ride has been scheduled to transport you. Driver "David" (Toyota Camry, Black, License: 7XYZ89) is en route to your exact location. ETA: 7 minutes.'
  },
  CALL_911: {
    btnText: '🚨 Call 911 Immediately',
    icon: '🚑',
    title: 'Calling Emergency Services',
    message: 'Connecting to 911 emergency services dispatch. Please stay calm, keep the line open, and prepare to state your physical coordinates.'
  },
  CALL_POLICE: {
    btnText: '🚔 Call Local Police',
    icon: '🚔',
    title: 'Calling Police Dispatch',
    message: 'Connecting you to the nearest local police dispatch for accident reporting.'
  },
  CONTACT_SUPPORT: {
    btnText: '📞 Speak to Allstate Agent',
    icon: '📞',
    title: 'Calling Allstate Agent',
    message: 'Connecting to an Allstate Roadside Assistance Support Specialist. ETA to connect: under 1 minute.'
  }
};

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, ActionModalComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  private wsService = inject(WebSocketService);
  private locationService = inject(LocationService);
  private http = inject(HttpClient);
  private zone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);

  // State
  public currentScreen = 'phoneEntry'; // 'phoneEntry', 'incidentPicker', 'description', 'progress', 'result'
  public rawPhone = '';
  public formattedPhone = '';
  public phoneError = '';
  public greetingPhoneText = '';

  public incidentType: string | null = null;
  public description = '';
  public charCountText = '';

  public locationState: LocationState = {
    status: 'not-set',
    latitude: null,
    longitude: null,
    message: 'Location not set'
  };

  public wsConnected = false;
  public progressSubtext = 'Analyzing your situation…';
  public timelineEvents: TimelineEvent[] = [];
  public resultPayload: any = null;

  // Modal
  public modalState = {
    show: false,
    icon: '📱',
    title: 'Confirmation',
    message: ''
  };

  private wsSubscription: Subscription | null = null;
  private wsConnectedSub: Subscription | null = null;
  private locationSub: Subscription | null = null;
  private sessionId = '';

  ngOnInit(): void {
    // Subscribe to websocket connection state
    this.wsConnectedSub = this.wsService.isConnected().subscribe(connected => {
      this.zone.run(() => {
        this.wsConnected = connected;
        this.cdr.detectChanges();
      });
    });

    // Subscribe to location updates
    this.locationSub = this.locationService.getLocationState().subscribe(state => {
      this.zone.run(() => {
        this.locationState = state;
        this.cdr.detectChanges();
      });
    });
  }

  ngOnDestroy(): void {
    this.wsConnectedSub?.unsubscribe();
    this.locationSub?.unsubscribe();
    this.wsSubscription?.unsubscribe();
    this.wsService.disconnect();
  }

  // ── Phone Entry ───────────────────────────
  public onPhoneInput(event: Event): void {
    this.phoneError = '';
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 10);
    this.rawPhone = digits;

    let fmt = '';
    if (digits.length <= 3) {
      fmt = digits;
    } else if (digits.length <= 6) {
      fmt = `${digits.slice(0, 3)}-${digits.slice(3)}`;
    } else {
      fmt = `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`;
    }
    this.formattedPhone = fmt;
    input.value = fmt;
  }

  public onPhoneSubmit(): void {
    if (this.rawPhone.length !== 10) {
      this.phoneError = 'Please enter a valid 10-digit phone number.';
      return;
    }

    this.greetingPhoneText = `📋 Phone: ${this.formattedPhone}`;
    this.currentScreen = 'incidentPicker';
    this.wsService.connect();
  }

  // ── Incident Picker ───────────────────────
  public selectIncident(type: string): void {
    this.incidentType = type;
    this.description = '';
    this.charCountText = '';
    this.currentScreen = 'description';
  }

  public getIncidentCardClass(type: string): string {
    const meta = INCIDENT_META[type] || INCIDENT_META['UNKNOWN'];
    let classes = 'incident-card';
    if (meta.emergency) {
      classes += ' emergency';
    }
    if (this.incidentType === type) {
      classes += ' selected';
    }
    return classes;
  }

  public getSelectedBadgeHtml(): string {
    if (!this.incidentType) return '';
    const meta = INCIDENT_META[this.incidentType] || INCIDENT_META['UNKNOWN'];
    return `<span>${meta.icon}</span><span>${meta.label}</span>`;
  }

  public backToPicker(): void {
    this.incidentType = null;
    this.currentScreen = 'incidentPicker';
  }

  // ── Description & Geolocation ────────────
  public onDescriptionInput(): void {
    const len = this.description.trim().length;
    this.charCountText = len ? `${len} / 500` : '';
  }

  public useMyLocation(): void {
    this.locationService.fetchLocation();
  }

  public onSubmitDescription(): void {
    if (!this.description.trim()) {
      return;
    }

    // Default coordinates if not set
    let lat = this.locationState.latitude;
    let lng = this.locationState.longitude;
    if (!lat || !lng) {
      lat = 40.2384;
      lng = -74.0126;
    }

    this.sessionId = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2);

    if (this.wsConnected) {
      this.submitViaWebSocket(lat, lng);
    } else {
      this.submitViaRest(lat, lng);
    }
  }

  private submitViaWebSocket(lat: number, lng: number): void {
    this.currentScreen = 'progress';
    this.timelineEvents = [];
    this.progressSubtext = 'Analyzing your situation…';
    this.addTimelineEvent('classifying', '🤖', 'AI Classifying', 'Analyzing your description…');

    this.wsSubscription?.unsubscribe();
    this.wsSubscription = this.wsService.subscribeToIncident(this.sessionId).subscribe({
      next: (event) => {
        this.zone.run(() => {
          try {
            this.handleProgressEvent(event);
            if (event.status === 'COMPLETE' || event.status === 'ERROR') {
              this.wsSubscription?.unsubscribe();
            }
          } catch (e: any) {
            console.error('[wsSubscription next error]', e);
            this.progressSubtext = 'Next Error: ' + e.message;
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        this.zone.run(() => {
          console.error('[WebSocket] Subscription error', err);
          this.addTimelineEvent('error', '❌', 'Error occurred', 'WebSocket communication failed');
        });
      }
    });

    this.wsService.publish('/app/incident.analyze', {
      description: this.description,
      incidentTypeHint: this.incidentType,
      latitude: lat,
      longitude: lng,
      phoneNumber: Number(this.rawPhone),
      sessionId: this.sessionId,
    });
  }

  private async submitViaRest(lat: number, lng: number): Promise<void> {
    this.currentScreen = 'progress';
    this.timelineEvents = [];
    this.progressSubtext = 'Analyzing your situation…';
    this.addTimelineEvent('classifying', '🤖', 'AI Classifying', 'Analyzing your description…');
    this.cdr.detectChanges();

    try {
      const payload: any = await this.http.post('/incident/analyze', {
        description: this.description,
        incidentTypeHint: this.incidentType,
        latitude: lat,
        longitude: lng,
        phoneNumber: Number(this.rawPhone),
        sessionId: this.sessionId,
      }).toPromise();

      // Simulate events
      await this.delay(600);
      const determinedType = payload.incidentType || this.incidentType || 'UNKNOWN';
      this.addTimelineEvent('classified', 'classified', `Classified as ${this.formatIncidentType(determinedType)}`, 'Incident type determined');
      await this.delay(500);
      this.addTimelineEvent('complete', 'complete', 'Analysis complete', 'Your assistance plan is ready');
      await this.delay(300);

      this.renderResult(payload);
    } catch (err: any) {
      const msg = err.error?.detail || err.error?.error || err.message || 'Request failed';
      this.addTimelineEvent('error', 'error', 'Error', msg);
      console.error('[App] REST error', err);
      this.cdr.detectChanges();
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private handleProgressEvent(event: any): void {
    try {
      const { status } = event;

      if (status === 'CLASSIFYING') {
        if (!this.timelineEvents.some(e => e.type === 'classifying')) {
          this.addTimelineEvent('classifying', '🤖', 'AI Classifying', 'Analyzing your description…');
        }
      } else if (status === 'CLASSIFIED') {
        this.addTimelineEvent('classified', '✓', `Classified as ${this.formatIncidentType(event.type)}`, 'Incident type determined by AI');
        this.progressSubtext = `Handling ${this.formatIncidentType(event.type)} incident…`;
      } else if (status === 'TOOL_CALL') {
        this.addTimelineEvent('tool-call', '🔧', `Calling: ${this.formatToolName(event.tool)}`, 'Fetching real-time data…');
      } else if (status === 'COMPLETE') {
        this.addTimelineEvent('complete', '✅', 'Analysis complete', 'Your assistance plan is ready');
        setTimeout(() => this.renderResult(event.result), 400);
      } else if (status === 'ERROR') {
        this.addTimelineEvent('error', '❌', 'Error occurred', event.message || 'Something went wrong');
      }
      this.cdr.detectChanges();
    } catch (e: any) {
      console.error('[handleProgressEvent Error]', e);
      this.progressSubtext = 'Progress Error: ' + e.message;
      this.cdr.detectChanges();
    }
  }

  private addTimelineEvent(type: string, icon: string, title: string, detail: string): void {
    this.timelineEvents = [...this.timelineEvents, { type, icon, title, detail }];
    this.cdr.detectChanges();
  }

  private renderResult(payload: any): void {
    this.zone.run(() => {
      try {
        this.resultPayload = payload;
        this.currentScreen = 'result';
        this.cdr.detectChanges();
      } catch (e: any) {
        console.error('[renderResult Error]', e);
        this.progressSubtext = 'Render Error: ' + e.message;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Result Helpers ────────────────────────
  public getResultType(): string {
    return this.resultPayload?.incidentType || this.incidentType || 'UNKNOWN';
  }

  public getResultMeta() {
    return INCIDENT_META[this.getResultType()] || INCIDENT_META['UNKNOWN'];
  }

  public isEmergencyResult(): boolean {
    return this.getResultMeta().emergency;
  }

  public getResultTitle(): string {
    const type = this.getResultType();
    const titles: { [key: string]: string } = {
      OUT_OF_FUEL:       'Fuel Assistance Ready',
      FLAT_TYRE:         'Tyre Help on the Way',
      DEAD_BATTERY:      'Battery Service Arranged',
      VEHICLE_SMOKE:     '⚠️ Please Exit Your Vehicle',
      MEDICAL_EMERGENCY: '🚑 Emergency Response Active',
      ACCIDENT:          'Accident Response Initiated',
      NEARBY_MECHANIC:   'Mechanic Located',
      TOW_REQUEST:       'Tow Truck Dispatched',
      UNKNOWN:           'We Are Here to Help',
    };
    return titles[type] || 'Help Is on the Way';
  }

  public formatIncidentType(type: string): string {
    return (INCIDENT_META[type] || INCIDENT_META['UNKNOWN']).label;
  }

  public formatToolName(tool: string): string {
    if (!tool) return 'External Service';
    return tool.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase()).trim();
  }

  public formatDistanceKm(km: any): string {
    const v = Number(km);
    if (!Number.isFinite(v) || v <= 0) return '—';
    return v < 1 ? `${Math.round(v * 1000)} m` : `${v.toFixed(1)} km`;
  }

  public formatDistanceLabel(km: any): string {
    const v = Number(km);
    if (!Number.isFinite(v) || v <= 0) return '';
    return v < 1 ? 'away' : 'away';
  }

  public getActionLabel(action: string): string {
    return ACTION_LABELS[action] || action.replace(/_/g, ' ');
  }

  public hasActionConfig(action: string): boolean {
    return !!ACTION_BUTTONS[action];
  }

  public getActionConfig(action: string) {
    return ACTION_BUTTONS[action];
  }

  public triggerAction(action: string): void {
    const config = ACTION_BUTTONS[action];
    if (!config) return;

    this.modalState = {
      show: true,
      icon: config.icon || 'ℹ️',
      title: config.title,
      message: config.message
    };
  }

  public closeModal(): void {
    this.modalState.show = false;
  }

  public startOver(): void {
    this.incidentType = null;
    this.description = '';
    this.charCountText = '';
    this.resultPayload = null;
    this.timelineEvents = [];
    this.locationService.resetLocation();
    this.currentScreen = 'incidentPicker';
  }
}
