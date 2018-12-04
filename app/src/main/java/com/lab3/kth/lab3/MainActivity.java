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

public class MainActivity extends AppCompatActivity {

    private float prevAngle = 0;
    private float prevTiltX = 0;
    private float prevTiltY = 0;
    private float prevTiltZ = 0;
    private float acceTiltAlpha = (float)0.2;
    private float acceAlpha = (float)0.2;
    private float accelTiltPrevTimestamp = 0;
    private int nr = 1;
    private TextView degreesView;
    private StringBuilder builder;
    private boolean doWrite;
    SensorManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        degreesView = findViewById(R.id.degreesView);
        builder = new StringBuilder();
        doWrite = false;

        Button start = findViewById(R.id.startWrite);
        start.setOnClickListener(event -> startWriter());
        Button stop = findViewById(R.id.stopWrite);
        stop.setOnClickListener(event -> stopWriter());
        Button reset = findViewById(R.id.resetWrite);
        reset.setOnClickListener(event -> resetWriter());


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
                manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyro != null) {
            manager.registerListener(
                    sensorEventListener, gyro, SensorManager.SENSOR_DELAY_GAME);
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
    }

    private void handleTiltChange(float x, float y, float z) {
        x = (prevTiltX * (1-acceTiltAlpha)) + (x * acceTiltAlpha);
        y = (prevTiltY * (1-acceTiltAlpha)) + (y * acceTiltAlpha);
        z = (prevTiltZ * (1-acceTiltAlpha)) + (z * acceTiltAlpha);
        write(x, y, z);
        float angle = zAngleDegrees(x, y, z);
        angle = (prevAngle * (1-acceTiltAlpha)) + (angle * acceTiltAlpha);
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
