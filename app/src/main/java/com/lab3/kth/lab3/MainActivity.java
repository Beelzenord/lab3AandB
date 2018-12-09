package com.lab3.kth.lab3;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import java.util.ArrayList;

/**
 *
 */
public class MainActivity extends AppCompatActivity {
    private final static int RED_COLOR = 1;
    private final static int BLACK_COLOR = 2;
    private int currentColor;
    private float prevAngle = 0;
    private float prevTiltX = 0;
    private float prevTiltY = 0;
    private float prevTiltZ = 0;
    private float prevMagnX = 0;
    private float prevMagnY = 0;
    private float prevMagnZ = 0;
    private float acceTiltAlpha = (float) 0.7;
    private float accelTiltPrevTimestamp = 0;
    private float highPassFilter = (float) 0.12;
    private float accelPrevTimestamp = 0;
    private ArrayList<Float> xValues;
    private ArrayList<Float> yValues;
    private ArrayList<Float> zValues;

    private TextView degreesView;
    SensorManager manager;

    private long shakeStartTimer;
    private boolean isShaking;

    private final float THRESHOLD = (float)1.3;
    private final int INDEXS_TO_CLEAR = 25;

    private final long DURANTION = 1000L;

    private boolean xDeviation;
    private boolean yDeviation;
    private boolean zDeviation;
    private int counter;

    private float[] filteredGravity;
    private float[] linearAcceleration;

    private float[] mGeomagnetic;


    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isShaking = false;
        currentColor = BLACK_COLOR;
        xDeviation = false;
        xValues = new ArrayList<Float>();
        yValues = new ArrayList<Float>();
        zValues = new ArrayList<Float>();
        degreesView = findViewById(R.id.degreesView);
        filteredGravity = new float[]{0,0,0};
        linearAcceleration = new float[]{0,0,0};
        mGeomagnetic = new float[]{0,0,0};
        counter = -1;


