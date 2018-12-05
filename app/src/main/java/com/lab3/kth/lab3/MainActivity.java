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
    private float prevMagnX = 0;
    private float prevMagnY = 0;
    private float prevMagnZ = 0;
    private float acceTiltAlpha = (float) 0.7;
//    private float acceAlpha = (float) 0.2;
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

    private final float THRESHOLD = (float)1.3;
    private final int INDEXS_TO_CLEAR = 25;

    private final long DURANTION = 1000L;

    private boolean xDeviation;
    private boolean yDeviation;
    private boolean zDeviation;

    private boolean colorFlag;

    private int counter;

    private Timer timer;
    private float[] filteredGravity;
    private float[] linearAcceleration;

    private float[] mGeomagnetic;

    float roll;


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
        filteredGravity = new float[]{0,0,0};
        linearAcceleration = new float[]{0,0,0};
        mGeomagnetic = new float[]{0,0,0};
        counter = -1;

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
                    handleMagneticField(event);
                    break;

                case Sensor.TYPE_GYROSCOPE:
//                    handleGyroEvent(event);
                    break;



                case Sensor.TYPE_LINEAR_ACCELERATION:
//                    System.out.println(" X : " + event.values[0] + " Y " + event.values[1] + " Z " + event.values[2]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private void handleMagneticField(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        prevMagnX = (prevMagnX * acceTiltAlpha + ((1-acceTiltAlpha) * x));
        prevMagnY = (prevMagnY * acceTiltAlpha + ((1-acceTiltAlpha) * y));
        prevMagnZ = (prevMagnY * acceTiltAlpha + ((1-acceTiltAlpha) * z));


