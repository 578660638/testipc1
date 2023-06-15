package com.yyf.testipc1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yyf.testipc1.BackgroundVideoRecorderService;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "sad";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private IDateTimeService dateTimeService;
    private boolean isBound = false;

    private Button btnGetDateTime;
    private TextView tvDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnGetDateTime = findViewById(R.id.btn_get_datetime);
        tvDateTime = findViewById(R.id.tv_datetime);
        showchuagan(this.getApplicationContext());
        boolean isBootloaderLocked = isBootloaderLocked();
        Toast.makeText(this, "Bootloader锁定状态: " + (isBootloaderLocked ? "已锁定" : "未锁定"), Toast.LENGTH_SHORT).show();
        String a1 = stringFromJNI();
        Log.i(TAG, "onCreate: "+a1);
        btnGetDateTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    try {
                        String dateTime = dateTimeService.getCurrentDateTime();
                        tvDateTime.setText(dateTime);
                        requestPermissions();

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Intent intent = new Intent(this, DateTimeService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
    private static final int PERMISSION_REQUEST_CODE = 1;

    private boolean isBootloaderLocked() {
        String bootloaderLocked = getSystemProperty("ro.boot.flash.locked", "unknown");
        return "1".equals(bootloaderLocked);
    }
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE
    };
    private String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getMethod("get", String.class, String.class);
            return (String) getMethod.invoke(null, key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }
    private void requestPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            Intent startIntent = new Intent(this, BackgroundVideoRecorderService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.e("yyff","开始了");
                startForegroundService(startIntent);
            } else {
                startService(startIntent);
            }
            // All permissions are granted, perform your actions
        }
    }



public  void showchuagan(Context context){
    List<Sensor> sensorListmy = new ArrayList<>();

// 获取传感器管理器
    SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

// 获取所有类型传感器列表
    List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

// 遍历列表添加到 sensorList
    for (Sensor sensor : sensorList) {
      sensorListmy.add(sensor);
        //Log.e("sensor","sad"+sensor.getVendor());
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        ArrayList<Sensor> aospSensor = new ArrayList<>();
        for(Sensor sensor:sensorListmy){
            if(sensor.getVendor().contains("Google")){
                aospSensor.add(sensor);
                Log.e("sensor","sad"+sensor.getVendor());
            }
        }
        if (aospSensor.size()>3) {
            Log.e("sensor","sad");

        }
    }
}
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            dateTimeService = IDateTimeService.Stub.asInterface(service);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };
    public native String stringFromJNI();
}