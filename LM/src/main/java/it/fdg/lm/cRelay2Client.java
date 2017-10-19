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

import static it.fdg.lm.cProgram3.mMsgDebug;
import static it.fdg.lm.cProgram3.mMsgLog;

public class cRelay2Client {
    // Intent request codes
    // Debugging
    private static final String TAG = "cRelay2Client";

    // Name for the SDP record when creating server socket
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
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
    public static cKonst.eSerial bRelayState = cKonst.eSerial.kBT_Undefined;
    private Context mContext;
    public static String sInString="";
    public static String sClientName = "";// Name of the mClientConnection device
    private int nCounter=0;
    private BluetoothAdapter oBTAdapter;         //Local reference
    private String sMessageLog;
    private cKonst.eSerial kBrokenConnection;

//  ******************************++            IMPLEMENTATIONS         *********************


    //PROPERTIES
    public cKonst.eSerial mStateGet() {
        return bRelayState;
    }
    void mStateSet(cKonst.eSerial newState) {
        bRelayState =newState;
    }
    public boolean mIsConnected1() {      //Is there a client
        if (mStateGet()==kBrokenConnection)
            mCloseService2();
        return mStateGet() == cKonst.eSerial.kBT_Connected1;
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
                mStateSet(cKonst.eSerial.kBT_TimeOut);
                mMsgDebug("Socket : " +  "listen() failed"+ e);
            }
        }
        public void run() {     //started by mInsecureAcceptThread.start();
            mStateSet(cKonst.eSerial.kListening); // Listen to the server socket if we're not mClientConnection
            bState=2;
             try {                    // This is a blocking call and will only return on a successful connection or an exception
                 oSocketClient = oSocketServer.accept(30000);  //170926 Timeout after 15 seconds
                 //*********************Thread stops here until connection can be accepted********
                 mStateSet(cKonst.eSerial.kListenAccepted);
                     try {                                      //Pass the streams onto global objects
                         oInputStream = oSocketClient.getInputStream();
                         oOutputStream = oSocketClient.getOutputStream();
                         sClientName =oSocketClient.getRemoteDevice().getName();
                         mStateSet(cKonst.eSerial.kBT_Connected1);
                     } catch (IOException e) {
                         mStateSet(cKonst.eSerial.kBT_TimeOut);
                         mMsgLog( "temp sockets not created"+ e);
                     }

                } catch (IOException e) {
                    mStateSet(cKonst.eSerial.kBT_TimeOut);
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

        // Only if the state is STATE_NONE, do we know that we haven't started already
        if (mStateGet() == cKonst.eSerial.kBT_Undefined) {           // Start listening for a connection
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
            if (ret){
                mMsgLog("Thread is alive "+ret);
                mInsecureAcceptThread.interrupt();      //Kill the old thread
            }
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
        mStateSet(cKonst.eSerial.kBT_Undefined);
    }       //Close streams

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
            if (mStateGet()!= cKonst.eSerial.kBT_Connected1) {
                return;
            }
            try {       //Maybe the steam was closed by receiver
                oOutputStream.write(out);
            } catch (IOException e) {
                mStateSet(cKonst.eSerial.kBT_BrokenConnection);
            }
        }
    }  //Write data to client



}

