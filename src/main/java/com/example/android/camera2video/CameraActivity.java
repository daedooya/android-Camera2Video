/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2video;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;

public class CameraActivity extends Activity implements LocationListener, SensorEventListener {
    private static final String TAG = "MainActivity";
    private static final int TAG_PERMISSION_FINE_LOCATION = 1;

    private static Boolean mLogging = false;
    private static FileWriter mFileWriter;
    private static long mLogTime;

    private LocationManager mLocationManager;

    private SensorManager mSensorManager;
    private Sensor mAccSensor;
    private Sensor mGraSensor;
    private Sensor mLinSensor;
    private Sensor mGyrSensor;
    private Sensor mUgySensor;
    private Sensor mMagSensor;

    float[] mAccVal;
    float[] mMagVal;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2VideoFragment.newInstance())
                    .commit();
        }

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    TAG_PERMISSION_FINE_LOCATION);
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this);
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_GAME);
//        mGraSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
//        mSensorManager.registerListener(this, mGraSensor, SensorManager.SENSOR_DELAY_GAME);
//        mLinSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        mSensorManager.registerListener(this, mLinSensor, SensorManager.SENSOR_DELAY_GAME);
//        mGyrSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
//        mSensorManager.registerListener(this, mGyrSensor, SensorManager.SENSOR_DELAY_GAME);
        mUgySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        mSensorManager.registerListener(this, mUgySensor, SensorManager.SENSOR_DELAY_GAME);
//        mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        mSensorManager.registerListener(this, mMagSensor, SensorManager.SENSOR_DELAY_GAME);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.acquire();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == TAG_PERMISSION_FINE_LOCATION) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            }
        }
    }

    public static void startLogging(String pathBase) {
        Log.e(TAG, "Logging " + pathBase);
        mLogTime = SystemClock.elapsedRealtime();
        try {
            mFileWriter = new FileWriter(pathBase + ".csv");
            mFileWriter.write("//VERSION_NAME " + BuildConfig.VERSION_NAME + "\n");
            mFileWriter.write("//MODEL " + Build.MODEL + "\n");
            mFileWriter.write("//MANUFACTURER " + Build.MANUFACTURER + "\n");
            mFileWriter.write("//HARDWARE " + Build.HARDWARE + "\n");
            mFileWriter.write("//SERIAL " + Build.SERIAL + "\n");
            mFileWriter.write("//ANDROID " + Build.VERSION.RELEASE + "\n");
            mFileWriter.write("//SDK_INT " + Build.VERSION.SDK_INT + "\n");
            mFileWriter.write("//INCREMENTAL " + Build.VERSION.INCREMENTAL + "\n");
            mFileWriter.write("//CODENAME " + Build.VERSION.CODENAME + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mLogging = true;
    }

    public static void stopLogging() {
        mLogging = false;
        try {
            mFileWriter.flush();
            mFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logEvent(String text) {
        ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
//        tg.startTone(ToneGenerator.TONE_CDMA_PIP,150);
        tg.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, 300);

        logText(text);
    }

    private static void logText(String text) {
        if (!mLogging) return;

        long tim = SystemClock.elapsedRealtime() - mLogTime;
        if (tim < 1000) return;

        try {
            mFileWriter.write(tim + "," + text + "\n");
        } catch (IOException e) {
            Log.e(TAG, "Logging failed:" + text);
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        logText("GPS," + location.getLatitude() + "," + location.getLongitude());
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps turned off ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps turned on ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            logText("ACCELEROMETER," + event.values[0] + "," + event.values[1] + "," + event.values[2]);
            mAccVal = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            logText("GRAVITY," + event.values[0] + "," + event.values[1] + "," + event.values[2]);
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            logText("LINEAR_ACCELERATION," + event.values[0] + "," + event.values[1] + "," + event.values[2]);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            logText("GYROSCOPE," + event.values[0] + "," + event.values[1] + "," + event.values[2]);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
            logText("GYROSCOPE_UNCALIBRATED," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.values[3] + "," + event.values[4] + "," + event.values[5]);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            logText("MAGNETIC_FIELD," + event.values[0] + "," + event.values[1] + "," + event.values[2]);
            mMagVal = event.values;
            if (mAccVal != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mAccVal, mMagVal);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    logText("ORIENTATION," + orientation[0] + "," + orientation[1] + "," + orientation[2]); //azimuth, pitch and roll
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * when the activity is resumed, we acquire a wake-lock so that the
         * screen stays on, since the user will likely not be fiddling with the
         * screen or buttons.
         */
        mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGraSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mLinSensor, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, GyrSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mUgySensor, SensorManager.SENSOR_DELAY_GAME);
        mWakeLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*
         * When the activity is paused, we make sure to stop the simulation,
         * release our sensor resources and wake locks
         */
        mSensorManager.unregisterListener(this);
        mWakeLock.release();
    }
}
