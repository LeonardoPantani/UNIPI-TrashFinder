/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.compass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

class Compass implements SensorEventListener {
    public interface CompassListener {
        void onNewAzimuth(float azimuth);
        void onSensorAccuracyChanged(float accuracy);
    }

    private CompassListener listener;

    private final SensorManager sensorManager;
    private final Display display;
    private final Sensor gsensor;
    private final Sensor msensor;

    private final float[] mGravity = new float[3];
    private final float[] mGeomagnetic = new float[3];
    private final float[] R = new float[9];
    private final float[] I = new float[9];

    public Compass(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void start() {
        sensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void setListener(CompassListener l) {
        listener = l;
    }

    /*
        parte di questo metodo è stata ottenuta dal seguente link:
        https://stackoverflow.com/questions/28798585/android-determine-memory-leak-in-compass-sensormanager
     */
    @SuppressLint("SwitchIntDef")
    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                        * event.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                        * event.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                        * event.values[2];
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                        * event.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                        * event.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                        * event.values[2];
            }

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = ((float) Math.toDegrees(orientation[0]) + 360) % 360;

                // correggo l'azimuth in base alla rotazione del dispositivo
                switch (display.getRotation()) {
                    case Surface.ROTATION_90: // a sinistra
                        azimuth += 90;
                        break;
                    case Surface.ROTATION_270: // a destra
                        azimuth -= 90;
                        break;
                }

                if (listener != null) {
                    listener.onNewAzimuth(azimuth);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if(listener != null) {
            listener.onSensorAccuracyChanged(accuracy);
        }
    }

    /**
     * Calcolo traiettoria per l'orientamento della freccia
     * @param startLat latitudine iniziale
     * @param startLng longitudine iniziale
     * @param endLat latitudine finale
     * @param endLng longitudine finale
     * @return angolo
     */
    public static double bearing(double startLat, double startLng, double endLat, double endLng){
        double latitude1 = Math.toRadians(startLat);
        double latitude2 = Math.toRadians(endLat);
        double longDiff = Math.toRadians(endLng - startLng);
        double y = Math.sin(longDiff)*Math.cos(latitude2);
        double x = Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);
        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }
}