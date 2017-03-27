package com.wilddog.video.meetingcast.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wilddog.client.SyncReference;
import com.wilddog.client.WilddogSync;

import com.wilddog.video.Conference;
import com.wilddog.video.LocalStream;
import com.wilddog.video.Participant;
import com.wilddog.video.RemoteStream;
import com.wilddog.video.WilddogVideo;
import com.wilddog.video.WilddogVideoClient;
import com.wilddog.video.WilddogVideoView;
import com.wilddog.video.bean.ConnectOptions;
import com.wilddog.video.bean.LocalStreamOptions;
import com.wilddog.video.bean.VideoException;
import com.wilddog.video.bean.VideoExceptionCode;
import com.wilddog.video.listener.CompleteListener;
import com.wilddog.video.listener.MeetingCastStateListener;
import com.wilddog.video.meetingcast.R;
import com.wilddog.video.meetingcast.util.AdjustNullTool;
import com.wilddog.video.meetingcast.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class ConferenceActivity extends AppCompatActivity {

    public static final String TAG = "M2M";
    private Conference mConference;
    private Conference.MeetingCast mMeetingCast;

    private boolean isMeetingCastStart=false;

    boolean isAudioEnable = false;
    @BindView(R.id.tv_cid)
    TextView tvConferenceId;
    @BindView(R.id.btn_operation_mic)
    Button btnMic;
    @BindView(R.id.btn_operation_video)
    Button btnVideo;
    @BindView(R.id.btn_operation_hangup)
    Button btnHangup;



    @BindView(R.id.local_video_view)
    WilddogVideoView local_video_view;
    @BindView(R.id.remote_video_view)
    WilddogVideoView remote_video_view;

    @BindView(R.id.vitamio_videoView)
    VideoView mVideoView;

    private WilddogVideo video;
    private WilddogVideoClient client;

    private LocalStream localStream;
    private String conferenceId;

    private String participant;
    private Map currentUrlMap = new HashMap();

    private String currentLiveId="";

    private boolean isMeetingCastFinished=false;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    String path =currentUrlMap.get("rtmp").toString();
                    play(path);
                    break;
                case 2:
                    stop();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams
                .FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View
                .SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_conference);



        //初始化控件
        ButterKnife.bind(this);
        //获取当前会议 ID
        conferenceId = getIntent().getStringExtra("conferenceId");
        tvConferenceId.setText(conferenceId);

        SyncReference reference = WilddogSync.getInstance().getReference();
        String path = reference.getRoot().toString();
        int startIndex = path.indexOf("https://") == 0 ? 8 : 7;
        String appid = path.substring(startIndex, path.length() - 14);
        //初始化WilddogVideoView
        initVideoRender();

        //初始化Video
        WilddogVideo.initializeWilddogVideo(getApplicationContext(), appid);
        //获取video实例
        video = WilddogVideo.getInstance();
        //通过video获取client实例
        client = video.getClient();
        //配置本地媒体流参数
        LocalStreamOptions.Builder builder = new LocalStreamOptions.Builder();
        LocalStreamOptions options = builder.height(240).width(320).build();
        //创建本地媒体流，通过video对象获取本地视频流
        localStream = video.createLocalStream(options, new CompleteListener() {
            @Override
            public void onCompleted(VideoException s) {
                if (s != null) {
                    LogUtil.e(TAG, "createLocalStream failured ! the detail :" + s.getMessage());
                }
            }
        });
        localStream.enableAudio(isAudioEnable);
        //将本地媒体流绑定到WilddogVideoView中
        localStream.attach(local_video_view);
        //加入会议 ID 为conferenceId的会议
        inviteToConference(conferenceId);
        initMeetingCast();
    }

    private void initVideoRender() {
        //初始化本地媒体流展示控件

        local_video_view.setZOrderMediaOverlay(true);
        //本地媒体流设置镜像
        local_video_view.setMirror(true);


    }


    private void inviteToConference(String conferenceId) {

        //创建 ConnectOptions 对象，此对象包含邀请所需要的参数
        ConnectOptions options = new ConnectOptions(localStream, "chaih");

        mConference = client.connectToConference(conferenceId, options, new Conference.Listener() {
            @Override
            public void onConnected(Conference conference) {

                LogUtil.e(TAG, "onConnected:");
                LogUtil.e(TAG, AdjustNullTool.adjustConference(conference));
            }

            @Override
            public void onConnectFailed(Conference conference, VideoException exception) {
                LogUtil.e(TAG, "onConnectFailed:" + exception.getMessage());
                if(exception.getErrorCode()== VideoExceptionCode.VIDEO_CLIENT_REGISTRATION_FAILED && exception.getMessage().equals("VIDEO_CLIENT_REGISTRATION_FAILED:App is stopped for resource limit")){
                    Toast.makeText(ConferenceActivity.this,"video功能未开启或者已停止服务",Toast.LENGTH_SHORT).show();
                }
                LogUtil.e(TAG, AdjustNullTool.adjustConference(conference));
            }

            @Override
            public void onDisconnected(Conference conference, VideoException exception) {
                LogUtil.e(TAG, "onDisconnected:");
                LogUtil.e(TAG, AdjustNullTool.adjustConference(conference));
                if (exception != null) {
                    LogUtil.e(TAG, "onDisconnected failured" + exception.getMessage());
                }
            }

            @Override
            public void onParticipantConnected(Conference conference, final Participant participant) {
                LogUtil.e(TAG, "onParticipantConnected:");
                LogUtil.e(TAG, AdjustNullTool.adjustConference(conference));
                LogUtil.e(TAG, AdjustNullTool.adjustParticipant(participant));
                participant.setListener(new Participant.Listener() {
                    @Override
                    public void onStreamAdded(RemoteStream remoteStream) {
                        LogUtil.e(TAG, "onStreamAdded:");
                        if (remote_video_view != null) {
                            remote_video_view.setZOrderMediaOverlay(true);
                            remote_video_view.setMirror(true);
                            remoteStream.enableAudio(true);
                            remoteStream.enableVideo(true);
                            remoteStream.attach(remote_video_view);
                        }
                    }

                    @Override
                    public void onConnectFailed(Participant participant, VideoException e) {
                        LogUtil.e(TAG, AdjustNullTool.adjustParticipant(participant));
                        LogUtil.e(TAG, "onConnectFailed");
                        if (e != null) {
                            LogUtil.e(TAG, "onConnectFailed" + e.getMessage());
                        }
                        // TODO: 16-12-16 handle onConnectFailed events
                    }

                    @Override
                    public void onDisconnected(Participant participant, VideoException e) {
                        LogUtil.e(TAG, "onDisconnected");
                        LogUtil.e(TAG, AdjustNullTool.adjustParticipant(participant));
                        if (e != null) {
                            LogUtil.e(TAG, "onDisconnected" + e.getMessage());
                        }
                        // TODO: 16-12-16 handle onDisconnected events
                    }
                });
            }

            @Override
            public void onParticipantDisconnected(Conference conference, Participant participant) {
                LogUtil.e(TAG, "onParticipantDisconnected:");
                LogUtil.e(TAG, AdjustNullTool.adjustConference(conference));
                LogUtil.e(TAG, AdjustNullTool.adjustParticipant(participant));
                participant.getRemoteStream().detach();
            }
        });
    }


    @OnClick(R.id.btn_flip_camera)
    public void flipCamera() {
        //切换摄像头
        video.flipCamera();
    }

    private ArrayList<String> getPartiList() {
        ArrayList<String> meeting = new ArrayList<>();
        meeting.clear();
        Set<Participant> partiSet = mConference.getParticipants();
        if (partiSet.size() > 0) {
            Iterator<Participant> iterator = partiSet.iterator();
            while (iterator.hasNext()) {
                Participant participant = iterator.next();
                meeting.add(participant.getParticipantId());
            }
        }
        // 将自己加入
        if (localStream != null && mConference.getLocalParticipant() != null) {
            meeting.add(mConference.getLocalParticipant().getParticipantId());
        }
        return meeting;
    }

    @OnClick(R.id.btn_share_meetingcast_url)
    public void shareMeetingCastUrl() {

        if (getPartiList() == null || getPartiList().size() == 0) {
            Toast.makeText(ConferenceActivity.this, "当前会议用户为空，无法分享", Toast.LENGTH_SHORT).show();
            return;
        }

        // 选择要分享直播会议的人的ID
        Intent intent = new Intent(ConferenceActivity.this, MeetingCastUserListActivity.class);

        ArrayList<String> list=getPartiList();
        intent.putStringArrayListExtra("list",list );
        startActivityForResult(intent,1);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            participant = data.getStringExtra("participant");
            Log.e(TAG, "分享的participantId:"+participant);
            if(isMeetingCastStart){
                switchMeetingCast(participant);
            }else {
            startMeetingCast(participant);}
        }
    }



    private void initMeetingCast() {
        if (meetingCastStateListener == null) {
            meetingCastStateListener = new MeetingCastStateListener() {
                @Override
                public void onMeetingCastStateChanged(Conference.MeetingCastStatus status, String participantId, Map<String, String> urlMap) {
                    if (status==Conference.MeetingCastStatus.ON) {
                        // 直播中做的逻辑
                        currentLiveId =participantId;
                        isMeetingCastStart =true;
                        LogUtil.e(TAG, urlMap.toString());
                        LogUtil.e(TAG, status+"");
                        currentUrlMap = urlMap;
                        isMeetingCastFinished =true;
                        handler.sendEmptyMessage(1);
                        LogUtil.e(TAG, participantId);
                    } else {
                        //直播结束做的逻辑
                        isMeetingCastStart =false;
                        LogUtil.e(TAG, status+"");
                        LogUtil.e(TAG, urlMap.toString());
                        isMeetingCastFinished = false;
                        handler.sendEmptyMessage(2);
                        currentUrlMap=null;
                        LogUtil.e(TAG, participantId);
                    }
                }
            };
        }

        if (mMeetingCast == null) {
            mMeetingCast = mConference.getMeetingCast(meetingCastStateListener);
        }

    }

    private MeetingCastStateListener meetingCastStateListener;




    private void startMeetingCast(final String participantId) {
        if (mMeetingCast != null) {
            mMeetingCast.start(participantId, new CompleteListener() {
                @Override
                public void onCompleted(VideoException exception) {
                    if (exception != null) {
                        Log.e(TAG, exception.getMessage());
                    } else {
                        Log.e(TAG, "mMeetingCast start success!" + "participantId:" + participantId);
                    }
                }
            });
        }
    }
    private void switchMeetingCast(final String participantId) {
        if (mMeetingCast != null) {
            mMeetingCast.switchParticipant(participantId, new CompleteListener() {
                @Override
                public void onCompleted(VideoException exception) {
                    if (exception != null) {
                        Log.e(TAG, exception.getMessage());
                    } else {
                        Log.e(TAG, "mMeetingCast start success!" + "participantId:" + participantId);
                    }
                }
            });
        }
    }

    private void stopMeetingCastAndViewInvisible() {
        if (mMeetingCast != null) {
            mMeetingCast.stop(new CompleteListener() {
                @Override
                public void onCompleted(VideoException exception) {
                    if(exception!=null){
                        Toast.makeText(ConferenceActivity.this,exception.getMessage(), Toast.LENGTH_SHORT).show();
                        LogUtil.e(TAG,"异常信息为："+exception.getMessage());
                    }else {
                        Toast.makeText(ConferenceActivity.this,"停止直播成功", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    private void stop(){
        mVideoView.stopPlayback();
    }


    private void play(String path){
        if (!LibsChecker.checkVitamioLibs(this)){
            Log.e(TAG,"checkVitamioLibs :: state : false");
            return;}
        mVideoView.setVideoLayout(0,0.0f);
        mVideoView.setVideoPath(path);
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.requestFocus();
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setPlaybackSpeed(1.0f);
                mVideoView.start();
            }
        });
    }

   /* @OnClick(R.id.btn_open_rtmp_stream)
    public void openRtmpStream() {
        if(mMeetingCast==null){
            return;
        }
        if(!isMeetingCastFinished){
            Toast.makeText(ConferenceActivity.this, "当前直播初始化未完成,稍后开启", Toast.LENGTH_SHORT).show();
            return;}
        if (TextUtils.isEmpty(currentUrlMap.get("rtmp").toString())) {
            Toast.makeText(ConferenceActivity.this, "当前直播地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        String path =currentUrlMap.get("rtmp").toString();
        play(path);
    }*/

    @OnClick(R.id.btn_stop_meetingCast)
    public void stopMeetingCast() {
        if(mMeetingCast==null){
            return;
        }
        if (!TextUtils.isEmpty(participant)) {
            stopMeetingCastAndViewInvisible();
        }
    }


    @OnClick(R.id.btn_operation_mic)
    public void micClick() {
        //关闭/启用本地流音频，此操作影响所有人收到的音频流
        isAudioEnable = !isAudioEnable;
        localStream.enableAudio(isAudioEnable);
    }

    boolean isVideoEnable = true;

    @OnClick(R.id.btn_operation_video)
    public void videoClick() {
        // 关闭/启用本地流视频
       isVideoEnable = !isVideoEnable;
        localStream.enableVideo(isVideoEnable);

    }

    //挂断会议
    @OnClick(R.id.btn_operation_hangup)
    public void hangupClick() {
        //挂断
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放持有的资源
        LogUtil.e(TAG,"onDestroy");
        if(localStream!=null){
        localStream.detach();
        localStream.close();}

        if (local_video_view != null) {
            local_video_view.release();
            local_video_view = null;
        }
        if (mConference != null) {
            mConference.disconnect();
        }
        if(client!=null){
        client.dispose();}
        if(video!=null){
        video.dispose();}
    }

}
