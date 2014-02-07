package dummy;

import android.location.LocationListener;
import android.location.Location;
import android.location.CountryListener;
import android.location.Country;
import android.location.GpsStatus.Listener;
import android.os.Bundle;

public class LocationListeners implements LocationListener, CountryListener, Listener {
    public LocationListeners() { }

    public void   onLocationChanged(Location location) { }
    public void   onProviderDisabled(String provider) { }
    public void   onProviderEnabled(String provider) { }
    public void   onStatusChanged(String provider, int status, Bundle extras) { }

    public void  onCountryDetected(Country country) { }

    public void  onGpsStatusChanged(int event) { }
}
