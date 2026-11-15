package io.github.ahassan07.carvideo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.car.app.CarContext;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.Speed;

import java.util.concurrent.Executor;

/**
 * Reports whether the car is moving so the picture can be covered while driving.
 *
 * <p>The car's own speed reading is used when the head unit provides it. If it does not, the
 * phone's GPS speed is used when location access was granted. If neither source is available the
 * car is treated as parked, because there is nothing to measure.
 */
final class MotionMonitor {
    interface Listener {
        void onMovingChanged(boolean moving);
    }

    private static final String TAG = "CarVideo";
    private static final float MOVING_METERS_PER_SECOND = 1.4f; // about 5 km/h
    private static final long GPS_STALE_MS = 6_000;
    private static final long UNLOCK_DELAY_MS = 4_000;

    private final CarContext carContext;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor mainExecutor = handler::post;

    private CarInfo carInfo;
    private LocationManager locationManager;

    private boolean hasCarSpeed;
    private float carSpeed;
    private float gpsSpeed;
    private long gpsTime;

    private boolean moving;
    private boolean unlockPending;
    private boolean started;

    private final OnCarDataAvailableListener<Speed> carSpeedListener = this::onCarSpeed;
    // A full class rather than a lambda: before Android 11 the extra callbacks are abstract.
    private final LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            onGpsLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };
    private final Runnable unlock = () -> {
        unlockPending = false;
        setMoving(false);
    };

    MotionMonitor(CarContext carContext, Listener listener) {
        this.carContext = carContext;
        this.listener = listener;
    }

    void start() {
        if (started) {
            return;
        }
        started = true;
        try {
            carInfo = carContext.getCarService(CarHardwareManager.class).getCarInfo();
            carInfo.addSpeedListener(mainExecutor, carSpeedListener);
        } catch (RuntimeException e) {
            Log.w(TAG, "Car speed is not available", e);
            carInfo = null;
        }
        try {
            if (carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager = carContext.getSystemService(LocationManager.class);
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000L, 0f, gpsListener, Looper.getMainLooper());
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Phone GPS speed is not available", e);
            locationManager = null;
        }
    }

    void stop() {
        if (!started) {
            return;
        }
        started = false;
        unlockPending = false;
        handler.removeCallbacksAndMessages(null);
        try {
            if (carInfo != null) {
                carInfo.removeSpeedListener(carSpeedListener);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not remove the car speed listener", e);
        }
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(gpsListener);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not remove the GPS listener", e);
        }
        carInfo = null;
        locationManager = null;
    }

    private void onCarSpeed(Speed speed) {
        Float value = read(speed.getRawSpeedMetersPerSecond());
        if (value == null) {
            value = read(speed.getDisplaySpeedMetersPerSecond());
        }
        hasCarSpeed = value != null;
        if (value != null) {
            carSpeed = Math.abs(value);
        }
        evaluate();
    }

    private static Float read(CarValue<Float> value) {
        if (value == null || value.getStatus() != CarValue.STATUS_SUCCESS) {
            return null;
        }
        return value.getValue();
    }

    private void onGpsLocation(Location location) {
        if (location.hasSpeed()) {
            gpsSpeed = location.getSpeed();
            gpsTime = SystemClock.elapsedRealtime();
            evaluate();
        }
    }

    private void evaluate() {
        if (!started) {
            return;
        }
        boolean nowMoving;
        if (hasCarSpeed) {
            nowMoving = carSpeed > MOVING_METERS_PER_SECOND;
        } else if (gpsTime != 0 && SystemClock.elapsedRealtime() - gpsTime < GPS_STALE_MS) {
            nowMoving = gpsSpeed > MOVING_METERS_PER_SECOND;
        } else {
            return; // no fresh reading; keep the current state
        }

        if (nowMoving) {
            handler.removeCallbacks(unlock);
            unlockPending = false;
            setMoving(true);
        } else if (moving && !unlockPending) {
            // Wait a few seconds at standstill before showing the picture again.
            unlockPending = true;
            handler.postDelayed(unlock, UNLOCK_DELAY_MS);
        }
    }

    private void setMoving(boolean value) {
        if (moving != value) {
            moving = value;
            listener.onMovingChanged(value);
        }
    }
}
