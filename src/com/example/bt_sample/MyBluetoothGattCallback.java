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
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class MyBluetoothGattCallback extends BluetoothGattCallback{
  private static final String TAG = "BLESample";
  private int mType;
  private BleScanGattHandler mHandler;
  private boolean isWriting;

  public MyBluetoothGattCallback(Context context, int type, BleScanGattHandler handler) {
    mType = type;
    mHandler = handler;
    isWriting = false;
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt,
    BluetoothGattCharacteristic characteristic, int status) {
    String str = characteristic.getStringValue(0);
	  Log.d(TAG, "START -> onCharacteristicRead() status(0:success, 257:fail) : " + status );
	  Log.d(TAG, "String that you read is ... " + str);
	  super.onCharacteristicRead(gatt, characteristic, status);
    Message message = new Message();
    Bundle bundle = new Bundle();
    bundle.putString("char_read_result", str);
    message.setData(bundle);
    mHandler.sendMessage(message);
  }

  @Override
  public void onCharacteristicWrite(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic, int status) {
    Log.d(TAG, "START -> onCharacteristicWrite() status(0:success, 257:fail) : " + status);
    super.onCharacteristicWrite(gatt, characteristic, status);
    
    if(!isWriting) {
      Message message = new Message();
      Bundle bundle = new Bundle();
      bundle.putString("char_write_result", characteristic.getStringValue(0));
      message.setData(bundle);
      mHandler.sendMessage(message);
    } else {
      Log.d(TAG, "waiting for finishing writing .....");
      //gatt.executeReliableWrite();
    }

  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic) {
    super.onCharacteristicChanged(gatt, characteristic);
    Log.d(TAG, "START -> onCharacteristicChanged() ");

  }

  @Override
  public void onDescriptorRead(BluetoothGatt gatt,
      BluetoothGattDescriptor descriptor, int status) {
    super.onDescriptorRead(gatt, descriptor, status);
    Log.d(TAG, "START -> onDescriptorRead() ");

  }

  @Override
  public void onDescriptorWrite(BluetoothGatt gatt,
      BluetoothGattDescriptor descriptor, int status) {
    super.onDescriptorWrite(gatt, descriptor, status);
    Log.d(TAG, "START -> onDescriptorWrite() ");

  }

  @Override
  public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
    super.onReliableWriteCompleted(gatt, status);
    Log.d(TAG, "START -> onReliableWriteCompleted() ");

  }

  @Override
  public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    super.onReadRemoteRssi(gatt, rssi, status);
    Log.d(TAG, "START -> onReadRemoteRssi() ");

  }

  @Override
  public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
    super.onMtuChanged(gatt, mtu, status);
    Log.d(TAG, "START -> onMtuChanged() status(0:success, 257:fail) : " + status );
    Log.d(TAG, " mtu : " + mtu); 
    gatt.close();

  }

  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    Log.d(TAG, "START ---> onConnectionStateChange : " + getGattStatus(status) + "  newState(2:connected, 0:disconn) : " + newState);
    if (status != BluetoothGatt.GATT_SUCCESS) {
      gatt.close();
      Message message = new Message();
      Bundle bundle = new Bundle();
      message.setData(bundle);
      mHandler.sendMessage(message);
      MainActivity.setStatus(BleStatus.DISCONNECTED);
      return;
    }
    if (newState == BluetoothProfile.STATE_CONNECTED) {
      // GATTへ接続成功
      // サービスを検索する
      gatt.discoverServices();
    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      // GATT通信から切断された
      MainActivity.setStatus(BleStatus.DISCONNECTED);
      gatt.close();
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
      default :
    	state = state + " : " + s;
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
            if(characteristic.setValue(MainActivity.getWriteString())) {
              if(gatt.writeCharacteristic(characteristic)){
                Log.d(TAG, " Write request operation was initiated successfully. Please wait callback.");
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
            if (gatt.readCharacteristic(characteristic)) {
                Log.d(TAG, " Read request operation was initiated successfully. Please wait callback.");
            }
          }          
        } else if (mType == MyUtils.REQ_MTU) {
          //nothing to do...
        } else if (mType == MyUtils.WRITE2) {
//          if(!gatt.beginReliableWrite()) {
//            Log.d(TAG, " beginReliableWrite is failed.");
//            return;
//          }
          BluetoothGattCharacteristic charactaristic =
              service.getCharacteristic(UUID.fromString(BleUuid.CHAR_ONOFF_STRING));
          isWriting = true;
          if(!writeCharactaristic(gatt, charactaristic)) {
            isWriting = false;
          }
        }
      }
    }
  } // end of the function [onServicesDiscovered()]
  
  private boolean writeCharactaristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
    boolean ret = false;
    if(characteristic == null) {
      Log.d(TAG, "ERROR: characteristic is null");
      return ret;
    }
    if(characteristic.setValue(MainActivity.getWriteLongString())) {
      if(gatt.writeCharacteristic(characteristic)){
        Log.d(TAG, " Requested long String write. Please wait callback.");
        ret = true;
      }else {
        Log.d(TAG, " writeCharacteristic is failed");
      }
    }else{
      Log.d(TAG, " setValue is failed");
    }
    return ret;
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
