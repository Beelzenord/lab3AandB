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

public class MainActivity extends AppCompatActivity {

    private float prevAngle = 0;
    private float acceAlpha = (float)0.85;
    private float accelPrevTimestamp = 0;
    private TextView degreesView;
    SensorManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        degreesView = findViewById(R.id.degreesView);

        manager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer =
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            manager.registerListener(
                    sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }


        manager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);
        Sensor gyro =
                manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
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

        float angle = zAngleDegrees(x, y, z);
        angle = (prevAngle * (1-acceAlpha)) + (angle * acceAlpha);
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

        float timeDiffMilli = (time - accelPrevTimestamp) / 1000000;
        if (timeDiffMilli > 1000) {
            accelPrevTimestamp = time;
            degreesView.setText(Math.round(angle) + "°");
//            Log.i("Main", "angle; " + angle);
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
