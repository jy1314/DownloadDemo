package com.example.down;
/*
* @author Jerry
* create at 2019/4/28 下午8:04
* description:对下载状态监听的接口
*/
public interface DownloadListener {
    //当前的下载进度
    void onProgress(int progress);
    //下载成功
    void onSuccess();
    //下载失败
    void onFailed();
    //下载暂停
    void onPaused();
    //下载取消
    void onCanceled();
}
