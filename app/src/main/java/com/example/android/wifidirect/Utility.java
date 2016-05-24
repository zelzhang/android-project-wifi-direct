package com.example.android.wifidirect;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.net.*;
import java.util.*;

//import org.apache.http.conn.util.InetAddressUtils;

public class Utility{


    public static final char file_type = 0;
    public static final char local_ip_type = 1;
    public static final char request_file_type = 2;

    public static final int port_num = 8999;

    /**
     * Get IP address from first non-localhost interface
     * @param ipv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getLocalIpAddress(boolean ipv4){
        boolean useIPv4 = ipv4;
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
    public static long convertIpv4ToLong(String ip){
        String[] sub = splitString(ip, ".");
        //Log.d(WiFiDirectActivity.TAG, "sub = " +sub[0]);
        long multiply = 1000000000;
        long returnVal = 0;
        for(int i=0; i<sub.length; i++){
            returnVal += multiply*Integer.parseInt(sub[i]);
            multiply /= 1000;
        }
        return returnVal;
    }

    public static String convertLongToIpv4(Long ipLong){
        String returnStr = "";
        long devide = 1000000000;
        long add;
        for(int i=0; i<4; i++){
            add = ipLong/devide;
            if(i != 3)returnStr = returnStr + add + ".";
            else returnStr = returnStr + add;
            ipLong %= devide;
            devide /= 1000;
        }
        return returnStr;
    }


    public static String[] splitString(String oldString, String splitter){
        char splitterChar = splitter.charAt(0);
        String newString = "";
        char c;
        for(int i=0; i<oldString.length(); i++){
            c = oldString.charAt(i);
            if(c == splitterChar) newString += "\n";
            else newString += c;
        }
        return newString.split("[\r\n]+");
    }
    public static String changeTmpPath(String originalPath){
        String[] strings = originalPath.split("\\.");
        Log.d("Utility", "changeTmpPath = " + originalPath);
        if(strings.length == 2)return strings[0]+"_tmp."+strings[1];
        else if(strings.length == 1) return  strings[0]+"_tmp";
        else return null;
    }
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    public static String secondToMinuteSecond(double second){
        int min = (int)second/60;
        String minS = (min<10)? "0"+min:""+min;
        int sec = (int)second%60;
        String secS = (sec<10)? "0"+sec:""+sec;
        return minS + ":" + secS;
    }
    public static void updateSongTime(){
        DeviceDetailFragment.songTimeString = secondToMinuteSecond(DeviceDetailFragment.startTime);
    }
    public static double ip2newTime(String peerIp){
        String[] ipNames = peerIp.split("\\s+");
        String song = ipNames[2];
        int len = song.length();
        int a= song.charAt(len-1) - '0';
        int b= song.charAt(len-2) - '0';
        int c= song.charAt(len-4) - '0';
        int d= song.charAt(len-5) - '0';
        return (double)(600*d+60*c+10*b+a+0.5);
    }
}
