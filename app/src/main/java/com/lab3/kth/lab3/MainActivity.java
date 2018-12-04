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
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private float prevAngle = 0;
    private float prevxFiltered = 0;
    private float prevyFiltered = 0;
    private float prevzFiltered = 0;
    private float acceAlpha = (float)0.85;
    private float highPassFilter = (float) 0.12;
    private float accelPrevTimestamp = 0;
    private ArrayList<Float> xValues;
    private ArrayList<Float> yValues;
    private ArrayList<Float> zValues;

    private TextView degreesView;
    SensorManager manager;

    private TimerTask timerTask;

    private final float THRESHOLD = 15;

    private final long DURANTION = 1000L;

    private boolean xDeviation;
    private boolean yDeviation;
    private boolean zDeviation;

    private boolean colorFlag;

    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        colorFlag = false;


        xDeviation = false;
        xValues = new ArrayList<Float>();
        yValues = new ArrayList<Float>();
        zValues = new ArrayList<Float>();
        degreesView = findViewById(R.id.degreesView);



        timer = new Timer();



        manager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer =
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            manager.registerListener(
                    sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }


        manager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);
        Sensor gyro =
                manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (gyro != null) {
            manager.registerListener(
                    sensorEventListener, gyro, SensorManager.SENSOR_DELAY_NORMAL);
        }


    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch(event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER :
                    handleAccelerationEvent(event);
                    break;

                case Sensor.TYPE_GYROSCOPE :
//                    handleGyroEvent(event);
                    break;

                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        System.out.println(" X : " + event.values[0] + " Y " + event.values[1] + " Z "  + event.values[2]);
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private void handleAccelerationEvent(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        long time = event.timestamp;

        float[] g = new float[3];
        g = event.values.clone();
        float norm_Of_g = (float) Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);


        // Normalize the accelerometer vector
        g[0] = g[0] / norm_Of_g;
        g[1] = g[1] / norm_Of_g;
        g[2] = g[2] / norm_Of_g;

        int inclination = (int) Math.round(Math.toDegrees(Math.acos(g[2])));


      //  System.out.println("inclination " +inclination);
        float angle = zAngleDegrees(x, y, z);



        angle = (prevAngle * (acceAlpha)) + (angle * (1-acceAlpha));
        prevAngle = angle;

        float xFiltered = (prevxFiltered * (highPassFilter)) + (x * (1-highPassFilter));
        float yFiltered = (prevxFiltered * (highPassFilter)) + (y * (1-highPassFilter));
        float zFiltered = (prevxFiltered * (highPassFilter)) + (z * (1-highPassFilter));


        // Bottom left
        if (y < 0 && angle > 0) {
            float tmp = 90 - angle;
            angle = 90 + tmp;
        }
        // Bottom right
        else if (y < 0 && angle < 0) {
            float tmp = angle * -1;
            angle = 180 + tmp;
        }
        // Top left
        else if (y > 0 && angle < 0) {
            float tmp = 90 + angle;
            angle = 270 + tmp;
        }

        xValues.add(xFiltered);
        yValues.add(yFiltered);
        zValues.add(zFiltered);


        if(xValues.size()==50 || yValues.size() == 50 || zValues.size() == 50){
            xDeviation = findStandardDeviation(xValues);
            yDeviation = findStandardDeviation(yValues);
            zDeviation = findStandardDeviation(zValues);
        }
        if(xDeviation || yDeviation || zDeviation){

        }


        float timeDiffMilli = (time - accelPrevTimestamp) / 1000000;
         if (timeDiffMilli > 1000 && (xDeviation || yDeviation || zDeviation)) {
            accelPrevTimestamp = time;
            degreesView.setText(Math.round(angle) + "°");
             degreesView.setTextColor(getResources().getColor(R.color.red,null));
//            Log.i("Main", "angle; " + angle);
        }
    }

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

        Log.i("Mean","Standard Deviation : " + standDev);
         xValues.remove(0);
        Log.i("Mean","Mean : " + average);
        xValues.remove(0);

        if((average + THRESHOLD) < standDev){
            System.out.println("Performing shift " + average + " " + standDev);
            return true;
        }
        else{

            return false;
        }

    }

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
}
