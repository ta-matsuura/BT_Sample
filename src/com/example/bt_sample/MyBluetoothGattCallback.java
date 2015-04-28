package com.example.bt_sample;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.example.bt_sample.MainActivity.BleStatus;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class MyBluetoothGattCallback extends BluetoothGattCallback{
  private static final String TAG = "BLESample";
  private int mType;
  private BleScanGattHandler mHandler;

  public MyBluetoothGattCallback(Context context, int type, BleScanGattHandler handler) {
    mType = type;
    mHandler = handler;
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicRead(gatt, characteristic, status);
    String str = characteristic.getStringValue(0);
    Log.d(TAG, "STRT -> onCharacteristicRead() status(0:success, 257:fail) : " + status );
    Log.d(TAG, "String that you read is ... " + str);
    
    Message message = new Message();
    Bundle bundle = new Bundle();
    bundle.putString("char_read_result", str);
    message.setData(bundle);
    mHandler.sendMessage(message);
  }

  @Override
  public void onCharacteristicWrite(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicWrite(gatt, characteristic, status);
    Log.d(TAG, "STRT -> onCharacteristicWrite() status(0:success, 257:fail) : " + status);

  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic) {
    super.onCharacteristicChanged(gatt, characteristic);
    Log.d(TAG, "STRT -> onCharacteristicChanged() ");

  }

  @Override
  public void onDescriptorRead(BluetoothGatt gatt,
      BluetoothGattDescriptor descriptor, int status) {
    super.onDescriptorRead(gatt, descriptor, status);
    Log.d(TAG, "STRT -> onDescriptorRead() ");

  }

  @Override
  public void onDescriptorWrite(BluetoothGatt gatt,
      BluetoothGattDescriptor descriptor, int status) {
    super.onDescriptorWrite(gatt, descriptor, status);
    Log.d(TAG, "STRT -> onDescriptorWrite() ");

  }

  @Override
  public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
    super.onReliableWriteCompleted(gatt, status);
    Log.d(TAG, "STRT -> onReliableWriteCompleted() ");

  }

  @Override
  public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    super.onReadRemoteRssi(gatt, rssi, status);
    Log.d(TAG, "STRT -> onReadRemoteRssi() ");

  }

  @Override
  public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
    super.onMtuChanged(gatt, mtu, status);
    Log.d(TAG, "STRT -> onMtuChanged() ");

  }

  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    Log.d(TAG, "START ---> onConnectionStateChange : " + getGattStatus(status) + "  newState(2:connected, 0:disconn) : " + newState);
    if (newState == BluetoothProfile.STATE_CONNECTED) {
      // GATTへ接続成功
      // サービスを検索する
      gatt.discoverServices();
    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      // GATT通信から切断された
      MainActivity.setStatus(BleStatus.DISCONNECTED);
      MainActivity.setBluetoothGatt(null);
    } else if (newState == BluetoothProfile.STATE_CONNECTING) {
    } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
    }
  }
  
  private String getGattStatus(int s) {
    String state = "エラー";
    switch(s) {
      case  BluetoothGatt.GATT_CONNECTION_CONGESTED:
        state = "GATT_CONNECTION_CONGESTED";
        break;
      case  BluetoothGatt.GATT_FAILURE:
        state = "GATT_FAILURE";
        break;
      case  BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
        state = "GATT_INSUFFICIENT_ENCRYPTION";
        break;
      case  BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
        state = "GATT_INSUFFICIENT_AUTHENTICATION";
        break;
      case  BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
        state = "GATT_INVALID_ATTRIBUTE_LENGTH";
        break;
      case  BluetoothGatt.GATT_INVALID_OFFSET:
        state = "GATT_INVALID_OFFSET";
        break;
      case  BluetoothGatt.GATT_READ_NOT_PERMITTED:
        state = "GATT_READ_NOT_PERMITTED";
        break;
      case  BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
        state = "GATT_REQUEST_NOT_SUPPORTED";
        break;
      case  BluetoothGatt.GATT_SUCCESS:
        state = "GATT_SUCCESS";
        break;
      case  BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
        state = "GATT_WRITE_NOT_PERMITTED";
        break;
    }
    return state;
  }

  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    List<BluetoothGattService> serviceList = gatt.getServices();
    Iterator<BluetoothGattService> i = serviceList.iterator();
    while (i.hasNext()) {
      BluetoothGattService servise= (BluetoothGattService)i.next();
      Log.d(TAG, "service UUID : " + servise.getUuid());
    }
    serviceList = gatt.getServices();
    Log.d(TAG, "START ---> onServicesDiscovered() status : " + getGattStatus(status));
    if (status == BluetoothGatt.GATT_SUCCESS) {
      BluetoothGattService service = gatt.getService(UUID.fromString(BleUuid.CHAR_INFO));
      if (service == null) {
        // サービスが見つからなかった
        Log.d(TAG, " ----- > SERVICE_NOT_FOUND");
        MainActivity.setStatus(BleStatus.SERVICE_NOT_FOUND);
      } else {
        // サービスを見つけた
        Log.d(TAG, " ----- > SERVICE_FOUND");
        MainActivity.setStatus(BleStatus.SERVICE_FOUND);
        if(mType == MyUtils.WRITE) {
          BluetoothGattCharacteristic characteristic =
              service.getCharacteristic(UUID.fromString(BleUuid.CHAR_ONOFF_STRING));
    
          if (characteristic == null) {
            // キャラクタリスティックが見つからなかった
            MainActivity.setStatus(BleStatus.CHARACTERISTIC_NOT_FOUND);
          } else {
            Log.d(TAG, " ----- > CHAR_ONOFF_STRING FOUND");
            if(characteristic.setValue(MainActivity.getmWriteString())) {
              if(gatt.writeCharacteristic(characteristic)){
                Log.d(TAG, " write operation was initiated successfully");
              }else {
                Log.d(TAG, " writeCharacteristic is failed");
              }
            }else{
              Log.d(TAG, " setValue is failed");
            }              
          }
        } else if(mType == MyUtils.READ){
          BluetoothGattCharacteristic characteristic =
              service.getCharacteristic(UUID.fromString(BleUuid.CHAR_NAME_STRING));
    
          if (characteristic == null) {
            // キャラクタリスティックが見つからなかった
            MainActivity.setStatus(BleStatus.CHARACTERISTIC_NOT_FOUND);
          } else {
            Log.d(TAG, " ----- > CHAR_NAME_STRING FOUND");
            gatt.readCharacteristic(characteristic);
          }          
        }
      }
    }
  }
}

// キャラクタリスティックを見つけた
// Notification を要求する
//boolean registered = gatt.setCharacteristicNotification(characteristic, true);

//    // Characteristic の Notification 有効化
//    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//            UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
//    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//    gatt.writeDescriptor(descriptor);

//    if (registered) {
//        // Characteristics通知設定完了
//      MainActivity.setStatus(BleStatus.NOTIFICATION_REGISTERED);
//    } else {
//      MainActivity.setStatus(BleStatus.NOTIFICATION_REGISTER_FAILED);
//    }
