package com.example.down;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
/*
* @author Jerry
* create at 2019/4/28 下午8:35
* description:实现具体的下载功能。
* 继承自AsyncTask，三个参数类型String, Integer, Integer，分别是传入的参数，进度参数，结果参数的类型。
*/
public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    //常量用于表示下载状态，最终的返回结果
    public static final int TYPE_SUCCESS = 0;//成功
    public static final int TYPE_FAILED = 1;//失败
    public static final int TYPE_PAUSED = 2;//暂停
    public static final int TYPE_CANCELED = 3;//取消

    private DownloadListener listener;//用于监听下载状态的listener
    private boolean isCanceled = false;//是否取消
    private boolean isPaused = false;//是否暂停
    private int lastProgress;//最终的进度

    /*
     * @author: Jerry
     * @create at 2019/4/28 下午8:38
     * @Param: DownloadListener listener
     * @description: 构造方法，传入DownloadListener，下载状态通过这个listener进行回调返回Service中
     * @return:
     */
    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }
    /*
     * @author: Jerry
     * @create at 2019/4/28 下午8:39
     * @Param:String... strings strings[0]为具体的下载url地址
     * @description: 后台执行具体的下载任务
     * @return: 下载结果
     */
    @Override
    protected Integer doInBackground(String... strings) {
        InputStream inputStream = null;//输入流
        RandomAccessFile saveFile = null;
        //文件访问，由于下载有可能暂停之后继续，因此使用可以访问文件任意地方的RandomAccessFile更方便
        File file = null;//文件
        try{
            long downloadedLength = 0;//记录已下载文件的长度
            String downloadUrl = strings[0];//获取下载url地址
            //通过获取最后一个/之后的字符获得文件名
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));//解析出文件名
            String directory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getPath();
            //文件要下载到的目录
            file = new File(directory + fileName);//创建文件
            if (file.exists()){//文件已存在，则获取已存在的字节数
                downloadedLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);//获取待下载文件的总长度
            if (contentLength == 0){//长度为0说明有问题，直接结束下载
                return TYPE_FAILED;
            }else if (contentLength == downloadedLength){
                //已下载字节和待下载文件总字节相等，说明已经下载完毕，直接返回已完成
                return TYPE_SUCCESS;
            }
            //建立Okhttp
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RENGE","bytes=" + downloadedLength + "-")//添加一个header，说明要从什么位置开始请求
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if(response != null){
                inputStream = response.body().byteStream();//使用流的方式读取，获取输入流
                saveFile = new RandomAccessFile(file,"rw");
                saveFile.seek(downloadedLength);//跳过已下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while((len = inputStream.read(b)) != -1){
                    //判断是否需要暂停或取消
                    if(isCanceled){
                        return TYPE_CANCELED;
                    }else if (isPaused){
                        return TYPE_PAUSED;
                    }else {
                        //不需要暂停或取消的话，写入文件，并更新下载进度
                        total += len;
                        saveFile.write(b, 0, len);
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        publishProgress(progress);//更新下载进度
                    }
                }
                //下载完成返回
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //避免因出现异常而引发内存泄露，在finally中释放资源（保证可以执行）
            try{
                if (inputStream != null){
                    inputStream.close();
                }
                if(saveFile != null){
                    saveFile.close();
                }
                if(isCanceled && file != null){
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    /*
     * @author: Jerry
     * @create at 2019/4/28 下午8:40
     * @Param: Integer integer下载结果
     * @description: 通过回调listener，通知最终的下载结果
     * @return:
     */
    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
            default:
                break;
        }
    }

    /*
     * @author: Jerry
     * @create at 2019/4/28 下午8:41
     * @Param:
     * @description: 在界面上更新当前的下载进度
     * @return:
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if(progress> lastProgress){//有变化才更新，没有就不变
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    public void pauseDownload(){
        isPaused = true;
    }

    public void cancelDownload(){
        isCanceled = true;
    }

    /*
     * @author: Jerry
     * @create at 2019/4/28 下午8:53
     * @Param:
     * @description: 获取要下载的文件长度
     * @return:
     */
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if(response != null && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}
