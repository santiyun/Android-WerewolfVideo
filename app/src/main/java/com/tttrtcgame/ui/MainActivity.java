package com.tttrtcgame.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtcgame.LocalConfig;
import com.tttrtcgame.LocalConstans;
import com.tttrtcgame.MainApplication;
import com.tttrtcgame.R;
import com.tttrtcgame.bean.EnterUserInfo;
import com.tttrtcgame.bean.JniObjs;
import com.tttrtcgame.bean.VideoViewObj;
import com.tttrtcgame.callback.MyTTTRtcEngineEventHandler;
import com.tttrtcgame.callback.PhoneListener;
import com.tttrtcgame.dialog.ExitRoomDialog;
import com.tttrtcgame.utils.MyLog;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngineForGamming;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import java.util.ArrayList;
import java.util.List;

import static com.tttrtcgame.LocalConstans.CALL_BACK_ON_AUDIO_VOLUME_INDICATION;
import static com.tttrtcgame.LocalConstans.CALL_BACK_ON_ERROR;
import static com.tttrtcgame.LocalConstans.CALL_BACK_ON_USER_JOIN;
import static com.tttrtcgame.LocalConstans.CALL_BACK_ON_USER_OFFLINE;


public class MainActivity extends BaseActivity {

    private static final int AUTHOR_MAX_NUM = 12;
    private static final int VOLUME_MAX_NUM = 9;

    public static final int MSG_TYPE_ERROR_ENTER_ROOM = 0;
    public static final int DISCONNECT = 100;
    private static final int CONTROL_VOICE_SPEAK = 13;
    private static final int CONTROL_VOICE_IMAGE_VISIBILE = 14;

    private TTTRtcEngineForGamming mTTTEngine;

    private ArrayList<VideoViewObj> mLocalSeiList;
    private List<EnterUserInfo> listData;

    private Handler mHandler;
    private ExitRoomDialog mExitRoomDialog;

    private TextView mSpeakingTV;
    private MyVideoLayout mVideoParentLy;

