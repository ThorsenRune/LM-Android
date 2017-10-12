package it.fdg.lm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;


import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_OFF;

import static it.fdg.lm.cAndMeth.mSleep;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.mMsgDebug;
import static it.fdg.lm.cProgram3.mMsgLog;

public class cSerial5 {
    private static final  int ENABLE_BLUETOOTH_REQUEST_CODE =0 ;
    private static final String ACTION_DEVICE_SELECTED =   "android.bluetooth.devicepicker.action.DEVICE_SELECTED";;
    /*              DECLARATIONS                */
    private cProtocol3 oParent;                        //Parent class

    //  Bluetooth stuff
    private BluetoothAdapter    oBTadapter=null;    //Adapt 1->n Device  -> Socket 1->2 Strean
    private BluetoothDevice     oDevice = null;     // A device in the adapter
    private BluetoothSocket     oSocket = null;     //  socket for teh device
    private OutputStream        oOutput = null;     // streams for the socket
    private InputStream         oInput = null;
    private boolean isOpenedByMe=false; //Tracks if this module opened the bluetooth. Used to clean up

    //  Uses the following buffers
    public cFIFO oRXFIFO;
    public cFIFO oTXFIFO;

        //Relay ports
    public cRelay2Client oBTServer;        //170922    object holding methods for acting as a server.
    private Context mContext;                   //Holding the current context (!-? can be removed?)

	cKonst.eSerial nState_Serial_ =cKonst.eSerial.kBT_ConnectRequest;
	    // SPP UUID service unique numbers identification for connections
    private static final UUID UUID_ANDROID_INSECURE =UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");       //Client
    private static final UUID UUID_LMDEVICE_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //LM Device
		
    public int nDataRcvd=0;            //Number of bytes received
    public int  nSndCtr=0;

    private String sRemoteDeviceName;

// !- stuff to cancel?
    static int nSemaphore=0;


    /*********************************************              IMPLEMENTATION          /**********************************************/
    public cSerial5(cProtocol3 owner) {
        oParent=owner;
    }   //Constructor
    public void mInit(String sId) {
        oRXFIFO =new cFIFO(cFIFO.kFIFOSize);
        oTXFIFO =new cFIFO(cFIFO.kFIFOSize);
        String sMyId = sId;
        mContext= cProgram3.mContext;
        if (oBTadapter==null) mBTInit();
        BroadcastReceiver_mEventListener();     //Activate listening to BT events
    }   //Initialization

    //BT Actions
    //R170315	Manage serial communication using the FIFO buffers
    public synchronized byte[] mProcessSerial(){               //170927 Returns array of data received from serial RX they are also put on receive buffer
        for (int loop=0;loop<200;loop++){
            if (oTXFIFO.mCanPop(1)==false) break;                //Send FIFO data to serial output stream
            int byB=oTXFIFO.mFIFOpop();
            mWriteByte(oOutput, (byte) byB);
        }
        byte[] aBytes = mReadBytes(oInput);
        if (aBytes==null) return null;
        for (int i=0;i<aBytes.length;i++){       //Loop through buffer and transfer from input stream to FIFO buffer    //+170601 revised from while loop to avoid endless loop
            if (oRXFIFO.mCanPush()){
                oRXFIFO.mFIFOpush(aBytes[i]);     //Put the data on the fifo
            }
            else
            {
                mStateSet(cKonst.eSerial.kOverflow);
                nDataRcvd=-1;
            }
        }
        return aBytes;
    }       //This is the worker method


    //State register
    private void mStateSet(cKonst.eSerial nNewState) {
        nState_Serial_=nNewState;
        bDoRedraw=true;     //Something has changed redraw controls
    }
    public cKonst.eSerial mStateGet() {
        return nState_Serial_;
    }
    public boolean mIsState(cKonst.eSerial nCheckState) {
        return nCheckState==nState_Serial_;
    }

