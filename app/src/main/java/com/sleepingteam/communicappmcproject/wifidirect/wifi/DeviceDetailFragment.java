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

package com.sleepingteam.communicappmcproject.wifidirect.wifi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.kbeanie.multipicker.api.FilePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.FilePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenFile;
import com.sleepingteam.communicappmcproject.R;
import com.sleepingteam.communicappmcproject.wifidirect.beans.WiFiTransferModal;
import com.sleepingteam.communicappmcproject.wifidirect.utils.PermissionsAndroid;
import com.sleepingteam.communicappmcproject.wifidirect.utils.SharedPreferencesHandler;
import com.sleepingteam.communicappmcproject.wifidirect.utils.Utils;
import com.sleepingteam.communicappmcproject.wifidirect.wifi.DeviceListFragment.DeviceActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import static com.sleepingteam.communicappmcproject.wifidirect.utils.PermissionsAndroid.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends android.support.v4.app.Fragment implements ConnectionInfoListener, FilePickerCallback {

    static InterstitialAd mInterstitialAd;

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    private static ProgressDialog mProgressDialog;

    public static String WiFiServerIp = "";
    public static String WiFiClientIp = "";
    static Boolean ClientCheck = false;
    public static String GroupOwnerAddress = "";
    static long ActualFilelength = 0;
    static int Percentage = 0;
    public static String FolderName = "WiFiDirectDemo";

    private FilePicker filePicker;
    private String pickerPath;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("picker_path")) {
                pickerPath = savedInstanceState.getString("picker_path");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);

        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getString(R.string.fullscreen_ad_unit_id));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });

        requestNewInterstitial();

        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                if (config != null && config.deviceAddress != null && device != null) {
                    config.deviceAddress = device.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                            "Connecting to :" + device.deviceAddress, true, true
                    );
                    ((DeviceActionListener) getActivity()).connect(config);
                } else {

                }
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
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        checkExternalStoragePermission();
                    }
                });

        return mContentView;
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    private void checkExternalStoragePermission() {
        boolean isExternalStorage = PermissionsAndroid.getInstance().checkWriteExternalStoragePermission(getActivity());
        if (!isExternalStorage) {
            PermissionsAndroid.getInstance().requestForWriteExternalStoragePermission(getActivity());
        } else {
            pickFilesSingle();
        }
    }

    private void pickFilesSingle() {
        filePicker = new FilePicker(getActivity());
        filePicker.setFilePickerCallback(this);
        filePicker.setFolderName(getActivity().getString(R.string.app_name));
        filePicker.pickFile();
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFilesSingle();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Utils.getInstance().showToast("Camera/Storage permission Denied");
                }
                return;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == Picker.PICK_FILE) {
                filePicker.submit(data);
            }
        } else {
            CommonMethods.DisplayToast(getActivity(), "Cancelled Request");
        }
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
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

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        if (info.groupOwnerAddress.getHostAddress() != null)
            view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
        else {
            CommonMethods.DisplayToast(getActivity(), "Host Address not found");
        }
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        try {
            String GroupOwner = info.groupOwnerAddress.getHostAddress();
            if (GroupOwner != null && !GroupOwner.equals(""))
                SharedPreferencesHandler.setStringValues(getActivity(),
                        getString(R.string.pref_GroupOwnerAddress), GroupOwner);
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);

            //first check for file storage permission
            if (!PermissionsAndroid.getInstance().checkWriteExternalStoragePermission(getActivity())) {
                Utils.getInstance().showToast("Please enable storage Permission from application storage option");
                return;
            }

            if (info.groupFormed && info.isGroupOwner) {
            /*
             * set shaerdprefrence which remember that device is server.
        	 */
                SharedPreferencesHandler.setStringValues(getActivity(),
                        getString(R.string.pref_ServerBoolean), "true");

                FileServerAsyncTask FileServerobj = new FileServerAsyncTask(
                        getActivity(), FileTransferService.PORT);
                if (FileServerobj != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        FileServerobj.executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR,
                                new String[]{null});
                        // FileServerobj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,Void);
                    } else
                        FileServerobj.execute();
                }
            } else {
                // The other device acts as the client. In this case, we enable the
                // get file button.
                if (!ClientCheck) {
                    firstConnectionMessage firstObj = new firstConnectionMessage(
                            GroupOwnerAddress);
                    if (firstObj != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            firstObj.executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR,
                                    new String[]{null});
                        } else
                            firstObj.execute();
                    }
                }

                FileServerAsyncTask FileServerobj = new FileServerAsyncTask(
                        getActivity(), FileTransferService.PORT);
                if (FileServerobj != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        FileServerobj.executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR,
                                new String[]{null});
                    } else
                        FileServerobj.execute();

                }

            }
        } catch (Exception e) {

        }
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
        /*
         * Remove All the prefrences here
         */
        SharedPreferencesHandler.setStringValues(getActivity(),
                getString(R.string.pref_GroupOwnerAddress), "");
        SharedPreferencesHandler.setStringValues(getActivity(),
                getString(R.string.pref_ServerBoolean), "");
        SharedPreferencesHandler.setStringValues(getActivity(),
                getString(R.string.pref_WiFiClientIp), "");

        ClientCheck = false;
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    static Handler handler;

    @Override
    public void onFilesChosen(List<ChosenFile> list) {
        ChosenFile file = list.get(0);
        String extension = "";
        int i = list.get(0).getDisplayName().lastIndexOf('.');
        if (i > 0) {
            extension = list.get(0).getDisplayName().substring(i + 1);
        }

        ActualFilelength = file.getSize();
        ;

        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + file.getOriginalPath());

        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, file.getQueryUri());
                /*
                 * Choose on which device file has to send weather its server or client
    	         */
        String Ip = SharedPreferencesHandler.getStringValues(
                getActivity(), getString(R.string.pref_WiFiClientIp));
        String OwnerIp = SharedPreferencesHandler.getStringValues(
                getActivity(), getString(R.string.pref_GroupOwnerAddress));
        if (!TextUtils.isEmpty(OwnerIp) && OwnerIp.length() > 0) {
            String host = null;
            int sub_port = -1;

            String ServerBool = SharedPreferencesHandler.getStringValues(getActivity(), getString(R.string.pref_ServerBoolean));
            if (!TextUtils.isEmpty(ServerBool) && ServerBool.equalsIgnoreCase("true") && !TextUtils.isEmpty(Ip)) {
                host = Ip;
                sub_port = FileTransferService.PORT;
                serviceIntent
                        .putExtra(
                                FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                Ip);

            } else {
                FileTransferService.PORT = 8888;
                host = OwnerIp;
                sub_port = FileTransferService.PORT;
                serviceIntent.putExtra(
                        FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        OwnerIp);
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, FileTransferService.PORT);

            serviceIntent.putExtra(FileTransferService.Extension, file.getDisplayName());

            serviceIntent.putExtra(FileTransferService.Filelength,
                    String.valueOf(ActualFilelength));

            if (host != null && sub_port != -1) {
                showprogress("Sending...");
                getActivity().startService(serviceIntent);
            } else {
                CommonMethods.DisplayToast(getActivity(),
                        "Host Address not found, Please Re-Connect");
                DismissProgressDialog();
            }

        } else {
            DismissProgressDialog();
            CommonMethods.DisplayToast(getActivity(),
                    "Host Address not found, Please Re-Connect");
        }
    }

    @Override
    public void onError(String s) {

    }

    public static class FileServerAsyncTask extends AsyncTask<String, String, String> {

        //        private TextView statusText;
        private Context mFilecontext;
        private String Extension, Key;
        private File EncryptedFile;
        private long ReceivedFileLength;
        private int PORT;

        /**
         * @param context
         * @param port
         */
        public FileServerAsyncTask(Context context, int port) {
            this.mFilecontext = context;
            handler = new Handler();
            this.PORT = port;
            if (mProgressDialog == null)
                mProgressDialog = new ProgressDialog(mFilecontext,
                        ProgressDialog.THEME_HOLO_LIGHT);
        }


        @Override
        protected String doInBackground(String... params) {
            try {
                CommonMethods.e("File Async task port", "File Async task port-> " + PORT);
                // init handler for progressdialog
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));

                Log.d(CommonMethods.Tag, "Server: Socket opened");
                Socket client = serverSocket.accept();

                Utils.d("Client's InetAddresssss  ", "" + client.getInetAddress());

                WiFiClientIp = client.getInetAddress().getHostAddress();
                ObjectInputStream ois = new ObjectInputStream(
                        client.getInputStream());
                WiFiTransferModal obj = null;
                String InetAddress;
                try {
                    obj = (WiFiTransferModal) ois.readObject();

                    if (obj != null) {
                        InetAddress = obj.getInetAddress();
                        if (InetAddress != null
                                && InetAddress
                                .equalsIgnoreCase(FileTransferService.inetaddress)) {
                            CommonMethods.e("File Async Group Client Ip", "port-> "
                                    + WiFiClientIp);
                            SharedPreferencesHandler.setStringValues(mFilecontext,
                                    mFilecontext.getString(R.string.pref_WiFiClientIp), WiFiClientIp);
                            CommonMethods
                                    .e("File Async Group Client Ip from SHAREDPrefrence",
                                            "port-> "
                                                    + SharedPreferencesHandler
                                                    .getStringValues(
                                                            mFilecontext,
                                                            mFilecontext.getString(R.string.pref_WiFiClientIp)));
                            //set boolean true which identifiy that this device will act as server.
                            SharedPreferencesHandler.setStringValues(mFilecontext,
                                    mFilecontext.getString(R.string.pref_ServerBoolean), "true");
                            ois.close(); // close the ObjectOutputStream object
                            // after saving
                            serverSocket.close();

                            return "Demo";
                        }

                        final Runnable r = new Runnable() {

                            public void run() {
                                // TODO Auto-generated method stub
                                mProgressDialog.setMessage("Receiving...");
                                mProgressDialog.setIndeterminate(false);
                                mProgressDialog.setMax(100);
                                mProgressDialog.setProgress(0);
                                mProgressDialog.setProgressNumberFormat(null);
                                mProgressDialog
                                        .setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                mProgressDialog.show();
                            }
                        };
                        handler.post(r);
                        Utils.d("FileName got from socket on other side->>> ",
                                obj.getFileName());
                    }

                    final File f = new File(
                            Environment.getExternalStorageDirectory() + "/"
                                    + FolderName + "/"
                                    + obj.getFileName());

                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();

				/*
                 * Recieve file length and copy after it
				 */
                    this.ReceivedFileLength = obj.getFileLength();

                    InputStream inputstream = client.getInputStream();


                    copyRecievedFile(inputstream, new FileOutputStream(f),
                            ReceivedFileLength);
                    ois.close(); // close the ObjectOutputStream object after saving
                    // file to storage.
                    serverSocket.close();

				/*
                 * Set file related data and decrypt file in postExecute.
				 */
                    this.Extension = obj.getFileName();
                    this.EncryptedFile = f;
                    return f.getAbsolutePath();

                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return "";
            } catch (IOException e) {
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
            if (result != null) {
                if (!result.equalsIgnoreCase("Demo")) {
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    }
                    openFile(result, mFilecontext);
                } else if (!TextUtils.isEmpty(result)) {
                    /*
                     * To initiate socket again we are intiating async task
					 * in this condition.
					 */
                    FileServerAsyncTask FileServerobj = new
                            FileServerAsyncTask(mFilecontext, FileTransferService.PORT);
                    if (FileServerobj != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            FileServerobj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{null});

                        } else FileServerobj.execute();
                    }
                }
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(mFilecontext);
            }
        }
    }


    public static void openFile(String stringUrl, Context context) {
        Uri uri = Uri.parse(stringUrl);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= 24) {
            Uri fileURI = FileProvider.getUriForFile(context,
                    com.sleepingteam.communicappmcproject.BuildConfig.APPLICATION_ID + ".provider",
                    new File(stringUrl));
            uri = fileURI;
        }else{
            uri = Uri.parse("file://" + uri.getPath());
        }
        if (stringUrl.toString().contains(".doc") || stringUrl.toString().contains(".docx")) {
            // Word document
            intent.setDataAndType(uri, "application/msword");
        } else if (stringUrl.toString().contains(".pdf")) {
            // PDF file
            intent.setDataAndType(uri, "application/pdf");
        } else if (stringUrl.toString().contains(".ppt") || stringUrl.toString().contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        } else if (stringUrl.toString().contains(".xls") || stringUrl.toString().contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(uri, "application/vnd.ms-excel");
        } else if (stringUrl.toString().contains(".zip") || stringUrl.toString().contains(".rar")) {
            // WAV audio file
            intent.setDataAndType(uri, "application/x-wav");
        } else if (stringUrl.toString().contains(".rtf")) {
            // RTF file
            intent.setDataAndType(uri, "application/rtf");
        } else if (stringUrl.toString().contains(".wav") || stringUrl.toString().contains(".mp3")) {
            // WAV audio file
            intent.setDataAndType(uri, "audio/x-wav");
        } else if (stringUrl.toString().contains(".gif")) {
            // GIF file
            intent.setDataAndType(uri, "image/gif");
        } else if (stringUrl.toString().contains(".jpg") || stringUrl.toString().contains(".jpeg") || stringUrl.toString().contains(".png")) {
            // JPG file
            intent.setDataAndType(uri, "image/*");
        } else if (stringUrl.toString().contains(".txt")) {
            // Text file
            intent.setDataAndType(uri, "text/plain");
        } else if (stringUrl.toString().contains(".3gp") || stringUrl.toString().contains(".mpg") || stringUrl.toString().contains(".mpeg") || stringUrl.toString().contains(".mpe") || stringUrl.toString().contains(".mp4") || stringUrl.toString().contains(".avi")) {
            // Video files
            intent.setDataAndType(uri, "video/*");
        } else {
            //if you want you can also define the intent type for any other file
            //additionally use else clause below, to manage other unknown extensions
            //in this case, Android will show all applications installed on the device
            //so you can choose which application to use
            intent.setDataAndType(uri, "*/*");
        }
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }


    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        long total = 0;
        long test = 0;
        byte buf[] = new byte[FileTransferService.ByteSize];
        if (buf == null) return false;

        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                try {
                    total += len;
                    if (ActualFilelength > 0) {
                        Percentage = (int) ((total * 100) / ActualFilelength);
                    }
                    mProgressDialog.setProgress(Percentage);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    Percentage = 0;
                    ActualFilelength = 0;
                }
            }
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }

            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    public static boolean copyRecievedFile(InputStream inputStream,
                                           OutputStream out, Long length) {

        byte buf[] = new byte[FileTransferService.ByteSize];
        byte Decryptedbuf[] = new byte[FileTransferService.ByteSize];
        String Decrypted;
        int len;
        long total = 0;
        int progresspercentage = 0;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                try {
                    out.write(buf, 0, len);
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                try {
                    total += len;
                    if (length > 0) {
                        progresspercentage = (int) ((total * 100) / length);
                    }
                    mProgressDialog.setProgress(progresspercentage);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    if (mProgressDialog != null) {
                        if (mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                    }
                }
            }
            // dismiss progress after sending
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    public void showprogress(final String task) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity(),
                    ProgressDialog.THEME_HOLO_LIGHT);
        }
        Handler handle = new Handler();
        final Runnable send = new Runnable() {

            public void run() {
                // TODO Auto-generated method stub
                mProgressDialog.setMessage(task);
                // mProgressDialog.setProgressNumberFormat(null);
                // mProgressDialog.setProgressPercentFormat(null);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(100);
//				mProgressDialog.setCancelable(false);
                mProgressDialog.setProgressNumberFormat(null);
                mProgressDialog
                        .setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
            }
        };
        handle.post(send);
    }

    public static void DismissProgressDialog() {
        try {
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }


    /*
     * Async class that has to be called when connection establish first time. Its main motive is to send blank message
     * to server so that server knows the IP address of client to send files Bi-Directional.
     */
    class firstConnectionMessage extends AsyncTask<String, Void, String> {

        String GroupOwnerAddress = "";

        public firstConnectionMessage(String owner) {
            // TODO Auto-generated constructor stub
            this.GroupOwnerAddress = owner;

        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            CommonMethods.e("On first Connect", "On first Connect");

            Intent serviceIntent = new Intent(getActivity(),
                    WiFiClientIPTransferService.class);

            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);

            if (info.groupOwnerAddress.getHostAddress() != null) {
                serviceIntent.putExtra(
                        FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        info.groupOwnerAddress.getHostAddress());

                serviceIntent.putExtra(
                        FileTransferService.EXTRAS_GROUP_OWNER_PORT,
                        FileTransferService.PORT);
                serviceIntent.putExtra(FileTransferService.inetaddress,
                        FileTransferService.inetaddress);

            }

            getActivity().startService(serviceIntent);

            return "success";
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            if (result != null) {
                if (result.equalsIgnoreCase("success")) {
                    CommonMethods.e("On first Connect",
                            "On first Connect sent to asynctask");
                    ClientCheck = true;
                }
            }

        }

    }
}
