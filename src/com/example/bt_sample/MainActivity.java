package com.example.bt_sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
 
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;
 
public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    /** BLE 機器スキャンタイムアウト (ミリ秒) */
    private static final long SCAN_PERIOD = 10000;
 
    private static final String TAG = "BLESample";
//    protected static final int REQUEST_ENABLE_BLUETOOTH = 0;
    private static BleStatus mStatus = BleStatus.DISCONNECTED;
    private static Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private static BluetoothGatt mBluetoothGatt;

    private TextView mStatusText;
    
    private BluetoothGattCallback mBluetoothGattCallback;
    private Context mContext;
    private ArrayList<String> scanList;
    private static ProgressDialog waitDialog;
    private Thread thread;
    
    public static BluetoothGatt getBluetoothGatt() {
      return mBluetoothGatt;
    }

    public static void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
      mBluetoothGatt = bluetoothGatt;
    }
    
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//         Log.i(TAG, "onActivityResult");
//         if (resultCode == RESULT_OK) {
//              if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
//                   Log.d(TAG, "success");
//                   connect();
//              }
//         }
//    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        setContentView(R.layout.activity_main);
 
        mBluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//                  Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                  startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
//              }else {
//                connect();
//              }
              connect();
            }
        });
        findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });
 
        mStatusText = (TextView)findViewById(R.id.text_status);
 
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mStatusText.setText(((BleStatus) msg.obj).name());
            }
        };
    }
 
    /** BLE機器を検索する */
    private void connect() {
      waitDialog = new ProgressDialog(this);
      waitDialog.setMessage("スキャン中・・・");
      waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      waitDialog.setCanceledOnTouchOutside(false);
      waitDialog.show();
      mHandler.postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "10秒たったので、scan終了");
            waitDialog.dismiss();
            mBluetoothAdapter.stopLeScan(MainActivity.this);
            if (BleStatus.SCANNING.equals(mStatus)) {
              setStatus(BleStatus.CLOSED);
            }
          }
      }, SCAN_PERIOD);
      Log.d(TAG, "START ------> LEScan: ");
      if(mBluetoothGattCallback == null) {
        mBluetoothGattCallback = new MyBluetoothGattCallback(getApplicationContext());
      }
      /* startLeScan()の第一引数はAndroidのバグがあり機能しない */
//        UUID[] uuids = {UUID.fromString(BleUuid.SERVICE_UUID),
//            UUID.fromString(BleUuid.SERVICE_DATA_UUID),
//            UUID.fromString(BleUuid.CHAR_INFO)};

      mBluetoothAdapter.startLeScan(this);
      setStatus(BleStatus.SCANNING);
    }
 
    /** BLE 機器との接続を解除する */
    private void disconnect() {
      Log.d(TAG, "START ------> disconnect(): ");
      if(waitDialog != null) {
        waitDialog.dismiss();
      }
      mBluetoothAdapter.stopLeScan(this);
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        setStatus(BleStatus.CLOSED);
    }
    
    private String getBondState(int state) {
      String strState;
      switch (state) {
      case BluetoothDevice.BOND_BONDED:
        strState = "接続履歴あり";
        break;
      case BluetoothDevice.BOND_BONDING:
        strState = "接続中";
        break;
      case BluetoothDevice.BOND_NONE:
        strState = "接続履歴なし";
        break;
      default :
        strState = "エラー";
      }
      return strState;
   }
   private String getDeviceType(int type) {
      String dType;
      switch (type) {
      case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
        dType = "Unknown";
        break;
      case BluetoothDevice.DEVICE_TYPE_CLASSIC:
        dType = "Classic - BR/EDR devices";
        break;
      case BluetoothDevice.DEVICE_TYPE_LE:
        dType = "Low Energy - LE-only";
        break;
      case BluetoothDevice.DEVICE_TYPE_DUAL:
        dType = "Dual Mode - BR/EDR/LE";
        break;
      default :
        dType = "エラー";
      }
      return dType;
   }
 
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
      String uuid = MyUtils.makeUuidFromAdv(scanRecord);

      if (uuid.equals(BleUuid.SERVICE_UUID)) {
        Log.d(TAG, "uuid : " + uuid);
      }
//      ProgressDialog progressDialog = new ProgressDialog(this);
//      progressDialog.show();

      Log.d(TAG, "device found: " + device.getName());
      Log.d(TAG, "rssi: " + rssi);
      Log.d(TAG, "scanRecord Length: " + scanRecord.length);

//        for (int i = 0; scanRecord.length > i; i++){
//          Log.d(TAG, "scanRecord[ " + i + "]" + " : 0x" + Integer.toHexString(scanRecord[i] & 0xff));
//
//        }
//      ParcelUuid[] uuids = device.getUuids();
//      if (uuids != null) {
//        for (int i = 0; uuids.length > i; i++){
//          Log.d(TAG, "uuids : " + uuids[i].toString());
//        }
//      }
            

      Log.d(TAG, "device.toString : " + device.toString());
      Log.d(TAG, "device.getType : " + getDeviceType(device.getType()));
      Log.d(TAG, "device.getAddress(MAC Addr) : " + device.getAddress());
      Log.d(TAG, "device.getBondState : " + getBondState(device.getBondState()));

      //setStatus(BleStatus.SCAN_STOP);
      // 省電力のためスキャンを停止する
      //mBluetoothAdapter.stopLeScan(this);
 
      // GATT接続を試みる
      //mBluetoothGatt = device.connectGatt(this, false, mBluetoothGattCallback);
    }
     
    public static void setStatus(BleStatus status) {
        mStatus = status;
        mHandler.sendMessage(status.message());
    }
 
    public enum BleStatus {
        DISCONNECTED,
        SCANNING,
        SCAN_FAILED,
        DEVICE_FOUND,
        SERVICE_NOT_FOUND,
        SERVICE_FOUND,
        CHARACTERISTIC_NOT_FOUND,
        NOTIFICATION_REGISTERED,
        NOTIFICATION_REGISTER_FAILED,
        CLOSED,
        SCAN_STOP
        ;
        public Message message() {
            Message message = new Message();
            message.obj = this;
            return message;
        }
    }
}