package com.example.screenrecorder_test;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ServiceClass extends Service {

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private int mResultCode;
    private Intent mResultData;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;

    File mVideoFolder;
    String mVideoFileName;

    private void createVideoFolder() {
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        mVideoFolder = new File(movieFile, "ScreenRec");
        if (!mVideoFolder.exists()) {
            mVideoFolder.mkdirs();
        }
        try {
            createVideoFileName();
        } catch (IOException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createVideoFileName() throws IOException {
        String timestamp = new SimpleDateFormat("YYYYMMDD_HHmmss").format(new Date());
        String fileName = "ECG" + timestamp;

        String filePath = mVideoFolder + File.separator + fileName + ".mp4";
        File file = new File(filePath);
        mVideoFileName = file.getAbsolutePath();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG", " onStartCommand intent = " + intent);
        createNotificationChannel();
        createVideoFolder();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "001";
            String channelName = "myChannel";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Notification notification;

                notification = new Notification.
                        Builder(getApplicationContext(), channelId).setOngoing(true).setSmallIcon(R.mipmap.ic_launcher).setCategory(Notification.CATEGORY_SERVICE).build();

                startForeground(101, notification);
            }
        } else {
            startForeground(101, new Notification());
        }

        mResultCode = intent.getIntExtra("code", -1);
        mResultData = intent.getParcelableExtra("data");
        mScreenWidth = intent.getIntExtra("width", 720);
        mScreenHeight = intent.getIntExtra("height", 1280);
        mScreenDensity = intent.getIntExtra("density", 1);



        mMediaProjection = createMediaProjection();
        mMediaRecorder = createMediaRecorder();
        mVirtualDisplay = createVirtualDisplay();

        mMediaRecorder.start();

        //do MediaProjection things that you want
        return START_NOT_STICKY;
    }

    private VirtualDisplay createVirtualDisplay() {
        Log.i("TAG", "Create VirtualDisplay");
        return mMediaProjection.createVirtualDisplay("this", mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }


    private MediaRecorder createMediaRecorder() {
        MediaRecorder mediaRecorder = new MediaRecorder();

        try {
//            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//            mediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);  //after setVideoSource(), setOutFormat()
//
//            mediaRecorder.setOutputFile(mVideoFileName);
//            mediaRecorder.setVideoEncodingBitRate(mScreenWidth * mScreenHeight);
//            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);  //after setOutputFormat()
//            mediaRecorder.setVideoFrameRate(1);

            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(25);
            //mediaRecorder.setVideoSize(1280, 720);
            mediaRecorder.setVideoSize(1920, 1080);
            mediaRecorder.setOutputFile(mVideoFileName);

        }catch (Exception e)
        {
           // Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("Mediarec",e.toString());
        }

       // int bitRate;
       // bitRate = mScreenWidth * mScreenHeight / 1000;
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            Log.e("TAG", "createMediaRecorder: e = " + e.toString());
        }
        return mediaRecorder;
    }


    private MediaProjection createMediaProjection() {
        return ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).
                getMediaProjection(mResultCode, mResultData);
    }


    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        Intent nfIntent = new Intent(this, MainActivity.class);

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                //.setContentTitle("Title")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("is running......")
                .setWhen(System.currentTimeMillis());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(110, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }else
        {
            startForeground(110, notification);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("TAG", "Service onDestroy");
        stopForeground(true);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaProjection.stop();
            mMediaRecorder.reset();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }
}
