/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.xml.transform.Result;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    private static String localIp;
    private static String groupOwnerIp;
    public static String fileDstIp;
    public static int ipsNum = 1;
    private static final int MAX_IP_NUM = 10;
    public static String[] groupIps = new String[MAX_IP_NUM];
    private static boolean peerIpSent = false;
    public static boolean isGroupOwner = false;
    private static String forwardIp; // for GO to forward
    private boolean groupIpsChanged = false;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);

        mContentView.findViewById(R.id.btn_send_ip).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        transferIps();
                    }
                });


        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                localIp = Utility.getLocalIpAddress();
                Log.d(WiFiDirectActivity.TAG, "connect botton... localIp = " + localIp);
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        /*pop*/
                        //String[] items = groupIps;
                        String[] items = new String[ipsNum];
                        Log.d(WiFiDirectActivity.TAG, "ipsNum = "+ipsNum);
                        for (int i = 0; i < ipsNum; i++) items[i] = groupIps[i];
                        ShowAdapterAlert(items);

                        Log.d(WiFiDirectActivity.TAG, "fileDstIp = " + fileDstIp);


                    }
                });

        return mContentView;
    }
    /*pop*/

    void ShowAdapterAlert(String[] strArray)
    {
        final String[] items = strArray;
        ArrayAdapter<String> arr=new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item,items);

        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose destination IP");

        builder.setAdapter(arr, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                fileDstIp = items[which];

                Log.d(WiFiDirectActivity.TAG, "dstIp chosen: fileDstIp = " + fileDstIp);
                try {
                    sendIp(fileDstIp, groupOwnerIp, true);
                }catch (Exception e){
                    e.printStackTrace();
                }
                // Allow user to pick an image from Gallery or other
                // registered apps
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                Log.d(WiFiDirectActivity.TAG, "pick image activity ends = ");
            }
        });
        builder.show();
    }

    private void transferIps(){

        if(info.isGroupOwner){
            Log.d(WiFiDirectActivity.TAG, "GO, send it's group ips to non-GO now");
            groupIpsChanged = false;
            for(int i=1; i<ipsNum; i++){
                for(int j=1; j<ipsNum; j++){
                    try {
                        if(i!=j)sendIp(groupIps[j], groupIps[i], false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }else{
            peerIpSent = true;
            try {
                Log.d(WiFiDirectActivity.TAG, "non-GO, send ip to GO now");
                sendIp(localIp, groupOwnerIp, false);
            } catch (Exception e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());
            }
        }

    }

    private void sendIp(String sentIpAdr, String dstIp, boolean isForwardIp) throws Exception {

        //TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        if(isForwardIp) serviceIntent.setAction(FileTransferService.ACTION_SEND_FDIP_ADDR);
        else serviceIntent.setAction(FileTransferService.ACTION_SEND_IP);

        serviceIntent.putExtra(FileTransferService.EXTRAS_DST_ADDRESS, dstIp);
        Log.d(WiFiDirectActivity.TAG, "ip = " + sentIpAdr);
        serviceIntent.putExtra(FileTransferService.EXTRAS_IP_INFO, sentIpAdr);
        Log.d(WiFiDirectActivity.TAG, "send ip:" + sentIpAdr + " to = " + dstIp);
        getActivity().startService(serviceIntent);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Sending: " + uri.toString());
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        if(!info.isGroupOwner)serviceIntent.putExtra(FileTransferService.EXTRAS_DST_ADDRESS, groupOwnerIp);
        else serviceIntent.putExtra(FileTransferService.EXTRAS_DST_ADDRESS, fileDstIp);

        if(!info.isGroupOwner) Log.d(WiFiDirectActivity.TAG, "Send image to ip = " + groupOwnerIp);
        else Log.d(WiFiDirectActivity.TAG, "Send image to ip = " + fileDstIp);
        Log.d(WiFiDirectActivity.TAG, "Send image to port "  + Utility.port_num);

        getActivity().startService(serviceIntent);
    }


    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        Log.d(WiFiDirectActivity.TAG, "onConnect start!!");



        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
        localIp = Utility.getLocalIpAddress();
        groupOwnerIp = info.groupOwnerAddress.getHostAddress();
        isGroupOwner = info.isGroupOwner;

        if(fileDstIp == null) fileDstIp = groupOwnerIp; //initialize fileDstIp
        groupIps[0] = localIp;
        if(!info.isGroupOwner && ipsNum < 2){
            groupIps[1] = groupOwnerIp;
            ipsNum = 2;
        }

        if(info.isGroupOwner && groupIpsChanged) transferIps();
        else if(!info.isGroupOwner && !peerIpSent) transferIps();

        if(info.groupFormed) {
            //boolean cancel = AsyncTask.cancel(; boolean mayInterruptIfRunning)
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                    .execute();
        }





        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.

        // The other device acts as the client. In this case, we enable the
        // get file button.
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
        mContentView.findViewById(R.id.btn_send_ip).setVisibility(View.VISIBLE);
        ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                .getString(R.string.client_text));
        //}

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
        Log.d(WiFiDirectActivity.TAG, "onConnect fished!!");
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        private int type; // type defined in Utility
        ServerSocket serverSocket;

        private boolean isIncluded(String[] strArr, String str){
            for(int i = 0; i<strArr.length; i++){
                if(str.equals(strArr[i])) return true;
            }
            return false;
        }

        public void forwardFile(Uri uri, String dstIp) {


            //Uri uri = data.getData();
            //TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
            //statusText.setText("Sending: " + uri);
            Log.d(WiFiDirectActivity.TAG, "Sending: " + uri);
            Log.d(WiFiDirectActivity.TAG, "Sending: " + uri.toString());
            Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
            serviceIntent.putExtra(FileTransferService.EXTRAS_DST_ADDRESS, dstIp);
            Log.d(WiFiDirectActivity.TAG, "Send image to ip = " + dstIp);
            Log.d(WiFiDirectActivity.TAG, "Send image to port "  + Utility.port_num);

            getActivity().startService(serviceIntent);


        }

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.d(WiFiDirectActivity.TAG, "Server: before socket open");
            try {
                Log.d(WiFiDirectActivity.TAG, "Server: before socket open");
                serverSocket = new ServerSocket(Utility.port_num);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened in port "+Utility.port_num);
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");

                InputStream stream = client.getInputStream();
                DataInputStream dStream = new DataInputStream(stream);
                type = dStream.readInt();
                Log.d(WiFiDirectActivity.TAG, "type = "+type);

                switch (type){
                    case Utility.file_type:
                        String fileAdr = Environment.getExternalStorageDirectory() + "/"
                                + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                                + ".jpg";
                        final File f = new File(fileAdr);

                        File dirs = new File(f.getParent());
                        if (!dirs.exists())
                            dirs.mkdirs();
                        f.createNewFile();

                        Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                        InputStream inputstream = client.getInputStream();
                        copyFile(inputstream, new FileOutputStream(f));

                        Log.d(WiFiDirectActivity.TAG, "DeviceDetailFragment.isGroupOwner = "+DeviceDetailFragment.isGroupOwner);


                        if(DeviceDetailFragment.isGroupOwner){
                            Log.d(WiFiDirectActivity.TAG, "Group Owner forward file to ip = "+forwardIp);
                            Log.d(WiFiDirectActivity.TAG, "Uri of f = "+Uri.fromFile(f));
                            if(forwardIp != groupOwnerIp) forwardFile(Uri.fromFile(f), forwardIp);
                        }

                        serverSocket.close();
                        Log.d(WiFiDirectActivity.TAG, "server: close");
                        return f.getAbsolutePath();

                    case Utility.local_ip_type:
                        BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        String peerIp = br.readLine();

                        Log.d(WiFiDirectActivity.TAG, "IP server: peer IP = " + peerIp);

                        if(peerIp != null){
                            Log.d(WiFiDirectActivity.TAG, "peerIp != null");
                            if(ipsNum >= MAX_IP_NUM) Log.d(WiFiDirectActivity.TAG, "ip number exceeds: " + peerIp);
                            else{
                                Log.d(WiFiDirectActivity.TAG, "groupIps["+ipsNum+"] = " + peerIp);
                                groupIps[ipsNum] = peerIp;
                                ipsNum++;
                                Log.d(WiFiDirectActivity.TAG, "new ipsNum = " + ipsNum);
                                groupIpsChanged = true;
                            }
                        } else Log.d(WiFiDirectActivity.TAG, "peerIp is null");

                        if(info.isGroupOwner && groupIpsChanged) transferIps();

                        serverSocket.close();
                        Log.d(WiFiDirectActivity.TAG, "IP Server closed ");
                        return peerIp;

                    case Utility.forward_ip_addr:
                        BufferedReader brf = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        forwardIp = brf.readLine();

                        Log.d(WiFiDirectActivity.TAG, "GO Forward Server: forwardIp = " + forwardIp);


                        serverSocket.close();
                        Log.d(WiFiDirectActivity.TAG, "IP Server closed ");
                        return forwardIp;

                    default:
                        return null;



                }

            } catch (IOException e) {
                Log.d(WiFiDirectActivity.TAG, "oh my god exception!");
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }


        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (type == Utility.file_type && result != null) {
                //statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }else{
                onConnectionInfoAvailable(info);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            //statusText.setText("Opening a server socket");
            Log.d(WiFiDirectActivity.TAG, "Opening a server socket");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }


}
