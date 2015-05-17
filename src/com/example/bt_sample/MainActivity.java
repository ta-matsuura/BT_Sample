package com.example.bt_sample;

import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private static BleStatus mStatus = BleStatus.INIT;
    private static Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private TextView mStatusText;
    private MyBluetoothGattCallback mMyBluetoothCallback = null;

    public static BluetoothDevice mDevice;
    private EditText mEditText;
    private EditText mEditText2;

    private static String mWriteString;
    private static String mWriteLongString;
    private static ProgressDialog mDialog;
    private IntentFilter intentfilter;
  
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
        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Button btn = (Button)findViewById(R.id.btn_scan);
            if (btn.getText().toString().equals("Clear")) {
              discardDevice();
            } else {
              scan();
            }
          }
      });
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
        
//        receiver = new MyBroadcastReceiver();
//        intentfilter = new IntentFilter();
//        intentfilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, intent);
    }
    
    /** BLE機器を解除する */
    private void discardDevice() {
      Log.d(TAG, "START ------> all information clear... start from scan. ");
      Toast.makeText(getApplicationContext(), "すべて破棄しました。Scanからやり直してください。", Toast.LENGTH_LONG).show();

      Button btn = (Button)findViewById(R.id.btn_scan);
      btn.setText("Scan");
      mDevice = null;
           
      TextView tx = (TextView)findViewById(R.id.read_result);
      tx.setText("none");
      if(mDialog != null) {
        mDialog.dismiss();
      }
      if (mMyBluetoothCallback != null) {
        mMyBluetoothCallback.gattClose();
      }
      mBluetoothAdapter.stopLeScan(mLeScanCallback);
      setStatus(BleStatus.INIT);
    }
 
    /** BLE機器を検索する */
    private void scan() {
      /*お試しなのでデッドコードでよい*/
      if(false) {
        Intent discoverableIntent = new
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
            return;
      }
      
      mDevice = null;
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
              setStatus(BleStatus.DEVICE_FOUND);
              //pairDevice(mDevice);
            }else {
              Toast.makeText(getApplicationContext(), "デバイスが見つかりませんでした", Toast.LENGTH_LONG).show();
              if (BleStatus.SCANNING.equals(mStatus)) {
                setStatus(BleStatus.INIT);
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
      
      Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
      // If there are paired devices
      if (pairedDevices.size() > 0) {
        // Loop through paired devices
        for (BluetoothDevice device : pairedDevices) {
          // Add the name and address to an array adapter to show in a ListView
          Log.d(TAG, "pairedDevice.Name : " + device.getName());
          Log.d(TAG, "pairedDevice.Address : " + device.getAddress());
          Log.d(TAG, "pairedDevice.Type : " + device.getType());
          Log.d(TAG, "pairedDevice.Class : " + device.getBluetoothClass());

        }
      }
    }
    
    /** GATT Serverと接続する */
    private void connect() {
      if (mDevice == null) {
        Toast.makeText(getApplicationContext(), "デバイスが見つかりません。スキャンが必要です。", Toast.LENGTH_LONG).show();
        return;
      }
      if (mStatus != BleStatus.DEVICE_FOUND && mStatus != BleStatus.GATT_DISCONNECTED) {
        Toast.makeText(getApplicationContext(), "すでに接続中かデバイスが見つかりません", Toast.LENGTH_LONG).show();
        return;
      }
      Log.v(TAG,"onClick connect");
      mDialog.setMessage("connecting・・・");
      mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mDialog.setCanceledOnTouchOutside(false);
      mDialog.show();
      
      if(mMyBluetoothCallback == null) {
        mMyBluetoothCallback = new MyBluetoothGattCallback(getApplicationContext(), myHandler);
      }
      /* これを呼ぶと、ペアリング要求が対向機に送られて
       * パスキー認証が行われるが、それをクリアしても
       * なぜかうまくいかない */
      //
      //Log.v(TAG,"mDevice.createBond() : " + mDevice.createBond());
      
      mDevice.connectGatt(getApplicationContext(), false, mMyBluetoothCallback);      
    }
 
    /** GATT Serverとのdisconnectする */
    private void disconnect() {
      Log.d(TAG, "START ------> disconnect(): ");
      if(MainActivity.mDialog != null) {
    	  mDialog.dismiss();
      }
      if (mMyBluetoothCallback != null) {
        mMyBluetoothCallback.gattDisconnect();
      }
      if( mStatus == BleStatus.GATT_CONNECTED || mStatus == BleStatus.GATT_SERVICE_DISCOVERED) {
        setStatus(BleStatus.GATT_DISCONNECTED);
      }
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
   
//   private void pairDevice(BluetoothDevice device) {
//       Intent intent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);
//       intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
//       intent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.PAIRING_VARIANT_PIN);
//       intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//       mContext.startActivity(intent);
//   }
   
   private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
      String uuid = MyUtils.makeUuidFromAdv(scanRecord);

      if (uuid.equals(BleUuid.UUID_SERVICE)) {
        Log.d(TAG, "uuid : " + uuid);
        mDevice = device;
        Log.d(TAG, "device.toString : " + device.toString());
        Log.d(TAG, "device.getType : " + getDeviceType(device.getType()));
        Log.d(TAG, "device.getAddress(MAC Addr) : " + device.getAddress());
        Log.d(TAG, "device.getBondState : " + getBondState(device.getBondState()));
   

        Button btn = (Button)findViewById(R.id.btn_scan);
        btn.setText("Clear");
      }
//
//      Log.d(TAG, "rssi: " + rssi);
//      Log.d(TAG, "scanRecord Length: " + scanRecord.length);

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

    }};
     
    public static void setStatus(BleStatus status) {
        mStatus = status;
        mHandler.sendMessage(status.message());
    }
 
    public enum BleStatus {
        SCAN_FAILED,
        SCANNING,
        DEVICE_FOUND,
        GATT_CONNECTED,
        GATT_DISCONNECTED,
        GATT_SERVICE_DISCOVERED,
        GATT_SERVICE_NOT_FOUND,
        CHARACTERISTIC_NOT_FOUND,
        INIT
        ;
        public Message message() {
            Message message = new Message();
            message.obj = this;
            return message;
        }
    }
    
    /* Tap Write button */
    public void onClickWriteButton(View v) {
      Log.v(TAG,"onClick writeClicked");
      if (mDevice == null) {
        Toast.makeText(getApplicationContext(), "デバイスが見つかりません。スキャンが必要です。", Toast.LENGTH_LONG).show();
        return;
      }
      if (mStatus != BleStatus.GATT_SERVICE_DISCOVERED) {
        Toast.makeText(getApplicationContext(), "GATT Serviceに接続してください。", Toast.LENGTH_LONG).show();
        return;
      }
      mWriteString = mEditText.getText().toString();

      mDialog.setMessage("write・・・");
      mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mDialog.setCanceledOnTouchOutside(false);
      mDialog.show();
      if(mMyBluetoothCallback != null)
        mMyBluetoothCallback.writeRequest();
    }

    /* Tap Read button */
    public void onClickReadButton(View v) {
      Log.v(TAG,"onClick readClicked");
      if (mDevice == null) {
        Toast.makeText(getApplicationContext(), "デバイスが見つかりません。スキャンが必要です。", Toast.LENGTH_LONG).show();
        return;
      }
      if (mStatus != BleStatus.GATT_SERVICE_DISCOVERED) {
        Toast.makeText(getApplicationContext(), "GATT Serviceに接続してください。", Toast.LENGTH_LONG).show();
        return;
      }

      mDialog.setMessage("read・・・");
      mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mDialog.setCanceledOnTouchOutside(false);
      mDialog.show();
      
      if(mMyBluetoothCallback != null)
        mMyBluetoothCallback.readRequest();
    }

    public void onClickRequestMtu(View v) {
      Log.v(TAG,"onClick RequestMtu");
      if (mDevice == null) {
        return;
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

        EditText et = (EditText)findViewById(R.id.mtu_value);
        int i = Integer.parseInt(et.getText().toString());
//        mBluetoothGatt.requestMtu(i);
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
    public void onClickWriteButton2(View v) {
      Log.v(TAG, "onClickWriteButton2");
      if (mDevice == null) {
        Toast.makeText(getApplicationContext(), "デバイスが見つかりません。スキャンが必要です。", Toast.LENGTH_LONG).show();
        return;
      }
      if (mStatus != BleStatus.GATT_SERVICE_DISCOVERED) {
        Toast.makeText(getApplicationContext(), "GATT Serviceに接続してください。", Toast.LENGTH_LONG).show();
        return;
      }
      mWriteLongString = mEditText2.getText().toString();

      mDialog.setMessage("write・・・");
      mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mDialog.setCanceledOnTouchOutside(false);
      mDialog.show();
      if(mMyBluetoothCallback != null) {
        mMyBluetoothCallback.writeLongStringRequest();
      }
    }
    private BleScanGattHandler myHandler = new BleScanGattHandler(){
      @Override
      public void onProcessCompleted(Bundle bundle) {
    	if(mDialog.isShowing()) {
    		  mDialog.dismiss();
    	}
        if (bundle.get("char_read_result") != null) {
          TextView tx = (TextView)findViewById(R.id.read_result);
          tx.setText(bundle.get("char_read_result").toString());
        }
      }
    };
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
             final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
             final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

             if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
               Toast.makeText(getApplicationContext(), "paired", Toast.LENGTH_LONG).show();
               //mDevice.connectGatt(getApplicationContext(), false, mMyBluetoothCallback);

             } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
               Toast.makeText(getApplicationContext(), "unpaired", Toast.LENGTH_LONG).show();
             }

        }
      }
    };
}