    public boolean mBTOpen() {
        if (!oBTadapter.isEnabled()) {
            isOpenedByMe = true;
            oBTadapter.enable();  //Turn on bluetooth if not already active
        }
        return oBTadapter.isEnabled();
    }

    public void mFlushInput() {
        if (this.oInput ==null) return;
        try {
            this.oInput.skip(nBytesAvailable(oInput));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mBTClose1() {       //If this application opened the port then it will also politely close the door when it leaves
        mDevice_Disconnect();
        if (oBTadapter != null) {
            if (oBTadapter.isEnabled()) {
                if (isOpenedByMe)       //170905 only if this application opened the bluetooth
                    oBTadapter.disable();
                isOpenedByMe=false;
            }
        }
        if (mReceiver1!=null)
            mContext.unregisterReceiver(mReceiver1);
    }

    //mWriteByte    Writes to the serial stream
    private void mWriteByte(OutputStream oOutput,byte out)
    {

        if (oOutput!=null)      //Connection may have been interrupted
        {
            try {       //Maybe the steam was closed by receiver
                oOutput.write(out);
            } catch (IOException e) {
                mStateSet( cKonst.eSerial.kBrokenConnection);
                mProtocolStateSet(cKonst.eProtState.kConnectionError);
                mMsgDebug("Connection lost: "+e.getMessage());
                mDevice_Disconnect();
            }
        }
    }  //Write data to client safely

    private void mProtocolStateSet(cKonst.eProtState nValue) {
        oParent.mSetState(nValue);
    }

    private synchronized byte[] mReadBytes(InputStream oInput){
        nDataRcvd= nBytesAvailable(oInput);
        if (nDataRcvd==0)
            return null;
        byte[] buffer = new byte[nDataRcvd];
        try {
            nDataRcvd=oInput.read(buffer);          //Read is a blocking operation
        } catch (IOException e) {
            e.printStackTrace();
            buffer= null;
        }
        return buffer;
    }
    public int mReadByte2(InputStream oI) {        //To refactor into cProtocol 170929 // TODO: 29/09/2017
        int nAvail=0;
        try {
            nAvail = oI.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (nAvail>0)
            try {
                int data= oI.read();      //Remmeber that read is blocking
                return data;
            } catch (IOException e) {
                e.printStackTrace();
            }
        return -1;
    }       //Read one byte

    //	******************************Wrappers and helpers********************************
    public void mConnectionStateClear() {
        mStateSet(cKonst.eSerial.kBT_Connected);
    }   //Clear state flag

    public static int nBytesAvailable(InputStream oInput) {
        if (oInput==null) return 0;
        try {
            return oInput.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }   //Get bytes to read from stream

    public boolean mBTInit(){
        //Initialize the bluetooth adapter
        if (oBTadapter !=null) return true ;
        oBTadapter = BluetoothAdapter.getDefaultAdapter();
        //!!!r170307          returns true if connection is opened with success
        if (oBTadapter == null) {
            return false;
        }
        return true;
    }       //Init the BT device

    //Chain: mAsyncProcessing_Work->mBT_PickDevice..wait for user ...->receiver->onReceive
    public void mBT_PickDevice(Context oFocusdActivity){
        if (oBTadapter==null) mBTInit();
        mBTOpen();          //Make sure its opened
        IntentFilter filter = new IntentFilter();
//redundant        mStateSet(cKonst.eSerial.kDevicePickerActive);          //Will be changed dialog closes
        if ( oBTadapter.isDiscovering()){
            mErrMsg("Already in discover mode");
            return;
        }
        oBTadapter.startDiscovery();
        Intent intent = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
        mContext.startActivity(intent);
    }        //Find new devices  (discover)




    public boolean mStartServerService() {//170926    Start listening for a client to relay to (170926 !+todo make the call from connection event
        if (oBTServer == null) {
            oBTServer = new cRelay2Client();//170922
        }
        if (oBTServer.bRelayState ==cKonst.eSerial.kListening) {
            mMessage("Already listening");
            return false;
        }
        oBTServer.mOpenService(mContext, oBTadapter);//170922
        return true;
    }   //Start listening for a client (to where we can relay data)

//      Connection to the device given by name  Chain:mAsyncProcessing_Work->mConnect->
    public boolean mConnect(String sNewDeviceName) {        //Find and mConnect to the bluetooth
        boolean retval;
        mDevice_Disconnect();
        if (!oBTadapter.isEnabled())
            return false;
        if (oBTadapter.isDiscovering())
            oBTadapter.cancelDiscovery();       //Precautionary _mCloseStreams if something started discovery mode
        oDevice= mBTDeviceByName(sNewDeviceName);
        if (oDevice !=null) {
            retval= mConnect2Device(oDevice);
        }else {
            mMsgLog("Device: " + sNewDeviceName + "not found, try pairing");
            mStateSet(cKonst.eSerial.kDeviceNotFound);
            retval= false;
        }
        return retval;
    }

    public void mDevice_Disconnect() {       //Disconnect connection if there was one
        try {
            if (oOutput!=null){
                oOutput.close();
                oOutput=null;}
            if (oBTServer!=null)
                oBTServer.mCloseService2();
            if (oInput!=null){
                oInput.close();
                oInput=null;}
            if (oSocket!=null) {
                if (oSocket.isConnected())
                    mMessage("Disconnected");
                oSocket.close();
                oSocket = null;
            }
            if (oDevice!=null){
                oDevice=null;
            }
        } catch (IOException e) {
            mErrMsg("Err:"+e.toString());
        }
    }

    public boolean mIsDeviceResponding_Dodgy(){
        if (oSocket==null)
            return  false;
        if (oSocket.isConnected()==false)
            return false;
        if (0< (nBytesAvailable(oInput)+nDataRcvd ))      //Data in buffer or data received
            return true;
        else
            return false;
    }       //!+ rethink this. Should tell if the device is alive and connected

    private BluetoothDevice mBTDeviceByName(String sNewDeviceName) {       //Return the device having the name
        if (oDevice == null) {
            Set<BluetoothDevice> pairedDevices = oBTadapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (sNewDeviceName.equalsIgnoreCase(device.getName())) {
                        return device;
                    }
                }
            }
        }
        return null;
    }

    private boolean mIsAndroidDevice(BluetoothDevice oDevice1) {
        return (oDevice1.getBluetoothClass().getDeviceClass()== Device.PHONE_SMART);
    }
/*!-Remove
    private boolean mAndroidConnect(final BluetoothDevice oDevice){      //Experiment with a thread
        boolean bResult = false;
        int nAttempts;
        try {       //Make a socket for the connection 170929
            if (oSocket!=null) oSocket.close();
            oSocket = oDevice.createInsecureRfcommSocketToServiceRecord(UUID_ANDROID_INSECURE);
        } catch (IOException e) {
            mErrMsg("Socket Type:insecure " + "listen() failed" + e.toString());
        }
        for (nAttempts=0;nAttempts<5;nAttempts++) {
            //Wait a second before trying to connect
            mSleep(1000);
            try {
                mMessage("Connecting " + oDevice.getName() + " Attempt #:"+ nAttempts);
                oSocket.connect();
                bResult = mConnect_StreamsInit(oSocket);
                if (bResult)
                    mMessage("Connection success");
                return bResult;
            } catch (IOException e) {
                bResult = false;

            }
        }
        return bResult;
    }
*/

    private boolean mConnect2Device(BluetoothDevice oDevice1){    //mFileRead the BT and set the  _DeviceName
    if (mIsAndroidDevice(oDevice1)) {
        boolean bAndroidServer = mConnect2Device_Sub(UUID_ANDROID_INSECURE, oDevice1);
        mMsgLog("ANDROID is connecting170922");
        return bAndroidServer;
    } else if (mIsClassic(oDevice1)) { ;//
        boolean bLMConnection = mConnect2Device_Sub(UUID_LMDEVICE_INSECURE, oDevice1);
        mMsgLog("Classic bluetooth 170922");
        return bLMConnection;
    }
    mErrMsg("TF should never get here 170929");
    return false;
} //mConnect2Device   Connect to either device or an android as a client

//Caller: mConnect2Device->mConnect2Device_Sub
    private boolean mConnect2Device_Sub(UUID uuidLmdeviceInsecure, final BluetoothDevice oDevice){      //Experiment with a thread
        boolean bResult = false;
        int nAttempts;
        try {       //Make a socket for the connection 170929
            if (oSocket!=null) oSocket.close();
            oSocket = oDevice.createInsecureRfcommSocketToServiceRecord(uuidLmdeviceInsecure);
        } catch (IOException e) {
            mErrMsg("Socket Type:insecure " + "listen() failed" + e.toString());
        }
        for (nAttempts=0;nAttempts<2;nAttempts++) {
            //Wait a second before trying to connect
            if (nAttempts>0) mSleep(500);
            try {
                //mMsgLog("Connecting to " + oDevice.getName() + " Attempt#: "+ nAttempts);
                oSocket.connect();
                bResult = mConnect_StreamsInit(oSocket);
                if (bResult) {
                    mMsgLog("Connection success");
                    mStateSet(cKonst.eSerial.kBT_Connected);
                    return bResult;                                         //Exit point
                }
            } catch (IOException e) {
                bResult = false;

                mStateSet(cKonst.eSerial.kBT_TimeOut);
                return bResult;
            }
        }
        mStateSet(cKonst.eSerial.kBT_TimeOut);
       // mErrMsg("Timeout in connection mConnect2Device_Sub ");
        return bResult;
    } //Make the serial connection

    private boolean mIsClassic(BluetoothDevice oBTDevice) {
        return (oBTDevice.getType()== BluetoothDevice.DEVICE_TYPE_CLASSIC);
    }

    //Chain: mConnect2Device_Sub->mConnect_StreamsInit
    private boolean mConnect_StreamsInit(BluetoothSocket oSocket) {  //170905    Set input/output ports
        if (oSocket.isConnected()) {
            try {
                oOutput = oSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                mMsgDebug("170905");
                return false;
            }
            try {
                oInput = oSocket.getInputStream();
            } catch (IOException e) {
                mMsgDebug("170905B");
                e.printStackTrace();
                return false;
            }
            return true;
        }else {
            return false;
        }

    }

    public boolean mIsConnected() {
        if (oSocket==null) return false;
        return oSocket.isConnected();
    }

    public cKonst.eSerial mConnectionState(){
        //to obsolete by nState_Serial=eState.kConnectionError;
        int i= oBTadapter.getState();
        if(i==STATE_DISCONNECTED)       mStateSet(cKonst.eSerial.kDisconnected);
        if(i==STATE_OFF)                mStateSet( cKonst.eSerial.kDeviceNotFound);
        return mStateGet();
    }       //obsolete method

    private boolean mPair(BluetoothDevice oBTDevice) {
        boolean bRes=false;
        if (oBTDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            Method method = null;
            try {
                //method =BluetoothDevice.class.getDeclaredMethod("createBond", null);
                method =BluetoothDevice.class.getDeclaredMethod("createBond");
                bRes=true;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            try {
                boolean result = (Boolean) method.invoke(oBTDevice);
                mMessage("pairing bluetooth:"+oBTDevice.getName());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }else{
            bRes=true;
        }
        return bRes;
    }



    //  PRIVATE Helpers
    private void BroadcastReceiver_mEventListener() {     //170922     catching events on bluetooth using mReceiver1
        Context AC = mContext.getApplicationContext();
        AC.registerReceiver(mReceiver1,new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        AC.registerReceiver(mReceiver1,new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        AC.registerReceiver(mReceiver1,new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED));    //170922experimental
        AC.registerReceiver(mReceiver1,new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));    //170922experimental
        AC.registerReceiver(mReceiver1,new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        AC.registerReceiver(mReceiver1,new IntentFilter(BluetoothDevice.ACTION_FOUND));
        AC.registerReceiver(mReceiver1,new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        AC.registerReceiver(mReceiver1,new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        AC.registerReceiver(mReceiver1,new IntentFilter(ACTION_DEVICE_SELECTED));
    }
    private final BroadcastReceiver mReceiver1 = new BroadcastReceiver() {
        public BluetoothServerSocket server;
        public ParcelUuid[] aClientUUIDs;
        @Override
        public void onReceive(Context context, Intent intent) {
            nSemaphore++;
            String action = intent.getAction();
            BluetoothDevice oNewDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (oNewDevice!=null)
                sRemoteDeviceName = oNewDevice.getName();
            if (ACTION_DEVICE_SELECTED.equals(action)){                 //Return from device picker
                mSelectNewDevice(intent);
                mStateSet(cKonst.eSerial.kDevicePickerClosed);
            }else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                mStateSet(cKonst.eSerial.kDevicePickerActive);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {  //The device probably switched off
                mStateSet(cKonst.eSerial.kDevicePickerClosed);                  //So you know the device picker has closed
            }else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                if (mIsAndroidDevice(oNewDevice))       //Enable relaying to an android client
                if (sRemoteDeviceName.equalsIgnoreCase(oDevice.getName())){ //This application connects (not a client)
                    mMessage(sRemoteDeviceName +" is connecting");}
               else {  //-! uncomment next
                   if(mStartServerService()) //Start listening for a client
                       mMessage(sRemoteDeviceName +" is connecting as Client");
               }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {  //The device probably switched off
                mEvent_Disconnect(sRemoteDeviceName);
            }else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                mErrMsg("State changed  "+action.toString());
            } else {
                // When discovery finds a device
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, oBTadapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            mMsgDebug("BT adapter is in OFFSTATE");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            mMsgDebug("Bluetooth adapter is switching off");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            mMsgDebug("mBroadcastReceiver1: Bluetooth is ON");
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            mMsgDebug("mBroadcastReceiver1: Bluetooth is TURNING ON");
                            break;
                    }
                } else  if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                    BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){                    //case1: bonded already
                        mMsgDebug( "BroadcastReceiver: BOND_BONDED.");
                    }
                    //case2: creating a bone
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        mMsgDebug("BroadcastReceiver: BOND_BONDING.");
                    }
                    //case3: breaking a bond
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        mMsgDebug("BroadcastReceiver: BOND_NONE.");
                    }
                } else {
                    mErrMsg("Unimplemented action "+action.toString());
                }
            }
            nSemaphore--;
        };
    };

    private void mEvent_Disconnect(String sRemoteDeviceName) {
        if (sRemoteDeviceName==oDevice.getName()){
            mStateSet(cKonst.eSerial.kBrokenConnection);
            mProtocolStateSet(cKonst.eProtState.kConnectionError);
            bDoRedraw=true;
        }
        mMsgDebug( sRemoteDeviceName + " Disconnected");
    }

    //***********************RELAY METHODS

    public boolean mIsRelayConnected() {
        if (oBTServer==null) return false;
        return oBTServer.mIsConnected1();
    }

    private void mSelectNewDevice(Intent intent) {      //From broadcastreceiver
        BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        boolean bRes = mPair(newDevice);
        if (bRes) {
            oParent.mSetDeviceName(newDevice.getName());
        }
        mProtocolStateSet(cKonst.eProtState.kBT_ConnectReq);
        mStateSet(cKonst.eSerial.kTryToConnect);                //We assume a connection has been made
    }

}

