package com.yyf.testipc1;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class BackgroundVideoRecorderService extends Service {

    private static final String TAG = "BackgroundVideoRecorderService";
    private static final String CHANNEL_ID = "BackgroundVideoRecorderServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private CameraDevice mCameraDevice;
    private MediaRecorder mMediaRecorder;
    private CameraCaptureSession mCaptureSession;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("yyff", "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startBackgroundThread();
        Log.e("yyff", "onCreate");
        // 创建通知
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background Video Recorder")
                .setContentText("Recording video in the background")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        // 开启前台服务
       startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRecording();
        Log.e("yyff", "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        stopBackgroundThread();
        super.onDestroy();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("BackgroundVideoRecorderThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            return;
        }
        Log.e("yyff", "startRecording");
        String cameraId = null;
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }

            if (cameraId == null) {
                return;
            }

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] videoSizes = map.getOutputSizes(MediaRecorder.class);
            Size videoSize = videoSizes[0];

            // 设置 MediaRecorder
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            mMediaRecorder.setVideoEncodingBitRate(5000000);
            mMediaRecorder.setVideoFrameRate(30);
            File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "background_video.mp4");
            mMediaRecorder.setOutputFile(outputFile.getAbsolutePath());
            mMediaRecorder.prepare();

            // 打开相机
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    Log.e("yyff", "onOpened");
                    startCameraCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Log.e("yyff", "onDisconnected");
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    cameraDevice.close();
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    private void startCameraCaptureSession() {
        try {
            Surface recorderSurface = mMediaRecorder.getSurface();
            final CaptureRequest.Builder requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            requestBuilder.addTarget(recorderSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(recorderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    try {
                        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        mCaptureSession.setRepeatingRequest(requestBuilder.build(), null, mBackgroundHandler);
                        mMediaRecorder.start();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Background Video Recorder";
            String description = "Notification channel for background video recording service";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}