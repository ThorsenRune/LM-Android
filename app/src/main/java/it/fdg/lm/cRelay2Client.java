//171004
//Doc:  https://docs.google.com/document/d/198DBxS-FI_i8akcla2nmeknQPhWdiaWiAgYCejMRjZ8/edit
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
/*      Alpha version class for developing relay fuctionality
*
*
* */
package it.fdg.lm;

/*
*  Created by rthorsen on 22/09/2017.
*  Inspired by the android bluetooth chat example
Use

    public static BluetoothAdapter mBluetoothAdapter;   //ex mAdapter
    public static cRelay2Client mChatService = null;
…
        mChatService = new cRelay2Client();  //170921 remember to call CloseService
        mChatService.mOpenService(mContext,this);
… sending data
  if (mChatService.mIsConnected1()){
                    mChatService.sendMessage(message);
.., receiving data
 n = mChatService.oInputStream.available();

*/


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.mMsgDebug;
import static it.fdg.lm.cProgram3.mMsgLog;

public class cRelay2Client {
    // Intent request codes
    // Debugging
    private static final String TAG = "cRelay2Client";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
  //  private static final UUID MY_UUID_SECURE =  UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    public static int nBytesInBuffer;

    // Member fields
//    private final Handler mHandler;
//    private cAcceptThread mSecureAcceptThread;
    private cAcceptThread mInsecureAcceptThread;
    static BluetoothServerSocket oSocketServer =null;
    public BluetoothSocket oSocketClient;
    public InputStream oInputStream;
    public OutputStream oOutputStream;
    private int mNewState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTENING = 1;     // now listening for incoming connections
    public static final int STATE_WAIT2CONNECT = 2;
    public static final int STATE_CONNECTED = 3;  // now mClientConnection to a remote device
    private static final int STATE_LISTEN_FAILED = 104;

    private static final int STATE_BROKEN = 113;    //A broken pipe was reported 170922
    public static final int STATE_LISTEN_ACCEPTED = 5;
    public static int bRelayState =STATE_NONE;

    private Context mContext;
    public static String sInString="";
    public static String mConnectedDeviceName = null;// Name of the mClientConnection device
    private int nCounter=0;
    private BluetoothAdapter oBTAdapter;         //Local reference
    private String sMessageLog;

//  ******************************++            IMPLEMENTATIONS         *********************


    //PROPERTIES
    public  int mRelayState() {
        return bRelayState;
    }
    private void mSetState(int newState) {
        bRelayState =newState;
    }
    public boolean mIsConnected1() {      //Is there a client
        if (mRelayState()==STATE_BROKEN)
            mCloseService2();
        return this.mRelayState() == cRelay2Client.STATE_CONNECTED;
    } //tHERE IS A CLIENT?


