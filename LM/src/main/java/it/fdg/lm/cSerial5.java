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

import static it.fdg.lm.cAndMeth.mSleep;
import static it.fdg.lm.cKonst.eSerial.kBT_ConnectReq1;
import static it.fdg.lm.cKonst.eSerial.kBT_Connected1;
import static it.fdg.lm.cKonst.eSerial.kBT_Connecting;
import static it.fdg.lm.cKonst.eSerial.kBT_DevicePickerActive;
import static it.fdg.lm.cKonst.eSerial.kBT_Disconnected;
import static it.fdg.lm.cKonst.eSerial.kBT_InvalidBT1;
import static it.fdg.lm.cKonst.eSerial.kBT_InvalidDevice1;
import static it.fdg.lm.cKonst.eSerial.kBT_Undefined;
import static it.fdg.lm.cKonst.eSerial.kOverflow;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.mMsgDebug;
import static it.fdg.lm.cProgram3.mMsgLog;

public class cSerial5 {
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
	cKonst.eSerial nState_Serial;
	    // SPP UUID service unique numbers identification for connections
    private static final UUID UUID_ANDROID_INSECURE =UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");       //Client
    private static final UUID UUID_LMDEVICE_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //LM Device
		
    public int nDataRcvd=0;            //Number of bytes received
    public int  nSndCtr=0;

    private String sRemoteDeviceName;

// !- stuff to cancel?
    static int nSemaphore=0;
    private String sDeviceName;
    private cKonst.eSerial nRelayState;

    /*********************************************              IMPLEMENTATION          /**********************************************/
    public cSerial5(cProtocol3 owner) {
        oParent=owner;
    }   //Constructor

    public void mInit(String sId) {
        oRXFIFO =new cFIFO(cFIFO.kFIFOSize);
        oTXFIFO =new cFIFO(cFIFO.kFIFOSize);
        String sMyId = sId;
        mBTOpen1();                             //Open the bluetooth
        mContext= cProgram3.mContext;
        BroadcastReceiver_mEventListener();     //Activate listening to BT events
    }   //Initialization

    //BT Actions
    //R170315	Manage serial communication using the FIFO buffers
    public synchronized void mProcessSerial(){               //170927 Returns array of data received from serial RX they are also put on receive buffer
        for (int loop=0;loop<oTXFIFO.nBytesAvail;loop++){
            if (oTXFIFO.mCanPop(1)==false) break;                //Send FIFO data to serial output stream
            int byB=oTXFIFO.mFIFOpop();
            mWriteByte(oOutput, (byte) byB);

        }
        byte[] aBytes = mReadBytes(oInput);
        if (aBytes==null) return ;
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
    }       //This is the worker method
    //State register
    private void mStateSet(cKonst.eSerial nNewState) {
        if (nState_Serial!=nNewState)
            bDoRedraw=true;     //Something has changed redraw controls
        nState_Serial=nNewState;
    }

    public cKonst.eSerial mStateGet() {
        return nState_Serial;
    }

    public boolean mIsState(cKonst.eSerial nCheckState) {
        return nCheckState==nState_Serial;
    }

    public boolean mBTOpen1() {     //Returns true if BT is ready
        if (oBTadapter == null){    //Prepare BT
            return mBTInit1();
        }else if (oBTadapter.isEnabled()) {     //BT port is ready      , Running return
                return true;
        }else {                                  //Try to open BT port
            isOpenedByMe = true;
            oBTadapter.enable();  //Turn on bluetooth if not already active
            mSleep(1000);
            if (oBTadapter.isEnabled()) {
                return true;
            }
        }
        return false;
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
        mDisconnect();
        if (oBTadapter != null) {
            if (oBTadapter.isEnabled()) {
                if (isOpenedByMe)       //170905 only if this application opened the bluetooth
                    oBTadapter.disable();
            }
        }
        if (mReceiver1!=null)
            try {           //Protection from crash bugfix 171013
                mContext.unregisterReceiver(mReceiver1);
            }
            catch (final Exception exception) {
                // The receiver was not registered.  There is nothing to do in that case.  Everything is fine.
            }
    }



