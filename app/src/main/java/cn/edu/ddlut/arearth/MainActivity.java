package cn.edu.ddlut.arearth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.annotation.ThreadSafe;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @BindView(R.id.btn_scan)
    Button btnScan;
    @BindView(R.id.peer_list)
    ListView peerList;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    ArrayAdapter<String> mArrayAdapter;
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

    ProgressDialog progressDialog;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.v("========", device.getName() + "\n" + device.getAddress());
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, android.R.id.text1);
        peerList.setAdapter(mArrayAdapter);
        peerList.setOnItemClickListener(this);
    }

    @OnClick(R.id.btn_scan)
    public void btnScanOnClick() {
        btnScan.setEnabled(false);
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Utile.t(this, "设备不支持蓝牙！");
            btnScan.setEnabled(true);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 2);
            btnScan.setEnabled(true);
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(false);
        progressDialog.setMessage("扫描ing...");
        progressDialog.show();

        discoverPeer();
        btnScan.setEnabled(true);
    }

    public void discoverPeer() {
        // Register the BroadcastReceiver
        mArrayAdapter.clear();
        mArrayAdapter.notifyDataSetChanged();
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        bluetoothAdapter.cancelDiscovery();
        String macStr = ((TextView)view).getText().toString().split("\\n")[1].trim();
        Utile.t(this, macStr);
        Intent intent = new Intent(this, ShowActivity.class);
        intent.putExtra(ShowActivity.MAC_ADDR, macStr);
        startActivity(intent);
    }
}