    //METHODS

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class cAcceptThread extends Thread {
        // The local server socket
        public int bState=0;
        public cAcceptThread() {           //Called when mInsecureAcceptThread.mStartListening()
            try {
                oSocketServer = oBTAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                bState=1;
                mMsgDebug("Server is Listening");         //When a server connects it will call run
            } catch (IOException e) {
                bState=13;
                mSetState(STATE_LISTEN_FAILED);
                mMsgDebug("Socket : " +  "listen() failed"+ e);
            }
        }
        public void run() {     //started by mInsecureAcceptThread.start();
            mSetState(STATE_LISTENING); // Listen to the server socket if we're not mClientConnection
            bState=2;
             try {                    // This is a blocking call and will only return on a successful connection or an exception
                 oSocketClient = oSocketServer.accept(30000);  //170926 Timeout after 15 seconds
                 //*********************Thread stops here until connection can be accepted********
                 mSetState(STATE_LISTEN_ACCEPTED);
                     try {                                      //Pass the streams onto global objects
                         oInputStream = oSocketClient.getInputStream();
                         oOutputStream = oSocketClient.getOutputStream();
                         mSetState(STATE_CONNECTED);
                         mMessage("Connected to " + oSocketClient.getRemoteDevice().getName());
                     } catch (IOException e) {
                         mMsgLog( "temp sockets not created"+ e);
                     }

                } catch (IOException e) {
                    mSetState(STATE_LISTEN_FAILED);
                    mMsgLog("Error Stopped listening"+e.toString());
                }
            if (oSocketServer ==null) return;
            try {
                oSocketServer.close();
                oSocketServer =null;

            } catch (IOException e) {
                mMsgLog("close() of server failed " + e.toString());
            }
            bState=3;       //Ending the thread
        }
    }
    private synchronized void mConnectStreams(BluetoothSocket socket) {
        mMsgLog( "mClientConnection, Socket Type:" );
        try {
            oInputStream = socket.getInputStream();
            oOutputStream = socket.getOutputStream();
            mSetState(STATE_CONNECTED);
        } catch (IOException e) {
            mMsgLog( "temp sockets not created"+ e);
        }

    }   //Called when a connection is made
    //************************************OPENING CLOSING **************************************
    public void mOpenService(Context context, BluetoothAdapter oBTadapter){  //170921 remember to call CloseService
        mContext=context;    // Prepares a new BluetoothChat session.
        this.oBTAdapter=oBTadapter;
        // Check the adapter
        if (oBTAdapter== null) {
            mMsgLog("Bluetooth is not supported");
            return;
        } else if (oBTAdapter.isEnabled()==false){
            mMsgLog("Bluetooth not enabled");
            return;
        }
        mMessageState();
        // Only if the state is STATE_NONE, do we know that we haven't started already
        if (mRelayState() == STATE_NONE) {           // Start listening for a connection
            mMsgLog("Start listening");
            if (mInsecureAcceptThread == null) {
                mInsecureAcceptThread = new cAcceptThread();
                mInsecureAcceptThread.start();
            }
        } else {
            mMsgLog("Failed listening, retry");
            mCloseService2();
        }
    }

    public synchronized void mCloseService2() {     //Close the service if it was opened
        mMsgLog("Closing service");
        if (mInsecureAcceptThread != null) {
            boolean ret = mInsecureAcceptThread.isAlive();
            mMsgLog("Thread is alive "+ret);
            if (ret)
                mInsecureAcceptThread.interrupt();      //Kill the old thread
            mInsecureAcceptThread = null;
        }
        try {
            if (oSocketServer != null) oSocketServer.close();
            if (oInputStream != null) oInputStream.close();
            if (oOutputStream != null) oOutputStream.close();
            oInputStream = null;
            oOutputStream = null;
            oSocketServer = null;
            mMsgDebug("Socket Type  closing in _mCloseStreams ");
        } catch (IOException e) {
            mMsgLog("close() of server failed " + e.toString());
        }
        mSetState( STATE_NONE);
    }       //Close streams
    private void mMessageState() {
        if (bRelayState ==STATE_LISTENING)
            mMsgLog("Already listening");
        else if (bRelayState ==STATE_BROKEN)
            mMsgLog("Broken pipe");
        else if (bRelayState ==STATE_NONE)
            mMsgLog("Ready state");
        else if (bRelayState ==STATE_LISTEN_FAILED)
            mMsgLog("STATE_LISTEN_FAILED");
        else if (bRelayState ==STATE_LISTEN_ACCEPTED)
            mMsgLog("STATE_LISTEN_ACCEPTED");
        else
            mMsgLog("UNKNOWN STATE");

    }

    //*************************             PUBLIC  METHODS


    public void mWriteByte(OutputStream oOutputStream,int out) {           //Cast the byt to an array
        byte[] out1=new byte[1];
        out1[0]= (byte) out;
        mWriteByte(oOutputStream,out1);
    }
    public void mWriteByte(OutputStream oOutputStream,byte[] out) {
        //170922 Write a byt to the client
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mRelayState()!= STATE_CONNECTED) {
                mMessageState();
                return;
            }
            try {       //Maybe the steam was closed by receiver
                oOutputStream.write(out);
            } catch (IOException e) {
                mSetState(STATE_BROKEN);
            }
        }
    }  //Write data to client



}

