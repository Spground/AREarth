package cn.edu.ddlut.arearth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.universalvideoview.UniversalMediaController;
import com.universalvideoview.UniversalVideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;

public class ShowActivity extends AppCompatActivity implements UniversalVideoView.VideoViewCallback {

    public static final String MAC_ADDR = "mac_addr";

    @BindView(R.id.out_tv)
    TextView show;
    @BindView(R.id.btn_adjust)
    Button btnAdjust;

    @BindView(R.id.mVideoLayout)
    View mVideoLayout;
    @BindView(R.id.videoView)
    UniversalVideoView mVideoView;
    @BindView(R.id.media_controller)
    UniversalMediaController mMediaController;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    ProgressDialog progressDialog;
    MenuItem disconnectMenuItem;
    ConnectThread thread;

    public double last_yaw = 0, last_pitch = 0;
    public boolean isAdjusted = false;


    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //连接成功
            if(msg.what == 0x001) {
                if(progressDialog != null) {
                    disconnectMenuItem.setVisible(true);
                    progressDialog.setMessage("连接成功");
                    progressDialog.show();
                    btnAdjust.setEnabled(true);//允许校准
                }
            }

            //开始接受数据
            if(msg.what == 0x002) {
                progressDialog.cancel();
                Info info = (Info) msg.obj;

                last_yaw = info.yaw;
                last_pitch = info.pitch;

                if(isAdjusted) {

                    double gtmp = (info.yaw - Global.gOffset);
                    if(gtmp > 180)//180~360
                        info.yaw = gtmp - 360;
                    else if(gtmp < -180)//-360~-180
                        info.yaw = -gtmp + 360;
                    else
                        info.yaw = gtmp;

                    double tmp = info.pitch - Global.lOffset;
                    //-lOffset ~ 360 - lOffset
                    if(tmp > 90) {
                        if(tmp <= 180)//90 < < 180
                            tmp = -(180 - tmp);
                        else if(tmp < 270) //180~270
                            tmp = -(tmp - 180);
                        else //270 - 360
                            tmp = - (360 - tmp);
                    }
                    info.pitch = -tmp;
                    Log.v("===经度值", info.yaw + "");
                    Log.v("===纬度值", info.pitch + "");
                    String str = String.format("yaw:%f pitch:%f", info.yaw, info.pitch);
                    Utile.t(ShowActivity.this, str);
                    GL2CityName(info.yaw, info.pitch);
                }
            }

            //断开连接
            if(msg.what == 0x004) {
                disconnectMenuItem.setVisible(false);
                btnAdjust.setEnabled(true);
                Utile.t(ShowActivity.this, "连接已断开");
                ShowActivity.this.finish();
            }

