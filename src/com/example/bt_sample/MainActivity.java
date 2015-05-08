package com.example.bt_sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.*;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
 
 
public class MainActivity extends Activity {
	protected Context mContext;
    /** BLE 機器スキャンタイムアウト (ミリ秒) */
    private static final long SCAN_PERIOD = 3500;
 
    private static final String TAG = "BLESample";
    private static BleStatus mStatus = BleStatus.DISCONNECTED;
    private static Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private static BluetoothGatt mBluetoothGatt;

    private TextView mStatusText;
    
    private BluetoothGattCallback mReadCallback;
    private BluetoothGattCallback mWriteCallback;
    private BluetoothGattCallback mWriteCallback2;

    private BluetoothGattCallback mRequestMtuCallback;


    public static BluetoothDevice mDevice;
    private EditText mEditText;
    private EditText mEditText2;

    private static String mWriteString;
    private static String mWriteLongString;
    private static ProgressDialog mDialog;
  
    public static String getWriteString() {
      return mWriteString;
    }
    public static String getWriteLongString() {
      return mWriteLongString;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        mDialog = new ProgressDialog(this);

        setContentView(R.layout.activity_main);
 
        mBluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        mEditText   = (EditText)findViewById(R.id.editor1);
        mEditText2   = (EditText)findViewById(R.id.editor2);
        
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mStatusText.setText(((BleStatus) msg.obj).name());
            }
        };
    }
 
    /** BLE機器を検索する */
    private void connect() {
      mDialog.setMessage("スキャン中・・・");
      mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mDialog.setCanceledOnTouchOutside(false);
      mDialog.show();
      mHandler.postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "3.5秒たったので、scan終了します");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mDialog.dismiss();
            if (mDevice != null) {
              Toast.makeText(getApplicationContext(), mDevice.toString() + "を見つけました", Toast.LENGTH_LONG).show();
              setStatus(BleStatus.DEVICE_FOUND);
            }else {
              Toast.makeText(getApplicationContext(), "見つかりませんでした", Toast.LENGTH_LONG).show();
              if (BleStatus.SCANNING.equals(mStatus)) {
                setStatus(BleStatus.CLOSED);
              }
            }
          }
      }, SCAN_PERIOD);
      Log.d(TAG, "START ------> LEScan: ");
      /* startLeScan()の第一引数はAndroidのバグがあり機能しない */
//        UUID[] uuids = {UUID.fromString(BleUuid.SERVICE_UUID),
//            UUID.fromString(BleUuid.SERVICE_DATA_UUID),
//            UUID.fromString(BleUuid.CHAR_INFO)};
      Log.d(TAG, "CAll  mBluetoothAdapter.startLeScan(this);");
      mBluetoothAdapter.startLeScan(mLeScanCallback);
      setStatus(BleStatus.SCANNING);
    }
 
    /** BLE 機器との接続を解除する */
    private void disconnect() {
      Log.d(TAG, "START ------> disconnect(): ");
      TextView tx = (TextView)findViewById(R.id.read_result);
      tx.setText("none");
      mDevice = null;
      if(MainActivity.mDialog != null) {
    	  mDialog.dismiss();
      }
      mBluetoothAdapter.stopLeScan(mLeScanCallback);
      if (mBluetoothGatt != null) {
        mBluetoothGatt.close();
        mBluetoothGatt = null;
      }
      setStatus(BleStatus.CLOSED);
    }
    
    private static String getBondState(int state) {
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
   private static String getDeviceType(int type) {
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
   private static BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
      String uuid = MyUtils.makeUuidFromAdv(scanRecord);

      if (uuid.equals(BleUuid.UUID_SERVICE)) {
        Log.d(TAG, "uuid : " + uuid);
        mDevice = device;
      }

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
 
    }};
     
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
    
    public void onClickWriteButton(View v) {
      Log.v(TAG,"onClick writeClicked");
      if (mDevice == null) {
        return;
      }
      mWriteString = mEditText.getText().toString();
      if (mWriteCallback == null) {
        mWriteCallback = new MyBluetoothGattCallback(getApplicationContext(), MyUtils.WRITE, myHandler);
      }
      mDialog.setMessage("write・・・");
      mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mDialog.setCanceledOnTouchOutside(false);
      mDialog.show();

      mBluetoothGatt = mDevice.connectGatt(getApplicationContext(), false, mWriteCallback);
    }
    public void onClickReadButton(View v) {
      Log.v(TAG,"onClick readClicked");
      if (mDevice == null) {
        return;
      }
      if(mReadCallback == null) {
        mReadCallback = new MyBluetoothGattCallback(getApplicationContext(), MyUtils.READ, myHandler);
      }
      mDialog.setMessage("read・・・");
      mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mDialog.setCanceledOnTouchOutside(false);
      mDialog.show();
      mBluetoothGatt = mDevice.connectGatt(getApplicationContext(), false, mReadCallback);
    }
    public void onClickRequestMtu(View v) {
      Log.v(TAG,"onClick RequestMtu");
      if (mDevice == null) {
        return;
      }
      mBluetoothGatt = mDevice.connectGatt(getApplicationContext(), false, mRequestMtuCallback);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if(mRequestMtuCallback == null) {
          mRequestMtuCallback = new MyBluetoothGattCallback(getApplicationContext(), MyUtils.REQ_MTU, myHandler);
        }
        EditText et = (EditText)findViewById(R.id.mtu_value);
        int i = Integer.parseInt(et.getText().toString());
        mBluetoothGatt.requestMtu(i);
        Log.v(TAG,"CALLED ---> RequestMtu() : " + i);
      } else {
        Log.v(TAG," BluetoothGatt.requestMtu() is not supported for Android KK.");
        return;
      }
    }
    /* 
     * これはlong stringの書き込み用に用意したボタンなので
     * 無効にしておく。ともあれlong stringの書き込みはどうやらできないっぽい
     * android4.4 x Nexus9で試したが無理っぽい。FWのバグの可能性高い
     * 参考サイト：http://code.google.com/p/android/issues/detail?id=158619
     *  */
//    public void onClickWriteButton2(View v) {
//      Log.v(TAG,"onClick writeClicked2");
//      if (mDevice == null) {
//        return;
//      }
//      mWriteLongString = mEditText2.getText().toString();
//      if (mWriteCallback2 == null) {
//        mWriteCallback2 = new MyBluetoothGattCallback(getApplicationContext(), MyUtils.WRITE2, myHandler);
//        mDialog.setMessage("write・・・");
//        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        mDialog.setCanceledOnTouchOutside(false);
//        mDialog.show();
//        mBluetoothGatt = mDevice.connectGatt(getApplicationContext(), false, mWriteCallback2);
//      }
//
//    }
    private BleScanGattHandler myHandler = new BleScanGattHandler(){
      @Override
      public void onProcessCompleted(Bundle bundle) {
    	if(mDialog.isShowing()) {
    		  mDialog.dismiss();
    	}
    	mBluetoothGatt.close();
        if (bundle.get("char_read_result") != null) {
          TextView tx = (TextView)findViewById(R.id.read_result);
          tx.setText(bundle.get("char_read_result").toString());
        }
      }
    };
}