/*
        float timeD = (accelPrevTimestamp - accelTiltPrevTimestamp) / 1000000;
        float[] cross = crossProduct(prevTiltX, prevTiltY, prevTiltZ, x, y, z);
        cross = crossProduct(cross[0], cross[1], cross[2], prevTiltX, prevTiltY, prevTiltZ);

        if (timeD > 1000) {
            Log.i("Timestamp", "ACCEL: " + absolute(prevTiltX, prevTiltY, prevTiltZ));
            Log.i("Timestamp", "MAGNE: " + absolute(x, y, z));
            Log.i("Timestamp", "CROSS: " + absolute(cross[0], cross[1], cross[2]));
            accelTiltPrevTimestamp = accelPrevTimestamp;
        }
*/
    }

    private void handleAccelerationEvent(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        accelPrevTimestamp = event.timestamp;
        handleTiltChange(x, y, z);
        handleShakeChange(x, y, z, event.timestamp);
        handleOrientation(x,y,z);
    }

    private void handleOrientation(float x, float y, float z) {



    }

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

    private void removeIndexs(ArrayList<Float> values, int indexs) {

        for(int i = 0 ; i < indexs ; i++){
            if(i >= values.size()){
               break;
            }
            values.remove(i);
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
        x = (prevTiltX * (acceTiltAlpha)) + (x * (1 - acceTiltAlpha));
        y = (prevTiltY * (acceTiltAlpha)) + (y * (1 - acceTiltAlpha));
        z = (prevTiltZ * (acceTiltAlpha)) + (z * (1 - acceTiltAlpha));
        x = (prevTiltX * (1-acceTiltAlpha)) + (x * acceTiltAlpha);
        y = (prevTiltY * (1-acceTiltAlpha)) + (y * acceTiltAlpha);
        z = (prevTiltZ * (1-acceTiltAlpha)) + (z * acceTiltAlpha);



        float R[] = new float[9];
        float I[] = new float[9];
        float[] accelerometer = new float[]{x,y,z};

        if(SensorManager.getRotationMatrix(R,I,accelerometer,mGeomagnetic)){
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);
            float azimut = orientation[0];
            float pitch = orientation[1];
            float roll = orientation[2];
            // roll = orientation[2] * -57;

            Log.i("degrees ", "AZIMUT " + Math.toDegrees(azimut) + " " + Math.toDegrees(pitch) + " "+ Math.toDegrees(roll));

        }



        write(x, y, z);
        float angle = zAngleDegrees(x, y, z);

        angle = (prevAngle * (acceTiltAlpha)) + (angle * (1 - acceTiltAlpha));
        prevAngle = angle;
        prevTiltX = x;
        prevTiltY = y;
        prevTiltZ = z;

        if (x >= 0 && y >= 0 && z >= 0) { //angle +

        }
        else if (x >= 0 && y >= 0 && z <= 0) { //angle -
            angle = 360 + angle;
        }
        else if (x <= 0 && y >= 0 && z >= 0) { //angle +
            angle = 90 + angle;
        }
        else if (x <= 0 && y >= 0 && z <= 0) { //angle -
            angle = 180 + (Math.abs(angle));
        }
        else if (x >= 0 && y <= 0 && z >= 0) { //angle +

        }
        else if (x >= 0 && y <= 0 && z <= 0) { //angle -
            angle = 360 + angle;
        }
        else if (x <= 0 && y <= 0 && z >= 0) { //angle +
            angle = 180 - angle;
        }
        else if (x <= 0 && y <= 0 && z <= 0) { //angle -
            angle = 180 + Math.abs(angle);
        }
        else if (x <= 0 && y >= 0 && z >= 0)

        // Bottom left
        if (y <= 0 && x <= 0 && angle >= 0) {
            float tmp = 90 - angle;
            angle = 90 + tmp;
        }
        // Bottom Right
        else if (y <= 0 && x <= 0&& angle <= 0) {
            float tmp = angle * -1;
            angle = 180 + tmp;
        }
        // Top Right
        else if (y >= 0 && x >= 0 && angle <= 0) {
            float tmp = 90 + angle;
            angle = 270 + tmp;
        }



        int roundedAngle = Math.round(angle);
        if (roundedAngle == 360)
            roundedAngle = 0;
        float timeD = (accelPrevTimestamp - accelTiltPrevTimestamp) / 1000000;
        if (timeD > 1000) {
            Log.i("Timestamp", "angle: " + prevAngle);
            Log.i("Timestamp", "AX: " + x);
            Log.i("Timestamp", "AY: " + y);
            Log.i("Timestamp", "AZ: " + z);
            Log.i("Timestamp", "MX: " + prevMagnX);
            Log.i("Timestamp", "MY: " + prevMagnY);
            Log.i("Timestamp", "MZ: " + prevMagnZ);
            Log.i("Timestamp", "MA: " + absolute(prevMagnX, prevMagnY, prevMagnZ));
            accelTiltPrevTimestamp = accelPrevTimestamp;
        }
        if (roundedAngle == 892) {
            Log.i("Timestamp", "Rounded: 89");
            Log.i("Timestamp", "angle " + prevAngle);
            Log.i("Timestamp", "Y: " + y);
        }
        if (roundedAngle == 902) {
            Log.i("Timestamp", "Rounded: 90");
            Log.i("Timestamp", "angle " + prevAngle);
            Log.i("Timestamp", "Y: " + y);
        }
        if (roundedAngle == 912) {
            Log.i("Timestamp", "Rounded: 91");
            Log.i("Timestamp", "angle " + prevAngle);
            Log.i("Timestamp", "Y: " + y);
        }
        degreesView.setText(roundedAngle + "°");
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

    private float zAngleDegrees(float x, float y, float z) {
       // System.out.println("X: " +  x  + " Y: " + y + " Z: " + z);


        float Roll = (float)(Math.atan2((double)y, (double)z) * 180/(float)Math.PI);
        float Pitch = (float)Math.atan2(-x, sqrt(y*y + z*z)) * 180/(float)Math.PI;

        Log.i("ROLL","ROLL  " + Roll + " Pitch " + Pitch);
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

    private float[] crossProduct(float x1, float y1, float z1, float x2, float y2, float z2) {
        float[] tmp = new float[3];
        tmp[0] = (y1*z2 - y2*z1);
        tmp[1] = (x2*z1 - x1*z2);
        tmp[2] = (x1*y2 - x2*y1);
        return tmp;
    }

    private float absolute(float x, float y, float z) {
        return (float)Math.sqrt((Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)));
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