            //连接失败
            if(msg.what == 0x005) {
                Utile.t(ShowActivity.this, "连接失败");
                //ShowActivity.this.finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);
        btnAdjust.setEnabled(false);

        mVideoView.setMediaController(mMediaController);
        mVideoView.setVideoViewCallback(this);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Log.v("===", "onPrepared");
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.v("===", "onError " + i + "\t" + i1);
                return false;
            }
        });
        mVideoView.setFullscreen(true, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //清楚全屏标志
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mVideoView.setVideoPath("http://192.168.1.119/test.mp4");
        mVideoView.start();
        Log.v("===", "onCreate ShowActivity");

        Intent intent = getIntent();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在连接。。。");
        progressDialog.show();
        String mac_addr = intent.getStringExtra(MAC_ADDR);
        connectPeer(mac_addr);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.disconnect) {
            thread.threadHandler.sendEmptyMessage(0x1111);
        }
        if(item.getItemId() == android.R.id.home) {
            thread.threadHandler.sendEmptyMessage(0x1111);
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        mHandler.sendEmptyMessage(0x004);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        disconnectMenuItem = menu.findItem(R.id.disconnect);
        disconnectMenuItem.setVisible(false);
        return true;
    }

    @OnClick(R.id.btn_adjust)
    public void btnAdjustOnClick() {
        Global.gOffset = last_yaw;
        Global.lOffset = last_pitch;
        Log.v("===校准", String.format(" %f， %f", Global.gOffset, Global.lOffset));
        isAdjusted = true;
        btnAdjust.setEnabled(false);
    }

    @Override
    public void onScaleChange(boolean isFullscreen) {
        Log.v("===", "onScaleChange " + isFullscreen);
    }

    @Override
    public void onPause(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onStart(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onBufferingStart(MediaPlayer mediaPlayer) {
        Log.v("===", "Buffer start");
    }

    @Override
    public void onBufferingEnd(MediaPlayer mediaPlayer) {
        Log.v("===", "Buffer end");
    }

    public void GL2CityName(double g, double l) {
        //先纬度值后经度值
        Map paramsMap = new LinkedHashMap<>();
        paramsMap.put("location", l + "," + g);
        paramsMap.put("output", "json");
        paramsMap.put("ak", Utile.AK);
        String SN = Utile.calculateSN(paramsMap);
        Log.v("==SN==", SN);
        paramsMap.put("sn", SN);
        try {
            String queryString = Utile.toQueryString(paramsMap);
            Log.v("==", "qString=" + queryString);
            String url = "http://api.map.baidu.com/geocoder/v2/?" + queryString;
            Log.v("==url==", url);
            Utile.client.setURLEncodingEnabled(false);
            Utile.client.get(url, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    super.onSuccess(statusCode, headers, response);
                    Log.v("===onSuccess", response.toString());
                    try {
                        int status = response.getInt("status");
                        if(status != 0) {
                            Utile.t(ShowActivity.this, "稍后再试试。。");
                            return;
                        }
                        JSONObject result = response.getJSONObject("result");
                        JSONObject addressComponent = result.getJSONObject("addressComponent");
                        String country = addressComponent.getString("country");
                        int country_code = addressComponent.getInt("country_code");
                        String province = addressComponent.getString("province");
                        String city = addressComponent.getString("city");

                        show.setText("");
                        show.setText(String.format("国家:%s\n城市:%s", country, city));

                        mVideoView.stopPlayback();
                        mVideoView.setVideoPath("http://192.168.1.119/test.mp4");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    Log.v("===onFailed", responseString);
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void connectPeer(String macAddr) {
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddr);
        try {
            thread = new ConnectThread(bluetoothDevice, this.mHandler);
        } catch (IOException e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(0x005);
            return;
        }
        thread.start();
    }
}

class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public final int sample_frequency = 10;
    public final double sensibility = 1.0;
    Handler handler;
    public Handler threadHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            disconnect = true;
            super.handleMessage(msg);
            handler.sendEmptyMessage(0x004);
        }
    };
    public volatile boolean disconnect = false;

    public ConnectThread(BluetoothDevice device, Handler hander) throws IOException {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.handler = hander;
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            mmSocket = tmp;
        } catch (IOException e) {
            throw  e;
        }
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
            Message msg = new Message();
            msg.obj = "蓝牙连接成功";
            msg.what = 0x001;
            this.handler.sendMessage(msg);
            Log.v("===", "蓝牙连接成功");

            InputStream is = mmSocket.getInputStream();
            DataInputStream dis = new DataInputStream(is);
            String str;
            double last_yaw  = 0;
            double last_pitch = 0;

            while ((str = dis.readLine()) != null && !disconnect) {
                JSONObject jsonObject;
                double ballId;
                double yaw;
                double pitch;
                double roll;
                try {
                    jsonObject = new JSONObject(str);
                    ballId = jsonObject.getInt("BALL_ID");
                    yaw = jsonObject.getDouble("yaw");
                    pitch = jsonObject.getDouble("pitch");
                    roll = jsonObject.getDouble("roll");
                } catch (JSONException exception) {
                    continue;
                }
                //控制灵敏度
                if((Math.abs(last_pitch - pitch) > sensibility  || Math.abs(last_yaw - yaw) > sensibility)) {
                    Info info = new Info(ballId, yaw, pitch, roll);
                    Message msg0 = new Message();
                    msg0.obj = info;
                    msg0.what = 0x002;
                    this.handler.sendMessage(msg0);
                    Log.v("===", str);
                }
                last_pitch = pitch;
                last_yaw = yaw;
                Thread.sleep((long)(1 / (sample_frequency * 1.0) * 1000));
            }
        } catch (IOException connectException) {
            this.handler.sendEmptyMessage(0x005);
            return;
            // Unable to connect; close the socket and get out
        } catch (InterruptedException e) {
            e.printStackTrace();
            this.handler.sendEmptyMessage(0x005);
            return;
        }

        //断开连接
        try {
            mmSocket.close();
            if(disconnect) {
                this.handler.sendEmptyMessage(0x004);
            }
        } catch (IOException closeException) {
            this.handler.sendEmptyMessage(0x004);
        }
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



