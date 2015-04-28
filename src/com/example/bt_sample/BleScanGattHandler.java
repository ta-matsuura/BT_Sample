package com.example.bt_sample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public abstract class BleScanGattHandler extends Handler{
  //このメソッドは隠ぺいし，Messageなどの低レベルオブジェクトを
  // 直接扱わないでもよいようにさせる
  public void handleMessage(Message msg)
  {
    Bundle bundle = msg.getData();
    onProcessCompleted(bundle);
  }

  // 下記をoverrideさせずに抽象化した理由は，本クラス指定時に
  // 「実装されていないメソッドの追加」でメソッドスタブを楽に自動生成させるため。
  // また，異常系の処理フローも真剣にコーディングさせるため。
  
  public abstract void onProcessCompleted(Bundle bundle);

}
