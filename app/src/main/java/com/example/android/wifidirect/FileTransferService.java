// Copyright 2011 Google Inc. All Rights Reserved.

package com.example.android.wifidirect;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String ACTION_SEND_IP = "com.example.android.wifidirect.SEND_IP";
    public static final String ACTION_SEND_FDIP_ADDR = "com.example.android.wifidirect.SEND_FDIP_ADDR";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_IP_INFO = "ip_addr";
    public static final String EXTRAS_DST_ADDRESS = "go_host";

    private int port = Utility.port_num;

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_DST_ADDRESS);
            Log.d(WiFiDirectActivity.TAG, "host = "+host);
            Socket socket = new Socket();

            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);


                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                DataOutputStream dStream = new DataOutputStream(stream);
                dStream.writeInt(Utility.file_type);

                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                DeviceDetailFragment.copyFile(is, stream);
                Log.d(WiFiDirectActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }else if (intent.getAction().equals(ACTION_SEND_IP))  {
            String peerIp = intent.getExtras().getString(EXTRAS_IP_INFO);
            String host = intent.getExtras().getString(EXTRAS_DST_ADDRESS);
            Log.d(WiFiDirectActivity.TAG, "host = "+host);
            Socket socket = new Socket();
            try {
                Log.d(WiFiDirectActivity.TAG, "IP Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "IP Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                DataOutputStream dStream = new DataOutputStream(stream);
                dStream.writeInt(Utility.local_ip_type);

                try {
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);

                    pw.println(peerIp);
                } catch (IOException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                Log.d(WiFiDirectActivity.TAG, "IP Client: Data written");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                            Log.d(WiFiDirectActivity.TAG, "IP Client: closed");
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }else if (intent.getAction().equals(ACTION_SEND_FDIP_ADDR))  {
            String peerIp = intent.getExtras().getString(EXTRAS_IP_INFO);
            String host = intent.getExtras().getString(EXTRAS_DST_ADDRESS);
            Socket socket = new Socket();
            try {
                Log.d(WiFiDirectActivity.TAG, "IP Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "IP Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                DataOutputStream dStream = new DataOutputStream(stream);
                dStream.writeInt(Utility.forward_ip_addr);

                try {
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);

                    pw.println(peerIp);
                } catch (IOException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                Log.d(WiFiDirectActivity.TAG, "IP Client: Data written");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                            Log.d(WiFiDirectActivity.TAG, "IP Client: closed");
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }
}
