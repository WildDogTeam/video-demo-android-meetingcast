package com.wilddog.video.meetingcast.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.wilddog.client.SyncReference;
import com.wilddog.client.WilddogSync;
import com.wilddog.video.LocalStream;
import com.wilddog.video.WilddogVideo;
import com.wilddog.video.WilddogVideoClient;
import com.wilddog.video.bean.LocalStreamOptions;
import com.wilddog.video.bean.VideoException;
import com.wilddog.video.listener.CompleteListener;

import org.webrtc.EglBase;

public class MyService extends Service {

    private WilddogVideo video;
    private WilddogVideoClient client;
    private LocalStream localStream;
    private String mAppId;
    private LocalStreamOptions options;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initWilddogVideo();
    }

    private void initWilddogVideo() {
        SyncReference reference = WilddogSync.getInstance().getReference();
        String path = reference.getRoot().toString();
        int startIndex = path.indexOf("https://") == 0 ? 8 : 7;
        mAppId = path.substring(startIndex, path.length() - 14);
        //初始化Video
        //WilddogVideo.initializeWilddogVideo(, mAppId);

        //获取video实例
        video = WilddogVideo.getInstance();
        //通过video获取client实例
        client = video.getClient();
        //创建本地视频流，通过video对象获取本地视频流
        LocalStreamOptions.Builder builder = new LocalStreamOptions.Builder();

        options = builder.height(240).width(320).build();

    }
    private EglBase.Context eglBaseContext;
    public class MyBinder extends Binder {
        public void setEglBaseContext(EglBase.Context context){
            eglBaseContext=context;
        }


        public LocalStream getLocalStream(){
            Log.e("MyService","getLocalStream");
            localStream = video.createLocalStream(options, new CompleteListener() {

                @Override
                public void onCompleted(VideoException s) {

                }
            });
            return localStream;
        }

        public WilddogVideoClient getVideoClient(){
            return client;
        }


    }

}
