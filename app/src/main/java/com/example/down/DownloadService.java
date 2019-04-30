package com.example.down;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

/*
* @author Jerry
* create at 2019/4/29 下午12:00
* description:download Service
*/
public class DownloadService extends Service {
    private DownloadTask downloadTask;//具体的下载执行者
    private String downloadUrl;//下载链接Url
    private DownloadBinder mBinder = new DownloadBinder();


    //匿名类实例
    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            //获取任务进度
           getNotificationManager().notify(1,getNotification("Downloading......",progress));
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            //下载成功时会将前台服务关闭，并创建一个下载成功的通知
            stopForeground(true);//停止前台服务
            getNotificationManager().notify(1,getNotification("Download Success",-1));
            Toast.makeText(DownloadService.this,"Download Success",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadTask = null;
            //下载失败时会将前台服务关闭，并创建一个下载失败的通知
            stopForeground(true);//停止前台服务
            getNotificationManager().notify(1,getNotification("Download Failed",-1));
            Toast.makeText(DownloadService.this,"Download Failed",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onPaused() {
            //暂停
            downloadTask = null;
            Toast.makeText(DownloadService.this,"Paused",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            //删除任务
            stopForeground(true);
            Toast.makeText(DownloadService.this,"Canceled",Toast.LENGTH_SHORT).show();
        }
    };


    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    class DownloadBinder extends Binder{
        //开始下载
        public void startDownload(String url){
            if(downloadTask == null){
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("Download......",0));
                Toast.makeText(DownloadService.this,"Download......",Toast.LENGTH_SHORT).show();
            }
        }
        //暂停下载
        public void pauseDownload(){
            if(downloadTask != null){
                downloadTask.pauseDownload();
            }
        }
        //删除下载
        public void cancelDownload(){
            if(downloadTask != null){
                downloadTask.cancelDownload();
            }else{
                if(downloadUrl != null){
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if(file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this,"Canceled",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    /*
     * @author: Jerry
     * @create at 2019/4/30 下午3:41
     * @Param:
     * @description:获取 NotificationManager
     * @return:
     */
    private NotificationManager getNotificationManager(){

        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    /*
     * @author: Jerry
     * @create at 2019/4/30 下午3:39
     * @Param: String title, int progress
     * @description: 根据进度构建通知对象
     * @return: Notification
     */
    private Notification getNotification(String title, int progress){
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"default");
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .setContentTitle(title);
        if(progress > 0){
            //只有在进度大于0的时候才显示进度
            builder.setContentText(progress + "%");
            builder.setProgress(100,progress,false);//最大进度、当前进度、是否模糊进度条
        }
        return builder.build();

    }
}
