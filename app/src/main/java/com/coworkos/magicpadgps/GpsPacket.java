package com.coworkos.magicpadgps;

import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

final class GpsPacket {
    final double latitude;
    final double longitude;
    final float accuracyM;
    final double altitudeM;
    final boolean hasAltitude;
    final float speedMps;
    final boolean hasSpeed;
    final float bearingDeg;
    final boolean hasBearing;
    final long locationTimeMs;
    final long elapsedRealtimeNanos;

    private GpsPacket(
            double latitude,
            double longitude,
            float accuracyM,
            double altitudeM,
            boolean hasAltitude,
            float speedMps,
            boolean hasSpeed,
            float bearingDeg,
            boolean hasBearing,
            long locationTimeMs,
            long elapsedRealtimeNanos
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyM = accuracyM;
        this.altitudeM = altitudeM;
        this.hasAltitude = hasAltitude;
        this.speedMps = speedMps;
        this.hasSpeed = hasSpeed;
        this.bearingDeg = bearingDeg;
        this.hasBearing = hasBearing;
        this.locationTimeMs = locationTimeMs;
        this.elapsedRealtimeNanos = elapsedRealtimeNanos;
    }

    static String fromLocation(Location location) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("type", "location");
        root.put("protocolVersion", 1);
        root.put("sentAtEpochMs", System.currentTimeMillis());

        JSONObject loc = new JSONObject();
        loc.put("latitude", location.getLatitude());
        loc.put("longitude", location.getLongitude());
        loc.put("horizontalAccuracyM", location.hasAccuracy() ? location.getAccuracy() : 25.0);
        loc.put("locationTimeEpochMs", location.getTime());
        loc.put("elapsedRealtimeNanos", location.getElapsedRealtimeNanos());

        if (location.hasAltitude()) {
            loc.put("altitudeM", location.getAltitude());
        }
        if (location.hasSpeed()) {
            loc.put("speedMps", location.getSpeed());
        }
        if (location.hasBearing()) {
            loc.put("bearingDeg", location.getBearing());
        }

        root.put("location", loc);
        return root.toString();
    }

    static GpsPacket parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject loc = root.getJSONObject("location");
        return new GpsPacket(
                loc.getDouble("latitude"),
                loc.getDouble("longitude"),
                (float) loc.optDouble("horizontalAccuracyM", 25.0),
                loc.optDouble("altitudeM", 0.0),
                loc.has("altitudeM"),
                (float) loc.optDouble("speedMps", 0.0),
                loc.has("speedMps"),
                (float) loc.optDouble("bearingDeg", 0.0),
                loc.has("bearingDeg"),
                loc.optLong("locationTimeEpochMs", System.currentTimeMillis()),
                loc.optLong("elapsedRealtimeNanos", android.os.SystemClock.elapsedRealtimeNanos())
        );
    }

    Location toAndroidLocation(String providerName) {
        Location location = new Location(providerName);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(Math.max(accuracyM, 1.0f));
        location.setTime(locationTimeMs > 0 ? locationTimeMs : System.currentTimeMillis());
        location.setElapsedRealtimeNanos(
                elapsedRealtimeNanos > 0
                        ? elapsedRealtimeNanos
                        : android.os.SystemClock.elapsedRealtimeNanos()
        );
        if (hasAltitude) {
            location.setAltitude(altitudeM);
        }
        if (hasSpeed) {
            location.setSpeed(speedMps);
        }
        if (hasBearing) {
            location.setBearing(bearingDeg);
        }
        return location;
    }
}