    private MyLocalBroadcastReceiver mLocalBroadcast;
    private VideoViewObj mLocalUserViewObj;
    private boolean mIsExitRoom;
    private boolean mIsSpeaking;
    private boolean mIsPhoneComing;
    private boolean mIsOwnerOpend;

    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;
    protected boolean mIsStop;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainly);
        mTTTEngine = TTTRtcEngineForGamming.getInstance();
        initView();
        initData();
        initBroadcast();
        EnterUserInfo mLocalUserInfo = new EnterUserInfo(LocalConfig.mLoginUserID, Constants.CLIENT_ROLE_AUDIENCE);
        adJustRemoteViewDisplay(true, mLocalUserInfo);

        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        mTTTEngine.enableAudioVolumeIndication(300, 3);
        MyLog.d("MainActivity onCreate... ");
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < mLocalSeiList.size(); i++) {
            VideoViewObj videoViewObj = mLocalSeiList.get(i);
            videoViewObj.clear();
        }
        mHandler.removeCallbacksAndMessages(null);
        unregisterReceiver(mLocalBroadcast);
        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }

        mVideoParentLy.clear();
        mVideoParentLy = null;
        MyLog.d("MainActivity onDestroy... ");
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyLog.d("MainActivity onStart : " + mIsSpeaking);
        mIsStop = false;
        mTTTEngine.muteLocalVideoStream(false);
        mTTTEngine.muteAllRemoteAudioStreams(false);
        if (mIsSpeaking) {
            mTTTEngine.muteLocalAudioStream(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyLog.d("MainActivity onStop : " + mIsSpeaking);
        mIsStop = true;
        mTTTEngine.muteLocalVideoStream(true);
        mTTTEngine.muteAllRemoteAudioStreams(true);
        if (mIsSpeaking) {
            mTTTEngine.muteLocalAudioStream(true);
        }
    }

    private void initView() {
        mSpeakingTV = findViewById(R.id.mainly_speaking);
        mSpeakingTV.setClickable(true);
        mVideoParentLy = findViewById(R.id.mainly_user_videoly);
        mVideoParentLy.setActivityCallBack(() -> {
            if (!mIsOwnerOpend) {
                openLocalVideo();
            }
            openRemoteUserVideo();
        });
        mLocalSeiList = new ArrayList<>();
        for (int i = 1; i <= AUTHOR_MAX_NUM; i++) {
            VideoViewObj obj = new VideoViewObj();
            obj.mIndex = i;
            switch (i) {
                case 1:
                    obj.mRootBG = findViewById(R.id.mainly_user_left1);
                    break;
                case 2:
                    obj.mRootBG = findViewById(R.id.mainly_user_left2);
                    break;
                case 3:
                    obj.mRootBG = findViewById(R.id.mainly_user_left3);
                    break;
                case 4:
                    obj.mRootBG = findViewById(R.id.mainly_user_left4);
                    break;
                case 5:
                    obj.mRootBG = findViewById(R.id.mainly_user_left5);
                    break;
                case 6:
                    obj.mRootBG = findViewById(R.id.mainly_user_left6);
                    break;
                case 7:
                    obj.mRootBG = findViewById(R.id.mainly_user_right1);
                    break;
                case 8:
                    obj.mRootBG = findViewById(R.id.mainly_user_right2);
                    break;
                case 9:
                    obj.mRootBG = findViewById(R.id.mainly_user_right3);
                    break;
                case 10:
                    obj.mRootBG = findViewById(R.id.mainly_user_right4);
                    break;
                case 11:
                    obj.mRootBG = findViewById(R.id.mainly_user_right5);
                    break;
                case 12:
                    obj.mRootBG = findViewById(R.id.mainly_user_right6);
                    break;
            }

            obj.mSpeakImage = obj.mRootBG.findViewById(R.id.userly_audio_icon);
            obj.mRemoteUserIcon = obj.mRootBG.findViewById(R.id.userly_icon);
            obj.mRemoteUserIndex = obj.mRootBG.findViewById(R.id.userly_count);
            obj.mRemoteUserID = obj.mRootBG.findViewById(R.id.userly_user_id);
            obj.mLocalNameFlag = obj.mRootBG.findViewById(R.id.userly_local_user_name);
            mLocalSeiList.add(obj);
        }

        findViewById(R.id.main_btn_exit).setOnClickListener(v -> mExitRoomDialog.show());
    }

    private void initBroadcast() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        ((MainApplication) getApplicationContext()).mMyTTTRtcEngineEventHandler.setIsSaveCallBack(false);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        listData = new ArrayList<>();
        if (mHandler == null) {
            mHandler = new LocalHandler();
        }

        mSpeakingTV.setOnClickListener(v -> {
            if (mIsSpeaking) {
                Message.obtain(mHandler, CONTROL_VOICE_SPEAK, false).sendToTarget();
            } else {
                Message.obtain(mHandler, CONTROL_VOICE_SPEAK, true).sendToTarget();
            }
        });

        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(v -> {
            exitRoom();
            mExitRoomDialog.dismiss();
        });

        mExitRoomDialog.mDenyBT.setOnClickListener(v -> mExitRoomDialog.dismiss());
    }

    private void addListData(EnterUserInfo info) {
        boolean bupdate = false;
        for (int i = 0; i < listData.size(); i++) {
            EnterUserInfo info1 = listData.get(i);
            if (info1.getId() == info.getId()) {
                listData.set(i, info);
                bupdate = true;
                break;
            }
        }
        if (!bupdate) {
            listData.add(info);
        }
    }

    private EnterUserInfo removeListData(long uid) {
        int index = -1;
        for (int i = 0; i < listData.size(); i++) {
            EnterUserInfo info1 = listData.get(i);
            if (info1.getId() == uid) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            return listData.remove(index);
        }
        return null;
    }

    /**
     * Author: wangzg <br/>
     * Time: 2017-6-6 16:44:36<br/>
     * Description: 调整远端小窗口的显示与隐藏
     *
     * @param isVisibile the is visibile
     * @param info       the info
     */
    private synchronized void adJustRemoteViewDisplay(boolean isVisibile, EnterUserInfo info) {
        MyLog.d("adJustRemoteViewDisplay isVisibile : " + isVisibile +
                " | User ID : " + info.getId());
        if (isVisibile) {
            boolean checkRes = checkVideoExist(info.getId());
            if (checkRes) {
                MyLog.d("该用户已经存在... ");
                return;
            }

            long id = info.getId();
            VideoViewObj obj = getRemoteViewParentLayout();
            if (obj != null) {
                if (id == LocalConfig.mLoginUserID) {
                    mLocalUserViewObj = obj;
                    obj.mLocalNameFlag.setVisibility(View.VISIBLE);
                    openLocalVideo();
                }
                obj.mBindUid = id;
                obj.mRemoteUserIndex.setText(String.valueOf(obj.mIndex));
                obj.mRemoteUserIndex.setVisibility(View.VISIBLE);
                obj.mRemoteUserID.setText(String.valueOf(id));
                setUserIcon(obj.mRemoteUserIcon, obj.mBindUid);
                MyLog.d("openUserVideo 有用户进来，开始打开下一路视频");
                openUserVideo();
            } else {
                MyLog.d("getRemoteViewParentLayout 失败... ");
            }
        } else {
            VideoViewObj videoCusSei;
            for (int i = 0; i < mLocalSeiList.size(); i++) {
                videoCusSei = mLocalSeiList.get(i);
                if (videoCusSei.mBindUid == info.getId()) {
                    videoCusSei.mIsUsing = false;
                    videoCusSei.mVideoOpend = false;
                    videoCusSei.mBindUid = 0;
                    videoCusSei.mRemoteUserIndex.setText("");
                    videoCusSei.mRemoteUserID.setText("");
                    videoCusSei.mLocalNameFlag.setVisibility(View.GONE);
                    videoCusSei.mRemoteUserIcon.setImageResource(R.drawable.touxiangmoren);
                    videoCusSei.mSpeakImage.setVisibility(View.INVISIBLE);
                    videoCusSei.mRemoteUserIndex.setVisibility(View.INVISIBLE);
                    mVideoParentLy.removeVideoChild(info.getId());
                    MyLog.d("openUserVideo 有用户离开，开始打开下一路视频");
                    openUserVideo();
                    break;
                }
            }
        }
    }

    private synchronized void openLocalVideo() {
        FrameLayout videoChildLayout = mVideoParentLy.getVideoChildLayout(LocalConfig.mLoginUserID);
        if (videoChildLayout != null) {
            SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(mContext);
            int result = mTTTEngine.setupLocalVideo(new VideoCanvas(LocalConfig.mLoginUserID, Constants.
                    RENDER_MODE_HIDDEN, mSurfaceView), getRequestedOrientation());
            videoChildLayout.addView(mSurfaceView);
            mIsOwnerOpend = true;
        }
    }

    private synchronized boolean openUserVideo() {
        if (mVideoParentLy.mShowingDevices.size() < MyVideoLayout.MAX_VIDEO_NUM) {
            for (int i = 0; i < mLocalSeiList.size(); i++) {
                VideoViewObj videoViewObj = mLocalSeiList.get(i);
                if (videoViewObj.mBindUid != LocalConfig.mLoginUserID && videoViewObj.mBindUid != 0) {
                    if (videoViewObj.mIsUsing) {
                        if (!videoViewObj.mVideoOpend) {
                            FrameLayout videoChildLayout = mVideoParentLy.getVideoChildLayout(videoViewObj.mBindUid);
                            if (videoChildLayout != null) {
                                SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(mContext);
                                int result = mTTTEngine.setupRemoteVideo(new VideoCanvas(videoViewObj.mBindUid, Constants.
                                        RENDER_MODE_HIDDEN, mSurfaceView));
                                videoChildLayout.addView(mSurfaceView);
                                videoViewObj.mVideoOpend = true;
                                MyLog.d("openUserVideo open remote user id : " + videoViewObj.mBindUid
                                        + " | result : " + result + " | layout index : " + videoChildLayout.getId());
                                return true;
                            } else {
                                MyLog.d("openUserVideo 该用户的视频已经被打开!" + videoViewObj.mBindUid);
                            }
                        } else {
                            MyLog.d("openUserVideo 该videoViewObj并没有人使用!");
                        }
                    } else {
                        MyLog.d("openUserVideo videoViewObj的uid为0!");
                    }
                } else {
                    MyLog.d("openUserVideo open remote user failed! id : " + videoViewObj.mBindUid);
                }
            }
        } else {
            MyLog.d("openUserVideo 当前视频已经显示够满4路，不能再打开视频了!");
        }
        return false;
    }

    private void openRemoteUserVideo() {
        boolean b = openUserVideo();
        if (b) {
            openRemoteUserVideo();
        }
    }

    /**
     * Author: wangzg <br/>
     * Time: 2017-6-6 16:45:00<br/>
     * Description: 创建一个新的远端小视频的布局窗口
     *
     * @return the list
     */
    private VideoViewObj getRemoteViewParentLayout() {
        for (int i = 0; i < mLocalSeiList.size(); i++) {
            VideoViewObj videoCusSei = mLocalSeiList.get(i);
            if (!videoCusSei.mIsUsing) {
                videoCusSei.mIsUsing = true;
                return videoCusSei;
            }
        }
        return null;
    }

    private void setUserIcon(ImageView mView, long userID) {
        char[] chars = String.valueOf(userID).toCharArray();
        String end = String.valueOf(chars[chars.length - 1]);
        switch (Integer.valueOf(end)) {
            case 0:
                mView.setImageResource(R.drawable.touxiang1);
                break;
            case 1:
                mView.setImageResource(R.drawable.touxiang2);
                break;
            case 2:
                mView.setImageResource(R.drawable.touxiang3);
                break;
            case 3:
                mView.setImageResource(R.drawable.touxiang4);
                break;
            case 4:
                mView.setImageResource(R.drawable.touxiang5);
                break;
            case 5:
                mView.setImageResource(R.drawable.touxiang6);
                break;
            case 6:
                mView.setImageResource(R.drawable.touxiang7);
                break;
            case 7:
                mView.setImageResource(R.drawable.touxiang8);
                break;
            case 8:
                mView.setImageResource(R.drawable.touxiang9);
                break;
            case 9:
                mView.setImageResource(R.drawable.touxiang10);
                break;
        }
    }

    private boolean checkVideoExist(long uid) {
        for (int i = 0; i < mLocalSeiList.size(); i++) {
            VideoViewObj videoCusSei = mLocalSeiList.get(i);
            if (videoCusSei.mIsUsing && videoCusSei.mBindUid == uid) {
                return true;
            }
        }
        return false;
    }

    private void exitRoom() {
        mTTTEngine.leaveChannel();
        finish();
    }

    private AlertDialog exit;

    private class LocalHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TYPE_ERROR_ENTER_ROOM:
                    String message = "";
                    int errorType = (int) msg.obj;
                    if (errorType == Constants.ERROR_KICK_BY_HOST) {
                        mIsExitRoom = true;
                        message = getResources().getString(R.string.ERROR_KICK_BY_HOST);
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                        mIsExitRoom = true;
                        message = getResources().getString(R.string.ERROR_KICK_BY_PUSHRTMPFAILED);
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                        mIsExitRoom = true;
                        message = getResources().getString(R.string.ERROR_KICK_BY_SERVEROVERLOAD);
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                        mIsExitRoom = true;
                        message = getResources().getString(R.string.ERROR_KICK_BY_MASTER_EXIT);
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                        mIsExitRoom = true;
                        message = getResources().getString(R.string.ERROR_KICK_BY_RELOGIN);
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                        mIsExitRoom = true;
                        message = getResources().getString(R.string.ERROR_KICK_BY_NEWCHAIRENTER);
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                        mIsExitRoom = true;
                        message = getResources().getString(R.string.ERROR_KICK_BY_NOAUDIODATA);
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                        mIsExitRoom = true;
                        message = getResources().getString(R.string.ERROR_KICK_BY_NOVIDEODATA);
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    } else if (errorType == DISCONNECT) {
                        if (!mIsExitRoom) {
                            message = getResources().getString(R.string.DISCONNECT);
                            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (exit == null) {
                        //添加确定按钮
                        exit = new AlertDialog.Builder(MainActivity.this).setTitle(getResources().getString(R.string.exitroom_dialog_title))//设置对话框标题
                                .setMessage(getResources().getString(R.string.exitroom_dialog_user) + LocalConfig.mLoginUserID + getResources().getString(R.string.exitroom_dialog_reason) + message)//设置显示的内容
                                .setCancelable(false)
                                .setPositiveButton(getResources().getString(R.string.exitroom_dialog_sure), (dialog, which) -> {//确定按钮的响应事件
                                    exitRoom();
                                }).show();//在按键响应事件中显示此对话框
                    }
                    break;
                case CONTROL_VOICE_SPEAK:
                    boolean isSpeak = (boolean) msg.obj;
                    mTTTEngine.muteLocalAudioStream(!isSpeak);
                    if (isSpeak) {
                        mSpeakingTV.setText(getResources().getString(R.string.main_end));
                        mSpeakingTV.setBackgroundResource(R.drawable.mainly_btn_speaking_bg_press);
                        mIsSpeaking = true;
                    } else {
                        if (mLocalUserViewObj != null && mLocalUserViewObj.mSpeakImage != null) {
                            mLocalUserViewObj.mSpeakImage.setVisibility(View.INVISIBLE);
                        }

                        mSpeakingTV.setText(getResources().getString(R.string.main_start));
                        mSpeakingTV.setBackgroundResource(R.drawable.mainly_btn_speaking_bg);
                        mIsSpeaking = false;
                    }
                    break;
                case CONTROL_VOICE_IMAGE_VISIBILE:
                    VideoViewObj objs = (VideoViewObj) msg.obj;
                    objs.mSpeakImage.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MyTTTRtcEngineEventHandler.TAG)) {
                JniObjs mJniObjs = (JniObjs) intent.getSerializableExtra(
                        MyTTTRtcEngineEventHandler.MSG_TAG);
                Object[] objs = mJniObjs.mObjs;
                switch (mJniObjs.mJniType) {
                    case CALL_BACK_ON_ERROR:
                        Message.obtain(mHandler, MSG_TYPE_ERROR_ENTER_ROOM, objs[0]).sendToTarget();
                        break;
                    case CALL_BACK_ON_USER_JOIN:
                        long uid = (long) objs[0];
                        int identity = (int) objs[1];
                        if (listData.size() >= AUTHOR_MAX_NUM) {
                            mTTTEngine.kickChannelUser(uid);
                            return;
                        }

                        MyLog.d("CALL_BACK_ON_USER_JOIN user id : " + uid);
                        EnterUserInfo userInfo = new EnterUserInfo(uid, identity);
                        addListData(userInfo);
                        adJustRemoteViewDisplay(true, userInfo);
                        break;
                    case CALL_BACK_ON_USER_OFFLINE:
                        long offLineUserID = (long) objs[0];
                        MyLog.d("openUserVideo CALL_BACK_ON_USER_OFFLINE user id : " + offLineUserID);
                        EnterUserInfo enterUserInfo = removeListData(offLineUserID);
                        if (enterUserInfo != null) {
                            adJustRemoteViewDisplay(false, enterUserInfo);
                        } else {
                            MyLog.d("CALL_BACK_ON_USER_OFFLINE 没有从用户列表中找到该用户 id : " + offLineUserID);
                        }
                        break;
                    case CALL_BACK_ON_AUDIO_VOLUME_INDICATION:
                        long volumeUserID = (long) objs[0];
                        int volumeLevel = (int) objs[1];
                        for (final VideoViewObj obj : mLocalSeiList) {
                            if (obj.mBindUid == volumeUserID && obj.mIsUsing && obj.mSpeakImage != null) {
                                boolean isEnd = false;
                                if (volumeUserID == LocalConfig.mLoginUserID && !mIsSpeaking) {
                                    isEnd = true;
                                }

                                if (isEnd) {
                                    break;
                                }

                                if (volumeLevel > 0 && volumeLevel <= 3) {
                                    obj.mSpeakImage.setImageResource(R.drawable.xiao);
                                    adJustImageSpeak(obj);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    obj.mSpeakImage.setImageResource(R.drawable.zhong);
                                    adJustImageSpeak(obj);
                                } else if (volumeLevel > 6 && volumeLevel <= VOLUME_MAX_NUM) {
                                    obj.mSpeakImage.setImageResource(R.drawable.da);
                                    adJustImageSpeak(obj);
                                }
                                break;
                            }
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_COME:
                        MyLog.d("PhoneListener CALL_BACK_ON_PHONE_LISTENER_COME mIsSpeaking: " + mIsSpeaking
                                + " | mIsPhoneComing : " + mIsPhoneComing + " | mIsStop : " + mIsStop);
                        mTTTEngine.setEnableSpeakerphone(false);
                        mIsPhoneComing = true;
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:
                        MyLog.d("PhoneListener CALL_BACK_ON_PHONE_LISTENER_IDLE mIsSpeaking: " + mIsSpeaking
                                + " | mIsPhoneComing : " + mIsPhoneComing + " | mIsStop : " + mIsStop);
                        if (mIsPhoneComing) {
                            mTTTEngine.setEnableSpeakerphone(true);
                            mIsPhoneComing = false;
                        }
                        break;
                }
            }
        }
    }

    private void adJustImageSpeak(VideoViewObj obj) {
        mHandler.removeMessages(CONTROL_VOICE_IMAGE_VISIBILE, obj);
        obj.mSpeakImage.setVisibility(View.VISIBLE);
        Message obtain = Message.obtain();
        obtain.what = CONTROL_VOICE_IMAGE_VISIBILE;
        obtain.obj = obj;
        mHandler.sendMessageDelayed(obtain, 1500);
    }
}
