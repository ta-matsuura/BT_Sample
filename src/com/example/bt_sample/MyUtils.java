package com.example.bt_sample;

public class MyUtils {
  public final static int READ = 0;
  public final static int WRITE = 1;
  public final static int REQ_MTU = 2;
  public final static int WRITE2 = 3;


  
  /* Advertise packetからUUIDを作成する。Android 4.4以下では
   * UUIDをAPIで取得できない。
   * scanRecordの[5] - [20]まで（配列の6番目から21番目まで）を取り出す。
   * 取り出した値は128bitのService UUIDを期待する。
   * この値はperipheral側で設定し、Advertise paketに含まれるUUIDであること。*/
  public static String makeUuidFromAdv(byte[] scanRecord) {
    String uuid = null;
    uuid = IntToHex2(scanRecord[20] & 0xff) + IntToHex2(scanRecord[19] & 0xff)
        + IntToHex2(scanRecord[18] & 0xff) + IntToHex2(scanRecord[17] & 0xff) + "-"
        + IntToHex2(scanRecord[16] & 0xff) + IntToHex2(scanRecord[15] & 0xff) + "-"
        + IntToHex2(scanRecord[14] & 0xff) + IntToHex2(scanRecord[13] & 0xff) + "-"
        + IntToHex2(scanRecord[12] & 0xff) + IntToHex2(scanRecord[11] & 0xff) + "-"
        + IntToHex2(scanRecord[10] & 0xff) + IntToHex2(scanRecord[9] & 0xff) 
        + IntToHex2(scanRecord[8] & 0xff) + IntToHex2(scanRecord[7] & 0xff)
        + IntToHex2(scanRecord[6] & 0xff) + IntToHex2(scanRecord[5] & 0xff);
    return uuid;
  }  
  private static String IntToHex2(int i) {
    char hex_2[] = {Character.forDigit((i>>4) & 0x0f, 16), Character.forDigit(i&0x0f, 16)};
    String hex_2_str = new String(hex_2);
    return hex_2_str.toLowerCase();
  }
}
