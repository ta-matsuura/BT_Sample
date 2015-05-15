package com.example.bt_sample;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
  private MyBluetoothGattCallback mMyBluetoothCallback = null;

  
  public static String TAG = "BLESample";
  BluetoothDevice device;
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    Bundle b = intent.getExtras();
    Object[] lstName = b.keySet().toArray();
    Log.d(TAG, "MyBroadcastReceiver " + device.getBondState());

    if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
      device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
      switch (device.getBondState()) {
      case BluetoothDevice.BOND_BONDING:
              Log.d(TAG, "MyBroadcastReceiver :: BOND_BONDING");
              break;
      case BluetoothDevice.BOND_BONDED:
          Log.d(TAG, "MyBroadcastReceiver :: BOND_BONDED");

//              Log.d(TAG, "MyBroadcastReceiver");
//              if(mMyBluetoothCallback == null) {
//                mMyBluetoothCallback = new MyBluetoothGattCallback(getApplicationContext(), myHandler);
//              }
//              device.connectGatt(context, false, callback);
              break;
      case BluetoothDevice.BOND_NONE:
              Log.d(TAG, "MyBroadcastReceiver :: BOND_NONE");
      default:
              break;
      }
    }
  }
}
