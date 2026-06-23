import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface LocationState {
  status: 'not-set' | 'loading' | 'active' | 'error';
  latitude: number | null;
  longitude: number | null;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class LocationService {
  private state$ = new BehaviorSubject<LocationState>({
    status: 'not-set',
    latitude: null,
    longitude: null,
    message: 'Location not set'
  });

  constructor() {}

  public getLocationState(): Observable<LocationState> {
    return this.state$.asObservable();
  }

  public getRawState(): LocationState {
    return this.state$.value;
  }

  public fetchLocation(): void {
    if (!navigator.geolocation) {
      this.state$.next({
        status: 'error',
        latitude: null,
        longitude: null,
        message: 'Geolocation not supported'
      });
      return;
    }

    this.state$.next({
      status: 'loading',
      latitude: null,
      longitude: null,
      message: 'Locating…'
    });

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        this.state$.next({
          status: 'active',
          latitude: lat,
          longitude: lng,
          message: `${lat.toFixed(4)}, ${lng.toFixed(4)}`
        });
      },
      (err) => {
        this.state$.next({
          status: 'error',
          latitude: null,
          longitude: null,
          message: 'Could not get location'
        });
        console.warn('[LocationService] Geolocation error', err);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  }

  public resetLocation(): void {
    this.state$.next({
      status: 'not-set',
      latitude: null,
      longitude: null,
      message: 'Location not set'
    });
  }
}