    private void mWriteByte(OutputStream oOutput,byte out)    {

        if (oOutput!=null)      //Connection may have been interrupted
        {
            try {       //Maybe the steam was closed by receiver
                oOutput.write(out);
                mSleep(1);      //Don't send too fast
            } catch (IOException e) {
                mErrMsg("Connection lost " + oDevice.getName());
                mStateSet( cKonst.eSerial.kBT_BrokenConnection);
                 mDisconnect();
            }
        }
    }  //Write data to client safely
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

    public int mReadByte2(InputStream oI) {        //To refactor into cProtocol 170929
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
        mStateSet(kBT_InvalidDevice1);
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

    public boolean mBTInit1(){
        //Initialize the bluetooth adapter
        if (oBTadapter !=null) return true ;
        oBTadapter = BluetoothAdapter.getDefaultAdapter();
        return (oBTadapter != null);
    }       //Init the BT device and returns true on succss



    //Chain: mAsyncProcessing_Work->mBT_PickDevice..wait for user ...->receiver->onReceive
    public boolean mBT_PickDevice2(){   //Will change sRemoteDeviceName
        if (oBTadapter==null) return false;
        mStateSet(cKonst.eSerial.kBT_DevicePickerActive);
        oBTadapter.startDiscovery();
        {
            Intent intent = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
            mContext.startActivity(intent); //startActivityForResult
        }
        mSleep(1000);
        return true;
    }        //Find new devices  (discover)

    protected boolean mStartServerService() {//170926    Start listening for a client to relay to (170926
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


/*---------------------------------------------------------------------------------------*/
public boolean mConnect(String s) {        //Find and mConnect to the bluetooth
        sDeviceName=s;
        mStateSet(kBT_ConnectReq1);
        if (mBTOpen1()==false) {            //If BT cannot be opened
            mStateSet(kBT_InvalidBT1);
            mErrMsg("No bluetooth port available");
            return false;
        }
        if (mIsState(kBT_Connecting)) {
            mSleep(1000);
            if (oInput!=null)
                mStateSet(kBT_Connected1);
            else
                mStateSet(kBT_InvalidDevice1);
                return false;
        }
        if (mIsState(kBT_ConnectReq1))               //Get the device object
            oDevice=mBTDeviceByName(sDeviceName);
        else if (oDevice==null) {                        //Device not found,
            oDevice=mBTDeviceByName(sDeviceName);
            if (oDevice==null){
                mStateSet(kBT_InvalidDevice1);
                mSleep(2000);
            }
            return false;
        }
        //set by mConnectDeviceWithName, mRequestConnection
        if (mIsState(kBT_ConnectReq1)) {     //Try to connect first time
            if (mConnect2Device1(oDevice)==false)       //mConnect sets      mStateSet(kBT_DeviceConnected);
                mSleep(10);
        } else if (mIsState(cKonst.eSerial.kBT_BrokenConnection)) {     //Try to reconnect
            mSleep(2000);
            bDoRedraw=true;
            return false;
        } else if (mIsState(kOverflow)){            //Fatal, overflow, donno what to do
            mConnectionStateClear();
            return false;
        } else {
            mSleep(2000);       //Do nothing while connection was interrupted
        }
        return false;
    }

    public void mDisconnect() {       //Disconnect connection if there was one
        try {
            if (oOutput!=null){
                oOutput.close();
                oOutput=null;}
            if (oBTServer!=null)
                oBTServer.mCloseService2(); //Disconnect a relayed device
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

    private BluetoothDevice mBTDeviceByName(String sNewDeviceName) {       //Return the device having the name
        if (oDevice == null) {
            Set<BluetoothDevice> pairedDevices = oBTadapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                   if (cFunk.mTextLike(sNewDeviceName,device.getName())){
                        oDevice=device;
                        return device;
                    }
                }
            }
        } else {
            if (cFunk.mTextLike(sNewDeviceName,oDevice.getName())) {
                return oDevice;
            }
        }
        return null;
    }


    private boolean mIsAndroidDevice(BluetoothDevice oDevice1) {
        if (oDevice1==null) return false;
        return (oDevice1.getBluetoothClass().getDeviceClass()== Device.PHONE_SMART);
    }

    private boolean mConnect2Device1(BluetoothDevice oDevice1){
        // connect the BT and set the  _DeviceName
        oDevice=oDevice1;
        mStateSet(kBT_Connecting);                 //Connecting to a server
        if (mIsAndroidDevice(oDevice1)) {
            if (mConnect2Device_Sub(UUID_ANDROID_INSECURE, oDevice1)) {
                mStateSet(kBT_Connected1);
                mMsgLog("ANDROID Connected: " +oDevice1.getName());
                return true;
            }
        } else if (mIsClassic(oDevice1)) {          //  Classic bluetoot device
            if ( mConnect2Device_Sub(UUID_LMDEVICE_INSECURE, oDevice1)) {
                mStateSet(kBT_Connected1);
                mMsgLog("Classic bluetooth Connected: " +oDevice1.getName() );
                return true;
            }
        }
    mMsgDebug("Could not connect");
        mBTClose1();
    return false;
} //mConnect2Device   Connect to either device or an android as a client

    private void mRelayStateSet(cKonst.eSerial newState) {
        nRelayState=newState;
        oBTServer.mStateSet( newState);
    }

    //Caller: mConnect2Device->mConnect2Device_Sub
    private boolean mConnect2Device_Sub(UUID uuidLmdeviceInsecure, final BluetoothDevice oDevice){      //Experiment with a thread
        boolean bResult = false;
            try {       //Make a socket for the connection 170929
                if (oSocket!=null) {oSocket.close();mSleep(1000);}
                oSocket = oDevice.createInsecureRfcommSocketToServiceRecord(uuidLmdeviceInsecure);
                mSleep(300);
            } catch (IOException e) {
                mErrMsg("Socket Type:insecure " + "listen() failed" + e.toString());
            }
            if (oSocket==null) return false;
            try {
                mMessage("Searching " + oDevice.getName() );
                oSocket.connect();
                bResult = mConnect_StreamsInit(oSocket);
                if (bResult) {
                    mMessage(oDevice.getName()+" Connected");
                    return true;                                         //Exit point
                }
            } catch (IOException e) {
                mMessage(oDevice.getName()+" Timeout");
                mStateSet(cKonst.eSerial.kBT_TimeOut);
            }
        mStateSet(cKonst.eSerial.kBT_TimeOut);
        return false;
    } //Make the serial connection

    protected boolean mIsClassic(BluetoothDevice oBTDevice) {
        if (oBTDevice==null) return false;
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
            }else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
// has nothing to do with device picker
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {  //The device probably switched off
                    //This does NOT mean the device picker has closed
            }else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                if (mIsAnotherDevice(oNewDevice)==false)    //
                    mMsgDebug("Myself");
                else if (mAsClient(sRemoteDeviceName))       //Connecting to sDeviceName as client
                    mStateSet(kBT_Connected1);
                else if (mIsAndroidDevice(oNewDevice))
                    if(mStartServerService()) //Start listening for a client
                       mMessage("Starting listening for "+sRemoteDeviceName +" as server");
                    //Enable relaying to an android client
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {  //The device probably switched off
                mEvent_Disconnect(sRemoteDeviceName);//https://stackoverflow.com/questions/9537833/what-triggers-the-bluetoothdevice-action-acl-broadcasts
            } else {
                // When discovery finds a device
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, oBTadapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
    //                        mMsgDebug("BT adapter is in OFFSTATE");
                            mStateSet(kBT_Disconnected);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            //"Bluetooth adapter is switching off");
                            mStateSet(kBT_Disconnected);
                            break;
                        case BluetoothAdapter.STATE_ON:
                           // mMsgDebug("mBroadcastReceiver1: Bluetooth is ON");
                            mRequestConnection();
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            mMsgLog("mBroadcastReceiver1: Bluetooth is TURNING ON");
                            mStateSet(kBT_ConnectReq1);
                            break;
                    }
                } else  if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                    BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){                    //case1: bonded already
                        mSelectNewDevice(intent);  mMsgDebug( "BroadcastReceiver: BOND_BONDED.");
                    }
                    //case2: creating a bone
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        mMsgDebug("BroadcastReceiver: BOND_BONDING.");
                    }
                    //case3: breaking a bond
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) { //Bonding was not completed
                        mStateSet(kBT_Disconnected);mMsgDebug("BroadcastReceiver: BOND_NONE.");
                    }
                } else {
                    mErrMsg("Unimplemented action "+action.toString());
                }
            }
            nSemaphore--;
        };
    };

    private boolean mIsAnotherDevice(BluetoothDevice oNewDevice) {
        if (oNewDevice==null)
            return false;
        else if (cFunk.mTextLike(oBTadapter.getName(),oNewDevice.getName())) { // MYSELF
            return false;
        }
        return true;
    }

    private void mEvent_Disconnect(String sRemoteDeviceName) {
        if (oDevice==null) return;
        if (mAsClient(sRemoteDeviceName))  {
            if (mIsState(kBT_Connected1)){
                mMsgDebug( sRemoteDeviceName + " Disconnected");
                mStateSet(cKonst.eSerial.kBT_BrokenConnection);
            } else {
                mMsgDebug( sRemoteDeviceName + " Disconnected");
            }
            bDoRedraw=true;
        } else if (mAsServer(sRemoteDeviceName)){       //Client disconnected
            oBTServer.mCloseService2();
            mStartServerService();

        }
    }

    private boolean mAsServer(String sRemoteDeviceName) {       //Returns true if was server for a client
        if (oBTServer==null ) return false;
        String s = oBTServer.sClientName;
        return sRemoteDeviceName.equals(s);
    }

    private boolean mAsClient(String sRemoteDeviceName) {
        return   (sRemoteDeviceName.equals(sDeviceName));
    }

    //***********************RELAY METHODS

    public boolean mIsRelayConnected() {
        if (oBTServer==null) return false;
        return oBTServer.mIsConnected1();
    }

    protected void mSelectNewDevice(Intent intent) {      //From broadcastreceiver mReceiver1
        BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (newDevice.getBondState()==BluetoothDevice.BOND_BONDED) {
            mConnectNamedDevice(newDevice.getName());   //            mStateSet(kBT_ConnectReq1);
        }else if (newDevice.getBondState()==BluetoothDevice.BOND_BONDING){
            mStateSet(kBT_DevicePickerActive);
        }else if (newDevice.getBondState()==BluetoothDevice.BOND_NONE){
            mPair(newDevice);                                       //DO the bonding
            mStateSet(kBT_DevicePickerActive);
        }else{
            mStateSet(kBT_Undefined);       //never gets here
        }
    }

    public boolean mIsConnectionError() {
        if (mIsState(cKonst.eSerial.kBT_BrokenConnection)) return  true;
        if (mIsState(cKonst.eSerial.kConnectionError)) return  true;
        return false;
    }

    public String mDeviceNameGet() {
        return sDeviceName;
    }

    public void mConnectNamedDevice(String s) {
        oParent.mDeviceNameSet(s);
    }

    public void mRequestConnection() {
        mStateSet(kBT_ConnectReq1);
    }

    public boolean isConnected() {
        return mIsState(kBT_Connected1);
    }
}