        manager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer =
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            manager.registerListener(
                    sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        Sensor magnetometer =
                manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magnetometer != null) {
            manager.registerListener(
                    sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }


        manager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);
        Sensor gyro =
                manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (gyro != null) {
            manager.registerListener(
                    sensorEventListener, gyro, SensorManager.SENSOR_DELAY_GAME);
        }


    }

    /**
     * Sensor events for various sensors
     */
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    handleAccelerationEvent(event);

                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mGeomagnetic[0] = event.values[0];
                    mGeomagnetic[1] = event.values[1];
                    mGeomagnetic[2] = event.values[2];

                    break;
           }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    /**
     * precurser to processing accelerometer data, and displaying the tilt and observing
     * shake events
     * @param event
     */
    private void handleAccelerationEvent(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        accelPrevTimestamp = event.timestamp;
        handleTiltChange(x, y, z);
        handleShakeChange(x, y, z, event.timestamp);
    }

    /**
     * Handle data to observe if the user is shaking it. We aggregate the data in a list.
     * When the list is up to 20, we check to see if there is a standard deviation
     * @param x
     * @param y
     * @param z
     * @param timestamp
     */
    private void handleShakeChange(float x, float y, float z, long timestamp) {

        // filter the accelerometer data
        filteredGravity[0] = (filteredGravity[0] * (highPassFilter)) + (x * (1-highPassFilter));
        filteredGravity[1] = (filteredGravity[1] * (highPassFilter)) + (y * (1-highPassFilter));
        filteredGravity[2] = (filteredGravity[2] * (highPassFilter)) + (z * (1-highPassFilter));

        float linearAccelerationX = x - filteredGravity[0];
        float linearAccelerationY = y - filteredGravity[1];
        float linearAccelerationZ = z - filteredGravity[2];
        xValues.add(linearAccelerationX);
        yValues.add(linearAccelerationY);
        zValues.add(linearAccelerationZ);

        if(xValues.size()==20 || yValues.size() == 20 || zValues.size() == 20){
            xDeviation = findStandardDeviation(xValues);
            yDeviation = findStandardDeviation(yValues);
            zDeviation = findStandardDeviation(zValues);
        }

        if(xDeviation || yDeviation || zDeviation){
//            degreesView.setTextColor(getResources().getColor(R.color.red,null));
            Log.i("Flags", "is deviation");
            if (!isShaking) {
                isShaking = true;
                shakeStartTimer = timestamp;

            }
            counter= 2;
            if (isShaking) {

                long timeDiff = (timestamp - shakeStartTimer) / 1000000;
                if (timeDiff > 969) {
                    switchColor();
                    isShaking = false;
                    removeIndexs(xValues, INDEXS_TO_CLEAR);
                    removeIndexs(yValues, INDEXS_TO_CLEAR);
                    removeIndexs(zValues, INDEXS_TO_CLEAR);

                }
            }
        }
        else{
            if(counter > 0 ){
                counter--;
            }
            if(counter== 0){
                removeIndexs(xValues, INDEXS_TO_CLEAR);
                removeIndexs(yValues, INDEXS_TO_CLEAR);
                removeIndexs(zValues, INDEXS_TO_CLEAR);
                isShaking=false;
                counter--;
            }
        }
    }

    /**
     * We remove some values of the list so that we can be current with the values
     * provided by the accelerometer
     * @param values
     * @param indexs
     */

    private void removeIndexs(ArrayList<Float> values, int indexs) {

        for(int i = 0 ; i < indexs ; i++){
            if(i >= values.size()){
               break;
            }
            values.remove(i);
        }
    }

    /**
     * Switch color of the text when we discover a significant standard deviation.
     */
    private void switchColor() {
        if (currentColor == BLACK_COLOR) {
            degreesView.setTextColor(getResources().getColor(R.color.red, null));
            currentColor = RED_COLOR;
        }
        else {
            degreesView.setTextColor(getResources().getColor(R.color.black, null));
            currentColor = BLACK_COLOR;
        }

    }

    /**
     * We take in raw data and filter out the noise
     * We calculate the angle and update the text view
     * @param x
     * @param y
     * @param z
     */
    private void handleTiltChange(float x, float y, float z) {
        x = (prevTiltX * (acceTiltAlpha)) + (x * (1 - acceTiltAlpha));
        y = (prevTiltY * (acceTiltAlpha)) + (y * (1 - acceTiltAlpha));
        z = (prevTiltZ * (acceTiltAlpha)) + (z * (1 - acceTiltAlpha));
        float angle = zAngleDegrees(x, y, z);

        angle = (prevAngle * (acceTiltAlpha)) + (angle * (1 - acceTiltAlpha));
        prevAngle = angle;
        prevTiltX = x;
        prevTiltY = y;
        prevTiltZ = z;
                // Bottom left
        if (y <= 0 && x <= 0 && angle >= 0 ||
                y <= 0 && x >= 0 && angle >= 0 ) {
            float tmp = 90 - angle;
            angle = 90 + tmp;
        }
        // Bottom Right
        else if (y <= 0 && x <= 0&& angle <= 0  ||
                y <= 0 && x >= 0&& angle <= 0) {
            float tmp = angle * -1;
            angle = 180 + tmp;
        }
        // Top Right
        else if (y >= 0 && x >= 0 && angle <= 0 ||
                y >= 0 && x <= 0 && angle <= 0) {
            float tmp = 90 + angle;
            angle = 270 + tmp;
        }
        int roundedAngle = Math.round(angle);
        if (roundedAngle == 360)
            roundedAngle = 0;
//        WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
//        int rotation = windowManager.getDefaultDisplay().getRotation();
        float timeD = (accelPrevTimestamp - accelTiltPrevTimestamp) / 1000000;
        if (timeD > 500) {
            Log.i("Values", "x; " + x + " y; " + y + " z; " + z + " angle; " + prevAngle);
//            Log.i("Values", "D: " + d);
//            Log.i("Values", "ROtation: " + rotation);
            accelTiltPrevTimestamp = accelPrevTimestamp;
        }
        degreesView.setText(roundedAngle + "°");
    }


    /**
     * apply a mathematical formula to obtain the standard deviation and if there
     * is a deviation, then we toggle a flag to change the text color.
     * @param xValues
     * @return
     */
    private boolean findStandardDeviation(ArrayList<Float> xValues) {
          float variance = 0;
          float sum = 0;


          for(Float value : xValues){
              float tmp = value;
              sum += value;
              tmp = tmp*tmp;

              variance +=tmp;
          }
          variance/= xValues.size();
         float standDev = (float) Math.sqrt(variance);
         float average = sum / xValues.size();
         average = Math.abs(average);
         standDev = Math.abs(standDev);

        Log.i("Mean","Standard Deviation : " + standDev);
         xValues.remove(0);
        Log.i("Mean","Mean : " + average);
        xValues.remove(0);

        if((average + THRESHOLD ) < standDev){
            System.out.println("Performing shift " + average + " " + standDev);
            return true;
        }
        else{

            return false;
        }

    }

    private float xoryAngleDegrees(float top, float bot1, float bot2) {
        float bot = pow(bot1, 2) + pow(bot2, 2);
        bot = (float)Math.sqrt(bot);
        float res = top / bot;
        res = arctan(res);
        return (float)Math.round(Math.toDegrees(res));
    }

    /**
     * Derive the z-angle for measuring tilt.
     * @param x
     * @param y
     * @param z
     * @return value in degrees
     */
    private float zAngleDegrees(float x, float y, float z) {

        float u1 = pow(x, 2) + pow(y, 2);
        float u2 = sqrt(u1);
        float u3 = z / u2;
        float res1 = arctan(u3);
        float res2 = (float)Math.toDegrees(res1);
        return res2;
    }

    private float sqrt(float d) {
        return (float)Math.sqrt(d);
    }
    private float sin(float d) {
        return (float)Math.sin(d);
    }
    private float cos(float d) {
        return (float)Math.cos(d);
    }
    private float arctan(float val) {
        return (float)Math.atan(val);
    }
    private float pow(float val, float exp) {
        return (float)Math.pow(val, exp);
    }


    private float absolute(float x, float y, float z) {
        return (float)Math.sqrt((Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)));
    }






}
