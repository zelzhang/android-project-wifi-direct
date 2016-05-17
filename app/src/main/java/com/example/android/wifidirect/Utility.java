package com.example.android.wifidirect;

import android.media.AudioFormat;
import android.util.Log;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.*;

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


    /*
    public static String invalidCharEliminator(String oldString){
        String newString = "";
        char c;
        for(int i=0; i<oldString.length(); i++){
            c = oldString.charAt(i);
            if(c == '.' || c== ' ') newString += c;
            else if(c-'0' >=0 && c-'0'<=9) newString += c;
            else if(c-'a' >=0 && c-'a'<=26) newString += c;
            else if(c-'A' >=0 && c-'A'<=26) newString += c;
        }
        return newString;
    }
    */

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

    /*
    public static void copyAudio(String sourceFileName, String destinationFileName, int startSecond, int secondsToCopy) {

        AudioInputStream inputStream = null;
        AudioInputStream shortenedStream = null;

        try {
            File file = new File(sourceFileName);
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            AudioFormat format = fileFormat.getFormat();
            inputStream = AudioSystem.getAudioInputStream(file);
            int bytesPerSecond = format.getFrameSize() * (int)format.getFrameRate();
            inputStream.skip(startSecond * bytesPerSecond);
            long framesOfAudioToCopy = secondsToCopy * (int)format.getFrameRate();
            shortenedStream = new AudioInputStream(inputStream, format, framesOfAudioToCopy);
            File destinationFile = new File(destinationFileName);
            AudioSystem.write(shortenedStream, fileFormat.getType(), destinationFile);
        }catch (Exception e) {
            System.out.println(e);
        }finally {
            if (inputStream != null) try { inputStream.close(); } catch (Exception e) { System.out.println(e); }
            if (shortenedStream != null) try { shortenedStream.close(); } catch (Exception e) { System.out.println(e); }
        }
    }
    */
}
