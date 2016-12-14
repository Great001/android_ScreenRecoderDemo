package com.example.dw.screenrecorddemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button mBtnStart;
    private Button mBtnStop;
    private Button mBtnOpenFile;

    private boolean recording = true;

    public static final String TAG = "MainActivity";
    public static final int AUDIO_RECORD_REPERMISSION_CODE = 11;
    public static final int STORAGE_WRITE_PERMISSION_CODE = 12;
    public static final int REQUEST_CODE = 15;

    private String mPathFile;

    //Android 5.0 后提供公开的api实现屏幕截屏和录制
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;    //截屏使用
    private MediaRecorder mediaRecorder;   //屏幕录制使用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnStart = (Button) findViewById(R.id.btn_start_record);
        mBtnStop = (Button) findViewById(R.id.btn_stop_record);
        mBtnOpenFile = (Button) findViewById(R.id.btn_open_file);

        //1.获取ProjectionManager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaRecorder = new MediaRecorder();  //创建MediaRecorder

        mPathFile = getsaveDirectory();

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecord();  //开始屏幕录制
                Toast.makeText(MainActivity.this,"屏幕录制中.....",Toast.LENGTH_SHORT).show();
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();  //停止屏幕录制
                Toast.makeText(MainActivity.this,"已停止录制",Toast.LENGTH_SHORT).show();
            }
        });

        mBtnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setDataAndType(Uri.fromFile(new File(mPathFile)),"video/*");
                startActivity(intent);
            }
        });
    }


    //android 6.0 动态申请相关权限
    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_RECORD_REPERMISSION_CODE);
            }
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_WRITE_PERMISSION_CODE);
            }
        }
    }


    //开始屏幕录制
    public void startRecord() {
        requestPermission();
        //2.开始屏幕录制
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }


    public void stopRecord() {
        //必须先要预先判断，否则调用stop方法是会抛出异常
        if(isRecording()) {
            mediaRecorder.stop();
        }
        mediaRecorder.reset();
        virtualDisplay.release();
        mediaProjection.stop();
    }

    public void createVirturalDisplay() {
        //3.获取虚拟屏幕  关键是
        virtualDisplay = mediaProjection.createVirtualDisplay(TAG, 480, 720, 240, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }


    public void initMediaRecoder() {
        //注意顺序！！！
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);   //设置音频源，从麦克风中获取
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);    //设置视频源，从Surface中获取
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);   //设置视频输出格式
        mediaRecorder.setOutputFile(mPathFile + System.currentTimeMillis() + ".mp4");  //设置文件的输出路径，之前就掉进这个坑了
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);  //设置音频编码器
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);   //设置视频编码器
        mediaRecorder.setVideoSize(480, 720);   //设置视频文件的分辨率或者说是大小
        mediaRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);   //设置视频编码位率
        mediaRecorder.setVideoFrameRate(30);  //设置视频的帧率
        try {
            mediaRecorder.prepare();   //千万别忘了prepare
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void createImageReader() {
        imageReader = ImageReader.newInstance(480, 720, PixelFormat.RGBA_8888, 2);
    }


    public boolean isRecording(){
        return recording;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                return;
            }
            initMediaRecoder();
            createVirturalDisplay();
            mediaRecorder.start();
            recording = true;

        }
    }

    public String getsaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScreenRecord" + "/";

            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }
            return rootDir;
        } else {
            return null;
        }
    }
}
