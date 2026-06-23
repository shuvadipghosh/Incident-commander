import { Injectable } from '@angular/core';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private connected$ = new BehaviorSubject<boolean>(false);

  constructor() {}

  public connect(): void {
    if (this.stompClient && this.connected$.value) {
      return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;

    this.stompClient = new Client({
      brokerURL: wsUrl,
      heartbeatIncoming: 25000,
      heartbeatOutgoing: 25000,
      reconnectDelay: 5000,
      debug: (str) => {
        console.log('[STOMP Debug]', str);
      },
      onConnect: () => {
        this.connected$.next(true);
        console.log('[WebSocketService] Connected');
      },
      onDisconnect: () => {
        this.connected$.next(false);
        console.log('[WebSocketService] Disconnected');
      },
      onStompError: (frame) => {
        this.connected$.next(false);
        console.error('[WebSocketService] STOMP Error', frame);
      }
    });

    this.stompClient.activate();
  }

  public isConnected(): Observable<boolean> {
    return this.connected$.asObservable();
  }

  public getConnectedValue(): boolean {
    return this.connected$.value;
  }

  public subscribeToIncident(sessionId: string): Observable<any> {
    const subject = new Subject<any>();
    if (!this.stompClient) {
      console.warn('[WebSocketService] Cannot subscribe, STOMP client is not initialized');
      return subject.asObservable();
    }

    const subscription = this.stompClient.subscribe(
      `/topic/incident/${sessionId}`,
      (message: IMessage) => {
        try {
          const payload = JSON.parse(message.body);
          subject.next(payload);
        } catch (err) {
          console.error('[WebSocketService] Error parsing WebSocket message body', err);
        }
      }
    );

    return new Observable<any>(observer => {
      const sub = subject.subscribe(observer);
      return () => {
        sub.unsubscribe();
        subscription.unsubscribe();
      };
    });
  }

  public publish(destination: string, body: any): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination,
        body: JSON.stringify(body)
      });
    } else {
      console.error('[WebSocketService] Cannot publish, client not connected');
    }
  }

  public disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.connected$.next(false);
    }
  }
}
