package com.example.pc_libo.myupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends Activity {

    Context context;
    LayoutInflater inflater;
    View dialogView;
    Button btnDialog;
    float fileSize;
    ProgressBar myProgress;

    private Button button1;
    private static final String APK_URL_STRING = "https://bdp.haizhi.com/packages/bdp.apk";
    private static final String STORE_DIR = "/BDP/";
    private static int down = 0;
    File file;

    private RemoteViews remoteView=null;
    private Notification notification;
    private NotificationManager notificationManager;
    NotificationCompat.Builder mBuilder;
    private boolean flag = true;
    private PendingIntent pIntent;//更新显示

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case 1:
                    button1.setText("点击安装");
                    down = 1;
                    break;
                case 2:
                    down = 2;
                    button1.setText("打开");
                    break;
            }
        }

    };


    private Handler  progressHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int progress = msg.what;
            myProgress.setProgress(progress);
            mBuilder.setSmallIcon(R.drawable.ic_launcher);
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
            mBuilder.setProgress(100,progress,false);
            notification = mBuilder.build();
            notificationManager.notify(0 , notification);
            notification = null;
            Log.i("percent",String.valueOf(msg.what));

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        UmengUpdateAgent.setUpdateOnlyWifi(false);
//        UmengUpdateAgent.update(this);  //友盟更新提示


        init();

        initNotification();

        initDialog();

        showDialog();

        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 下载apk
                if (down == 0) {
                    downFile(APK_URL_STRING);
                    button1.setText("正在下载");
                    // 安装APK
                } else if (down == 1) {
                    installApk();
                    // 打开apk
                } else if (down == 2) {
                    openApk(MainActivity.this, APK_URL_STRING);
                }

            }
        });

    }

    private void init (){
        context = this;
        inflater = LayoutInflater.from(context);
        dialogView = inflater.inflate(R.layout.dialog, null);
        btnDialog = (Button)findViewById(R.id.btn_dialog);
        myProgress = (ProgressBar)findViewById(R.id.myprogress);

    }

    private void initNotification(){
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView=new RemoteViews(getPackageName(),R.layout.notification_download_view);
        mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setContentTitle("BDP");

        mBuilder.setTicker("BDP");//第一次提示消息的时候显示在通知栏上
        mBuilder.setAutoCancel(true);
        mBuilder.setContentIntent(pendingIntent);
        notification = mBuilder.build();

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void initDialog(){

//        String str = "/CanavaCancel.apk";
//        String fileName = Environment.getExternalStorageDirectory() + str;
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(Uri.fromFile(new File(fileName)), "application/vnd.android.package-archive");
//        startActivity(intent);
//
//        Uri packageURI = Uri.parse("package:com.demo.CanavaCancel");
//        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
//        startActivity(uninstallIntent);
    }

    private void showDialog() {
        btnDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(dialogView);
                AlertDialog valueInputDialog = builder.create();

                Button download = (Button) dialogView.findViewById(R.id.btn_dialog);

                download.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

                valueInputDialog.show();

            }
        });
    }


    // 接收到安装完成apk的广播
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            System.out.println("接收到安装完成apk的广播");

            Message message = handler.obtainMessage();
            message.what = 2;
            handler.sendMessage(message);
        }
    };

    /**
     * 后台在下面一个Apk 下载完成后返回下载好的文件
     *
     * @param httpUrl
     * @return
     */
    private File downFile(final String httpUrl) {

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    URL url = new URL(httpUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    FileOutputStream fileOutputStream = null;
                    InputStream inputStream;
                    if (connection.getResponseCode() == 200) {

                        fileSize = connection.getContentLength();
                        inputStream = connection.getInputStream();

                        if (inputStream != null) {
                            file = getFile(httpUrl);
                            fileOutputStream = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int length = 0;
                            float downLoadFileSize = 0;
                            int progress = 0;
                            while ((length = inputStream.read(buffer)) != -1) {
                                fileOutputStream.write(buffer, 0, length);
                                downLoadFileSize += length;
                                int tempProgress = (int)(downLoadFileSize/fileSize * 100);
                                if(tempProgress != progress){
                                    progress = tempProgress;
                                    progressHandler.sendEmptyMessage(progress);
                                }

                                Log.i("progress", String.valueOf(downLoadFileSize));
                                Log.i("filesize",String.valueOf(fileSize));
                            }
                            fileOutputStream.close();
                            fileOutputStream.flush();
                        }
                        inputStream.close();
                    }

                    System.out.println("已经下载完成");
                    // 往handler发送一条消息 更改button的text属性
                    Message message = handler.obtainMessage();
                    message.what = 1;
                    handler.sendMessage(message);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        return file;
    }

    /**
     * 安装APK
     */
    private void installApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");

        // 注册一个广播
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解除广播
        unregisterReceiver(broadcastReceiver);
    }

    /**
     * 打开已经安装好的apk
     */
    private void openApk(Context context, String url) {
        PackageManager manager = context.getPackageManager();
        // 这里的是你下载好的文件路径
        PackageInfo info = manager.getPackageArchiveInfo(Environment.getExternalStorageDirectory().getAbsolutePath()
                + getFilePath(url), PackageManager.GET_ACTIVITIES);
        if (info != null) {
            Intent intent = manager.getLaunchIntentForPackage(info.applicationInfo.packageName);
            startActivity(intent);
        }
    }

    /**
     * 根据传过来url创建文件
     *
     */
    private File getFile(String url) {

        File files = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), getFilePath(url));
        return files;
    }

    /**
     * 截取出url后面的apk的文件名
     *
     * @param url
     * @return
     */
    private String getFilePath(String url) {
        return url.substring(url.lastIndexOf("/"), url.length());
    }


}
