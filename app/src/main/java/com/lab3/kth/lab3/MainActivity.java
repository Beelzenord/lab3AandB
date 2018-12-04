package com.lab3.kth.lab3;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final static int RED_COLOR = 1;
    private final static int BLACK_COLOR = 2;
    private int currentColor;
    private float prevAngle = 0;
    private float prevTiltX = 0;
    private float prevTiltY = 0;
    private float prevTiltZ = 0;
    private float acceTiltAlpha = (float) 0.2;
    private float acceAlpha = (float) 0.2;
    private float accelTiltPrevTimestamp = 0;
    private int nr = 1;
    private float prevxFiltered = 0;
    private float prevyFiltered = 0;
    private float prevzFiltered = 0;
    private float highPassFilter = (float) 0.12;
    private float accelPrevTimestamp = 0;
    private ArrayList<Float> xValues;
    private ArrayList<Float> yValues;
    private ArrayList<Float> zValues;

    private TextView degreesView;
    private StringBuilder builder;
    private boolean doWrite;
    SensorManager manager;

    private long shakeStartTimer;
    private boolean isShaking;

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
        isShaking = false;
        currentColor = BLACK_COLOR;
        xDeviation = false;
        xValues = new ArrayList<Float>();
        yValues = new ArrayList<Float>();
        zValues = new ArrayList<Float>();
        degreesView = findViewById(R.id.degreesView);
        builder = new StringBuilder();
        doWrite = false;

        Button start = findViewById(R.id.startWrite);
        start.setOnClickListener(event -> startWriter());
        Button stop = findViewById(R.id.stopWrite);
        stop.setOnClickListener(event -> stopWriter());
        Button reset = findViewById(R.id.resetWrite);
        reset.setOnClickListener(event -> resetWriter());


        timer = new Timer();


        manager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer =
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            manager.registerListener(
                    sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
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

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    handleAccelerationEvent(event);

                    break;

                case Sensor.TYPE_GYROSCOPE:
//                    handleGyroEvent(event);
                    break;

                case Sensor.TYPE_LINEAR_ACCELERATION:
                    System.out.println(" X : " + event.values[0] + " Y " + event.values[1] + " Z " + event.values[2]);
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
        handleTiltChange(x, y, z);
        handleShakeChange(x, y, z, event.timestamp);

    }

    private void handleShakeChange(float x, float y, float z, long timestamp) {
        float xFiltered = (prevxFiltered * (highPassFilter)) + (x * (1-highPassFilter));
        float yFiltered = (prevyFiltered * (highPassFilter)) + (y * (1-highPassFilter));
        float zFiltered = (prevzFiltered * (highPassFilter)) + (z * (1-highPassFilter));

        xValues.add(xFiltered);
        yValues.add(yFiltered);
        zValues.add(zFiltered);

        if(xValues.size()==50 || yValues.size() == 50 || zValues.size() == 50){
            xDeviation = findStandardDeviation(xValues);
            yDeviation = findStandardDeviation(yValues);
            zDeviation = findStandardDeviation(zValues);
        }

        if(xDeviation || yDeviation || zDeviation){
//            degreesView.setTextColor(getResources().getColor(R.color.red,null));
            if (!isShaking) {
                isShaking = true;
                shakeStartTimer = timestamp;
            }

            if (isShaking) {
                long timeDiff = (timestamp - shakeStartTimer) / 1000000;
                if (timeDiff > 1000) {
                    switchColor();
                    isShaking = false;
                    xValues.clear();
                    yValues.clear();
                    zValues.clear();
                }
            }
        }
        else{

        }
    }

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
      //  System.out.println("inclination " +inclination);
    private void handleTiltChange(float x, float y, float z) {
        x = (prevTiltX * (1-acceTiltAlpha)) + (x * acceTiltAlpha);
        y = (prevTiltY * (1-acceTiltAlpha)) + (y * acceTiltAlpha);
        z = (prevTiltZ * (1-acceTiltAlpha)) + (z * acceTiltAlpha);
        write(x, y, z);
        float angle = zAngleDegrees(x, y, z);
        angle = (prevAngle * (1-acceTiltAlpha)) + (angle * acceTiltAlpha);



        angle = (prevAngle * (acceAlpha)) + (angle * (1-acceAlpha));
        prevAngle = angle;


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
        degreesView.setText(Math.round(angle) + "°");
    }

    public void write(float x, float y, float z) {
        if (doWrite) {
            builder.append("x: " + x);
            builder.append("| y: " + y);
            builder.append("| z: " + z);
            builder.append("\n");
        }
//            accelPrevTimestamp = time;
//            degreesView.setText(Math.round(angle) + "°");
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

    public void startWriter() {
        builder = new StringBuilder();
        doWrite = true;
    }

    public void stopWriter() {
        doWrite = false;
        Log.i("Data", builder.toString());
        Write write = new Write(this, builder.toString(), nr + "data.txt");
        write.run();
    }

    public void resetWriter() {
        builder = new StringBuilder();
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











    public static class Write implements Runnable {
        private Activity activity;
        private Object object;
        private String fileName;
        public Write(Activity activity, Object object, String fileName) {
            this.activity = activity;
            this.object = object;
            this.fileName = fileName;
        }
        @Override
        public void run() {
            if (object != null) {
                FileOutputStream outputStream = null;
                try {
                    outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
                    outputStream.write(object.toString().getBytes());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
