package com.example.down;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/*
* @author Jerry
* create at 2019/4/28 下午7:56
* description:下载功能的练习Demo，基于okhttp3
*/
public class MainActivity extends AppCompatActivity {
    @BindView(R.id.start_download)
    Button start_dowanload;
    @BindView(R.id.pause_download)
    Button pause_dowanload;
    @BindView(R.id.cancel_download)
    Button cancel_dowanload;

    private DownloadService.DownloadBinder downloadBinder;
    //绑定Service用
    private ServiceConnection connection = new ServiceConnection() {

        //Service和Activity成功绑定时调用
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            downloadBinder = (DownloadService.DownloadBinder) iBinder;
        }
        //Service和Activity连接断开时调用
        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);//绑定ButterKnife
        Intent intent = new Intent(this,DownloadService.class);
        startService(intent);//启动服务
        bindService(intent, connection, BIND_AUTO_CREATE);//绑定服务
        //启动服务可以保证服务一直在后台运行，绑定服务则让MainActivity和DownloadService可以通信，二者缺一不可
        //动态申请访问SD卡权限
        if(ContextCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
    }

    //点击下载按钮
    @OnClick(R.id.start_download)
    public void startOnClick(){
        if(downloadBinder == null){
            return;
        }
        String url = "https://raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe";
        downloadBinder.startDownload(url);
    }

    //点击暂停按钮
    @OnClick(R.id.pause_download)
    public void pauseOnClick(){
        if(downloadBinder == null){
            return;
        }
        downloadBinder.pauseDownload();
    }

    //点击取消按钮
    @OnClick(R.id.cancel_download)
    public void cancelOnClick(){
        if(downloadBinder == null){
            return;
        }
        downloadBinder.cancelDownload();
    }

    //申请权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"拒绝权限则无法使用应用程序",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);//Activity被销毁时一定要解绑Service，防止内存泄露
    }
}
