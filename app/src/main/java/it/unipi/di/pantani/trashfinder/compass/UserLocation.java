package it.unipi.di.pantani.trashfinder.compass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

public class UserLocation {
    public interface UserLocationListener {
        void onNewLocation(Location newLoc);
        void onAccuracyChanged(float accuracy);
    }

    private UserLocationListener listener;

    private final LocationManager locationManager;
    private final String locationProvider;

    private boolean active;

    private float prevAccuracy;

    public UserLocation(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Criteria locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationCriteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        locationCriteria.setPowerRequirement(Criteria.POWER_LOW);

        locationProvider = locationManager.getBestProvider(locationCriteria, true);

        prevAccuracy = 0;
        active = false;
    }

    @SuppressLint("MissingPermission")
    public void start(int locationRefreshTime) {
        if(locationProvider != null)
            locationManager.requestLocationUpdates(locationProvider, locationRefreshTime,  0, this::onLocationChanged);
    }

    public void stop() {
        locationManager.removeUpdates(this::onLocationChanged);
        active = false;
    }

    public void setListener(UserLocationListener l) {
        listener = l;
        active = true;
    }

    @SuppressLint("MissingPermission")
    public Location getLastLocation() {
        if(locationProvider != null) return locationManager.getLastKnownLocation(locationProvider);
        return null;
    }

    private void onLocationChanged(Location location) {
        if(listener == null || !active) return;

        listener.onNewLocation(location);
        notifyChange(location.getAccuracy());
    }

    private void notifyChange(float newAccuracy) {
        if(listener == null || !active) return;

        if(prevAccuracy != newAccuracy) {
            prevAccuracy = newAccuracy;
            listener.onAccuracyChanged(newAccuracy);
        }
    }
}
