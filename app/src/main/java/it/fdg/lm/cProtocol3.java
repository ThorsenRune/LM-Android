//170928  see server relay methods at the end
//Doc:  https://docs.google.com/document/d/18xL6H-XLR2rr2MjD_jXzKkW_k6YwTHRbHmcLJX7LdLU/edit
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//  Implementing relay in version 5

package it.fdg.lm;

import android.content.Context;

import java.io.InputStream;
import java.io.OutputStream;

import static it.fdg.lm.cAndMeth.mSleep;
import static it.fdg.lm.cFunk.mInt2ByteArray;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.oFocusdActivity;
import static it.fdg.lm.cProgram3.sDevices1;
import static it.fdg.lm.cSerial5.eState.kBrokenConnection;
import static it.fdg.lm.cSerial5.eState.kConnected;
import static it.fdg.lm.cSerial5.eState.kConnectionError;

/*--------------------------------------------------------------
*  Main processing communication with device through the protcol
*  */
public  class cProtocol3 {                      //This was formerly just called protocol, renamed to cDevice
    private final int kProtSizeLimit =30;                               //Maximum protocol size
    public  cProtElem[] oaProtData=new cProtElem[kProtSizeLimit];     //Elements in this protocol
    private  int nProtSize;            //The size of the protocol i.e. the number of variables exposed  nProtSize private int  int nProtSize;          //The size of the protocol i.e. the number of variables exposed  nProtSize   (DONT use .nDataLength since it will be full of null elements
    //  IO Connection
    public cSerial5 oSerial;                //      The serial communication class
    public cFIFO oRXFIFO;    // RX circular buffer identical to cSerial5.oRXFIFO
    public cFIFO oTXFifo;    // TX do

    //      Finite state machine of the dispatcher
    public int nCmd[]=new int [1];      //Primary FSM pointer. (Dispatcher)
    public int zState[]=new int [1];    //Secondarey FSM pointer
    private cProtElem oNewElement;  //A temporary new element when collecting elements from devices
    private int nServerState1= kReady2; //State of the server FSM
    //Flag for initializing protocol mTX_Dispatch:kCommInit
    //{kCommInit, kReady,kErrMsg} meaning: {reset the protocol, protocol is ready to be used, protocol is not initialized and not ready}
    public enum eProtState{
        kProtResetReq,             //Do a reset of the protocol
        //We are waing for device to send packs exposing the protocol
        kProtInitInProgress,                   //Request for reset of the protocol, kCommInit will be sent to device
        kProtInitDone,                 //Protocol is loaded and ready to mStartListening
        kDoConnect1,                 //Request to mConnect to device
        //The protocol has just been filled with exposed variables and is ready to use
        kProtInitFailure,
        //Error in protocol initialization
        kProtUndef,
        //The protocol is undefined and cannot be used
        kProtReady, //Protocol is readyto use
        kProtTimeOut,
        kBTUnavailable, kProtError,
        kBTDoDiscover,kBTDiscoverInProgress,kBTDiscoverDone,
        kDoConnect_step2,
        kRelay, kConnected1, kConnectionError;                                 //Do pairing
        int a=12;
    }

    //    Communication protocol headers(commands) shared with device Firmware
    public static final int kReady2=1;    //communication FSM is ready for a new pack
    static final int kError= 13;      //Indicates an error (renamed from kErrMsg il 161202)
    static final int kHandshake=15;      // Waiting for the protocol to settle
    static final int kCommInit = 101;    // Request to initialize Protocol, send request to target (R170314)
    static final int kGetReq = 111;           //'Request to read variable, this will increase a pointer in the target causing a send of the variable
    static final int kSetReq = 102;     //'Host write to device memory
    static final int kDSP2HostData = 201;   //'Response to DSP2HostCmd with data from device
    static final int kByte = 208;
    static final int k24Bit = 224;
    static final int k32Bit = 232;
    //Constants for the nProtState see https://docs.google.com/document/d/1Kf35UnrTbiNKoWXiDjztb_g7Ug1J8Y5BL0DyZphhK-g/edit#heading=h.hm1crmoyhzal
    //170316   Constants for the sub FSM of protocol package reception
    private final int   kStateNameString    =2002;         //Enumerators for the FMS states
    private final int   kStateVarID         =2003;
    private final int   kStateArrayLenght   =2004;
    private final int   kStateArrayType     =2005;
    private final int   kStateGetArrayData  =2006;         //Enumerators for the FMS states
    private final int   kStateGetDataLength =2007;         ////+161216
//END     Communication protocol headers(commands)

    //     We need to declare a static object to hold the incoming data
    static cProtElem oRXGetReqElem;           //Currently processed element in oRXGetReqElem  161205/rt
    private int nProcessCycleCount=0; //A 'timer' for the number of times the protocol is processed. incremented in mUIProcess
    private eProtState nProtState; //{kCommInit, kReady,kErrMsg,kUndefined} meaning

    private int k32BitInt= 232;
	//			PROPERTIES
    public  Context mContext;
	private String myId;              //Identifier for the device protocol
    private int myIndex=0;              //Index in the array of protocols

    //	!- Deletable debug rubbish
	public int nTimeOut=0;          //170323    for each successful receive nTimeOut will reset to kTimeOut
    private int nProcess2onlyonce;

	//******************************			IMPLEMENTATION     ************************
    public void mInit(int nClassIndex) {//Initializer/Constructor
        myIndex=nClassIndex;
        oSerial=new cSerial5(this);
        oSerial.mInit("Serial"+myIndex);
        oRXFIFO =oSerial.oRXFIFO;
        oTXFifo =oSerial.oTXFIFO;
        oaProtData=new cProtElem[kProtSizeLimit];      //Reserve room for the protocol elements
        for(int i=0;i<oaProtData.length;i++){               //Initialize elements
            oaProtData[i]=new cProtElem(this);
            oaProtData[i].nIndexInContainer=i;
        }

    }
    public void mEnd(){
        oSerial.mBTClose1();             //Close ports when program quit
    }
    public  void mPersist_Protocol(boolean bGet){//Load/mFileSave settings for the protocol defined by device
        /* Call:mPersistAllData->mPersist_Protocol          */
        for(int i = 0; i< nProtSize; i++){
            oaProtData[i].mSettings(bGet);      //Get settoings for sKey=sVarName;
        }
    }       //Set/Get persistent data
    //******************************			PROPERTIES     ************************
            //      *** REQUESTS ***
    public void mDoReset() {
        mSetState(eProtState.kProtResetReq);
    }
    public void doBTPair() {
        mSetState(eProtState.kBTDoDiscover);
    }
    public void mDoConnect(String sDevice) {
        if (sDevice!=null)   mSetDeviceName(sDevice);
        if (oSerial.mIsDeviceResponding_Dodgy()){
            mDoReset();
        } else {
            mSetState(eProtState.kDoConnect1);
        }
    }                       //Request a connection

            //     *** GETTERS SETTERS  ***
    public void mSetProtSize(int nNewSize) {     //Testing only, not for use
        nProtSize =nNewSize;
    }
    public int mGetProtSize(){        //Don't use oaProtData.nDataLength because it has null elements
        return nProtSize;
    };

    public String mGetConnectMenuText() {
        if (oSerial.mIsDeviceResponding_Dodgy()){
            return "->"+ mGetDeviceName();
        }
        return "Connect:"+ mGetDeviceName();
    }       //Returns a text to display on the menu

    public boolean mIsConnected() {
        return oSerial.mIsConnected();
    }

    //Chain: cSerial...mFindDevices->oParent.mSetDeviceName
    public void mSetDeviceName(String name) {    //  Getter setter for the portname
        sDevices1[ myIndex]=name;
        cProgram3.mPersistAllData(false);
        mDoConnect(name);

    }       //Set the name of the BT device (todo clean codesmell

    private String mGetDeviceName() {
        return sDevices1[myIndex];
    }




    //  Processing in background thread the serial communications
    public void mAsyncProcessing_Call() {  //A thread connecting to the device
        new Thread(new Runnable() {
            public synchronized void run() {
                Thread.currentThread().setName("mAsyncProcessing_Call");
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                nProcess2onlyonce=nProcess2onlyonce+1;
                if (nProcess2onlyonce<2) {
                    mProcess_Async();
                    cProgram3.bDoRefresh=true;
                }
                nProcess2onlyonce--;
            }
        }).start();         //Important. actually starts the process
    }
    private void mProcess_Async() {
        if (getState()== eProtState.kProtReady){//Protocol statemachine
            mRelay();       //Relay old data, get changes from client
            boolean ret = mTX_Dispatch();      //170605    Rewritten to process only datarequests
            oSerial.mProcessSerial();    //Send the request immediately
            mDispatchRX(nCmd, zState);
            if (mIsConnected()==false) { //Appears that communication was closed
                mSetState(eProtState.kProtUndef);    //Maybe we could retry in some time (ToDo)
            }
        } else if (getState()== eProtState.kDoConnect1) {
            if (oSerial.mBTOpen()) {              //Make sure we have opened the port
                boolean ret = oSerial.mConnect(mGetDeviceName());            //Connect to the device
                if (ret)
                    mSetState(eProtState.kProtResetReq);
                else
                    mSetState(eProtState.kConnectionError);
            }
        } else if (getState()== eProtState.kProtResetReq) {    //Request the protocol setup
            mTX_ProtReset();
            oSerial.mProcessSerial();    //Send the request immediately
            mSleep(100);                //Now device will start sending data
            mSetState(eProtState.kProtInitInProgress);      //Idle mode awaiting initialization of the protocol
        } else if (getState()== eProtState.kProtInitInProgress) {
            for (int i=0;i<100;i++) {        //Retry until success       (you are in background)
                if (oSerial.mProcessSerial() == null) {    //When no more data, then protocol should be ready
                    mMessage("Initializing protocol - "+ mGetProtSize() );
                    mSetState(eProtState.kProtInitDone);    //Protocol good to use
                    bDoRedraw=true;
                    return;
                }
                mSleep(100);                   //Data keep coming when initialization has been requested
            }
            mSetState(eProtState.kProtTimeOut);     //Error in receiving protocol   timed out
            mErrMsg("Timeout in protocol received");
        } else         if (getState()== eProtState.kProtInitDone) {
            mSetState(eProtState.kProtReady);   //Now the UI knows the protocol is ready  so display can be refreshed
        } else        if (getState()== eProtState.kBTDoDiscover){
            oSerial.mFindDevices((Context) oFocusdActivity);
            mSetState(eProtState.kBTDiscoverInProgress);
        } else if (getState()== eProtState.kBTDiscoverInProgress){
            if (oSerial.mConnectionState()== cSerial5.eState.kTryToConnect)   //Pairing has been done
                mDoConnect(mGetDeviceName());                           //Now try to mConnect
         } else if (getState()== eProtState.kDoConnect_step2)  {
            if (oSerial.nSerialState== kConnected) {
                mSetState(eProtState.kProtResetReq);
            }
            else if (oSerial.nSerialState == kConnectionError) {
                mSetState(eProtState.kBTUnavailable);
            }
        }
        else if (oSerial.nSerialState == kBrokenConnection) {
            mSleep(5000);       //Do nothing while connection was interrupted
        }
        else if (oSerial.mConnectionState()== cSerial5.eState.kOverflow){
            mReset_Buffers();
            oSerial.mConnectionStateClear();
        }
    }

    //mProcess_Async->mRelay
    public void mRelay() {      //Relay data to a client
        if (oSerial.mIsRelayConnected()) {       //Check for valid object
            for (int i=0;i<1000;i++) {           //Read for some time if possible
                boolean ret=mDispatchRXClient(oSerial.oBTServer.oInputStream);  //Includes transmission
                if (ret==false)  return;        //break out of loop when done
            }
            return;
        }
    }

/*  (De)Codign of commuication packages
*    //-----------------------------------mTX_Dispatch--------------------------------------------------------------------
*   R170316   revision to use new FSM constants for the protocolstate
*   R170314   some bugfixes, separating protocol states in requesto and acknowledge receipt
*   rev:161203/RT                    refactored to take the state as by reference
*   +170601  returns true if data are sent and some response is expected
*/
    private boolean mTX_Dispatch(){                 //Send requests for data
        boolean bRet=false;                         //Test if some response is expected
        for(int i = 0; i< nProtSize; i++) {           //Loop the Protocol objects to see if there are actions to take
            cProtElem oElem = oaProtData[i];
            if (oElem.mIsSetRequest()) {    //There are data to set in devices
                mTX_SetReq(oElem);
                bRet=true;
            } //Don't read while writing
            else if (oElem.mHasGetRequest()) {        //Request data from device
                mTX_GetReq(oaProtData[i]);
                bRet=true;                      //Expect a response
            }
        }
        return bRet;            //return whether to expect a response or not
    }           //Send  packs to  device
    //Chain: mProcess_Async->mDispatchTX_ProtReset               Reset the protocol
    private boolean mTX_ProtReset(){
        mReset_Buffers();
        //Send request to initialize the protocol
        oTXFifo.mFIFOpush((byte) kCommInit);  //Send the reset request to device
        // 170822  since protocol can be preloaded this has been removed  nProtSize =0;                     //Reset protocol to zero elements
        return true;            //expect a response
    }

    private void mTX_GetReq(cProtElem oProtElem){        //TX Get request
        oTXFifo.mFIFOpush((byte) kGetReq);    //+161216   BUGFIX: corrected constant
        oTXFifo.mFIFOpush((byte) oProtElem.nVarId);
    }

    private void mTX_SetReq(cProtElem oProtData){
      /* Change data in device memory          Write data to device memory*/
        int offs;
//-161216   bugfix    if (oProtData.nSetReqCntr<1) return;             //[] make the set request flag
        //-161216  bug remove line.  done by caller oProtData.nSetReqCntr=(byte) (oProtData.nSetReqCntr -  1);        //Decrease the write counter
        //Loop through object data elements
        for(offs=0; offs<oProtData.nDataLength(); offs++)  //+161216 Bugfix
        {
            oTXFifo.mFIFOpush((byte)kSetReq);                      //command
            oTXFifo.mFIFOpush((byte) oProtData.nVarId);             //array identifier
            oTXFifo.mFIFOpush((byte) offs);                         //offset in array
            // [] to implement the case for different datatypes k32bit
            byte b[]= mInt2ByteArray(oProtData.mDataPeek(offs));
            //+161216   bugfix push high to low bytes
            oTXFifo.mFIFOpush(b[0]);        //value of the array[offset]
            oTXFifo.mFIFOpush(b[1]);        //value of the array[offset]
            oTXFifo.mFIFOpush(b[2]);        //value of the array[offset]
            oTXFifo.mFIFOpush(b[3]);        //value of the array[offset]
            oProtData.mSetRequest(-1);
        }
    }
    //              --------------------------       RX Dispatcher  -----------------------------------
    public void mDispatchRX(int[] nCmd, int[] zState){                   //Rev 161202/RT
        for (int loop=0;loop<200;loop++){         //!+       refactor  canpop to take the minimum number of bytes avaiable for popping
            if (oRXFIFO.mCanPop(1)==false) return;
            switch (nCmd[0]) {
                case kReady2:
                    //Ready to accept a new command?
                    nCmd[0]= oRXFIFO.mFIFOpop();
                    break;
                case kError:
                    mErrMsg("Error in protocol");
                    mSetState(eProtState.kProtError);
                    nCmd[0]=kReady2;
                    break;
                case kCommInit:        //A protocol initialization element is to be received
                    //Call initialization of the protocol statemachine see here DEVICE:mTX_Dispatch:kCommInit
                    mRX_ProtInit(nCmd, zState);  //170323
                    break;
                case k32Bit:    //+161216 !!! the response from device is the datatype sent
//                    break;            fall through to kGetReq
                case kGetReq:
                    //Statemachine dispatching the FIFO into the object which data are received
                    if(mRXGetReq(nCmd, zState)==false) return;       //170323 :m DispatchRX:kGetReq
                    break;
                case kSetReq:
                    //   GUIAnd.message1(" mRXSetReq() does not return arguments. to be implemented");
//                    oRXFIFO.mFlush();                    oTXFifo.mFlush();
                    break;
                //Further expansion of the protocol as of here
                case kHandshake:
                    //   GUIAnd.status1("kHandshake");
                    nCmd[0]= oRXFIFO.mFIFOpop();
                    break;
                default:            //r170217 unknown command in protocol

                    nCmd[0]=kReady2;
                    break;
            }
        }

    }   //Send packs to device
//              ***COMMAND PACKINGS OF THE PROTOCOL

    private boolean mRX_ProtInit(int[] nCmd, int[] zState){    //it is called inside mRXdispatch when nCmd=kCommInit
/*     its a FSM therefore it is called with the static state variables using zero lenght array casting
//!+161202     int  zState=0;            zState needs to be static (remebered between calls, therefore it is passed as a parameter)
//!-       int nStringLength=oRXFIFO.mFIFOpop();    //Cant do that yet
// 170822  revision to use a new element and then insert it in existing protocol loaded from disk
*/
        for (int loop=0;loop<200;loop++)        //170927 substituted while with finite loops
        {
            if ( oRXFIFO.mCanPop(1)==false)  return false;     //Be sure we can pop a byte
            if(zState[0]==kReady2){
                oNewElement=new cProtElem(this);        //Make a new protocol element  R170822
                oNewElement.mInit2();                     //Reset its data

                oNewElement.nVarNameLen= oRXFIFO.mFIFOpop();  //Refactor nLengthName to nVarNameLength
                oNewElement.sVarName="";                                  //Initialize sVarName
                //Refactor sName to sVarName
                zState[0]=kStateNameString;                             //Set the next state
            }else if(kStateNameString== zState[0]){
                //Concatenate the variable name with next character
                oNewElement.sVarName=oNewElement.sVarName+(char) oRXFIFO.mFIFOpop();

                //     when sVarName is full proceed to the next state
                if (oNewElement.sVarName.length()>=oNewElement.nVarNameLen)
                {
                    zState[0]=kStateVarID;                           //Set the next state
                }
            }else if(kStateVarID== zState[0]){                   //Get VarId

                oNewElement.nVarId= oRXFIFO.mFIFOpop();
                zState[0]=kStateArrayLenght;                            //Set the next state
            }else if(kStateArrayLenght== zState[0]){           //Get the array size
                oNewElement.nDataLength((int) oRXFIFO.mFIFOpop());   //!!!however should be defined byte
                //oProtElem.aData=new int[oProtElem.nProtSize]; !! nLengthSet should take care of that
                zState[0]=kStateArrayType;                           //Set the next state
            }else if(kStateArrayType== zState[0]){                //Get the Variable type (32 bit integer?)
                //!+161203   todo: declare the nVarType in cProtData
                oNewElement.nVarType= oRXFIFO.mFIFOpop();
                mRXCommInit_AddElem(oNewElement);
                //Reset state machines
                zState[0]   =kReady2;                                       //Process is completed, reset the FSM
                nCmd[0]     =kReady2;                                        //Prepare to receive a new command from device
                return true;                                                   //Exit with success
            }
            else {
                zState[0]=kReady2;
                return false;
            }
        }        //End while
        return false;                   //Haven't finished yet
    }    //!+161203-----------------INITIALIZATION OF PROTOCOL RECEIVING EXPOSED VARIABLES---------

    private void mRXCommInit_AddElem(cProtElem oNewElement) { //Insert a named element in the protocol
        int idx;
        idx= mRXCommInit_Idx4Elem(oNewElement);
        if (idx<0){     //Element not found in protocol, create it
            idx= nProtSize;
            nProtSize = nProtSize +1;  //Increase list of elements
        }
        oaProtData[idx].mCopyElement(oNewElement);
        if (idx>=oaProtData.length-1)
        { mErrMsg("Overflow. Too many elements in protocol");
        }
    }

    private int mRXCommInit_Idx4Elem(cProtElem oE) {
        for (int idx = 0; idx<= nProtSize; idx++){
            if (oaProtData[idx]==oE){
                return idx;
            }
            if (oE.sVarName.equalsIgnoreCase(oaProtData[idx].sVarName)){
                return idx;
            }
        }
        return -1;
    }

    private boolean mRXGetReq(int[] nCmd,int[] nState){
        if (oRXFIFO.mCanPop(1)==false) return false;
        if(nState[0]==kReady2) {
            if (oRXFIFO.nBytesAvail < 2) return false;
            int nVarId = oRXFIFO.mFIFOpop();          //1: byte         Read the var  ID that is coming
            oRXGetReqElem = mProtDataObjById(nVarId);
            int nVarLength = oRXFIFO.mFIFOpop();      //2: byte         Read the var nDataLength  that is coming
            if (bElem_IsValid(oRXGetReqElem) == false)
                return false;
            nState[0]=kStateGetArrayData;
        }
        if (kStateGetArrayData==nState[0]){
            if (!bElem_IsValid(oRXGetReqElem))  { mErrMsg("FE 170929B"); return  false;}
            int nWordLen=0;
            if (k32Bit==oRXGetReqElem.nVarType) nWordLen=4;               //other if statements for other datatypes
            if (oRXFIFO.nBytesAvail<nWordLen*oRXGetReqElem.nDataLength()) return false;
            int value=0;
            for (int i = 0; i<oRXGetReqElem.nDataLength(); i++) {   //+161216
                if (nWordLen == 4) { //Perform bitwise or with the data shifted
                    value = oRXFIFO.mFIFOpop() << 24;
                    value |= oRXFIFO.mFIFOpop() << 16;
                    value |= oRXFIFO.mFIFOpop() << 8;
                    value |= oRXFIFO.mFIFOpop();
                }
                oRXGetReqElem.mDataSet(i,  value);
            }
            nCmd[0]=kReady2;    //Reset FSM
            nState[0]=kReady2;      //Reset statemachine
            oRXGetReqElem.mGetReqDone();             //Decrease the request counter
            return true;                            //  The data has been received
        }
        return false;           //Wait for some more data to complete the kStateGetArrayData           !!! R170221
    } //    RX a Get request, process the received data



    //-------------------------------------------HELPERS ------------------------------------------------------------

    private void mReset_Buffers() {//Chain:mTX_ProtReset->mReset_Buffers
        nCmd[0]=kReady2;             //r170314   initialize FSM to be ready
        zState[0]=kReady2;
        oTXFifo.mFlush();
        oRXFIFO.mFlush();
        oSerial.mFlushInput();           //Flush the buffer
    }           //Clear buffers

    private boolean bElem_IsValid(cProtElem oRXGetReqElem) {
        if (null==oRXGetReqElem){
            mErrMsg("Fatal error ID not found Error in mGETreqRxt!");
            mSetState(eProtState.kProtError);  //Set a request to device for the protocol  R170314
            return false; //this is an error
        }
        return true;
    }       //IS the element valid

    public cProtElem mProtDataObjById(int nVarId){  //Return the protocol data object by ID
        for(int i = 0; i<this.nProtSize; i++){
            if(nVarId==oaProtData[i].nVarId) return oaProtData[i]; //I find oData with the corresponding Id
        }
        return null;       //DataObject not found return null
    }



    public cProtElem mGetElemByName(String sVarName) {    //Returns the data element element by its variable name
        if (sVarName=="") return null   ;
        for (int i = 0; i < nProtSize; i++) {
            if (sVarName.equalsIgnoreCase(oaProtData[i].sVarName)) {
                oaProtData[i].nProtIndex=this.myIndex;
                return oaProtData[i];
            }
        }
        return null;
    }


    public String[] mGetProtElementList(){        //Return a list of element names
        String[] sVarList = new String[this.mGetProtSize()];
        for (int i = 0; i < this.mGetProtSize(); i++) {
            sVarList[i] = oaProtData[i].sVarName;
        }
        return  sVarList;
    } //Getter setter for protocol elements as a list
    public void mSetProtElementList(String[] sElList) {   //Define a protocol by a list of element names
        if (sElList.length<1)
            return;

        nProtSize =   0;
        for (int i = 0; i < sElList.length; i++) {
            if (sElList[i].length()>1) {
                oaProtData[nProtSize].sVarName = sElList[i];
                nProtSize = nProtSize +1;
            }
        }
    }

    //      Get set the status of the protocol, ready, idle etc.
    public eProtState getState() {
        return nProtState;
    }
    public void mSetState(eProtState newState) {      //!!!Refactor to enum
        nProtState=newState;
    }

    void mSendElementData2Client(cRelay2Client oTX, int nVarId){    //Send 32 bit data from element[nVarId].data to client
        cProtElem oElem = mProtDataObjById(nVarId);
        OutputStream os = oTX.oOutputStream;
        int nCount = oElem.nDataLength();
        oTX.mWriteByte(oTX.oOutputStream, k32BitInt); // xmit header cProtocol.h {   k32BitInt=232}
        // all mFIFO_push ===oTX.mWriteByte
        oTX.mWriteByte(os,nVarId);          // xmit VarId
        oTX.mWriteByte(os, nCount);         // xmit Count
        for (int i=0; i< nCount; i++){    //!!!implement overflow check
            int data = oElem.mDataPeek(i);
            oTX.mWriteByte(os, data>>24);       //RT:Send high octet
            oTX.mWriteByte(os, data>>16);       //RT:Send 2 high octet
            oTX.mWriteByte(os, data>>8);        //RT:Send 3 octet
            oTX.mWriteByte(os, data>>0);        //RT:Send low octet
        }
        oElem.mDeviceReadRequest();          //Read from device for next round
    }

    //Client will only be served if it requests data
    boolean mDispatchRXClient(InputStream oInputStream) {  //Receiving data and dispatch commands from client
        if (oSerial.mIsRelayConnected()==false) return false;
        int nAvail = oSerial.nBytesAvailable(oInputStream);
        if (nAvail<1) return false;
        if (nServerState1 == kReady2){
            nServerState1= oSerial.mReadByte2(oInputStream);
        }
        if (nServerState1 == kCommInit) {        //Client requested the protocol
            if (mTX_ProtInitReq(oSerial.oBTServer))				nServerState1 = kReady2;//Now the data are synchronized with client and we can relay all
        } else if (nServerState1==kSetReq){
            if (mTX_SetReq2(oSerial.oBTServer.oInputStream))	nServerState1 = kReady2;		//Reset FSM
        } else if (kGetReq==nServerState1){
            if (mRX_GetReq2(oSerial.oBTServer.oInputStream))			nServerState1 = kReady2;		//Reset FSM
        } else  {
            mErrMsg("170929 FE");									//Send error message to server??

            return false;
        }
        return true;
    }

    private boolean mTX_ProtInitReq(cRelay2Client oTX ){       //Send element structure to client initializing the protocol.
        OutputStream os = oTX.oOutputStream;
        for (int i = 0; i< mGetProtSize(); i++){
            cProtElem oElem = oaProtData[i];
            oTX.mWriteByte(os,kCommInit);          // Header
            int nLen = oElem.sVarName.length();                         //Send symbolic name
            oTX.mWriteByte(os,nLen);						//Send String mSetProtSize
            for ( int j=0;j<nLen;j++){
                oTX.mWriteByte(os,oElem.sVarName.charAt(j));  	// Transmit characters
            }
            if (oElem.nVarId<64) oElem.nVarId=64+i;         //Maybe offline so simulate varids
            oTX.mWriteByte(os,oElem.nVarId);                   // Send identifier
            oTX.mWriteByte(os,oElem.nDataLength());            //Send ArrLen
            oTX.mWriteByte(os,oElem.nVarType);                 //Send VarType
        }
        return true;
    }

    boolean mTX_SetReq2(InputStream oI){		//Check that you can receive 6  bytes before a call
        if (mGetBytesAvailable(oI)<6) return false;		//Cant process
        int nVarId = oSerial.mReadByte2(oI);                //1 byte
        int idx = oSerial.mReadByte2(oI);                    //2 byte Index of array to write to
        // Get variable
        cProtElem oElem = mProtDataObjById(nVarId);
        int rcv1 = oSerial.mReadByte2(oI);                            //3 byte
        int value = rcv1 << 24;
        rcv1=oSerial.mReadByte2(oI);   							//4 byte
        value+=rcv1<<16;
        rcv1=oSerial.mReadByte2(oI);   							//5 byte
        value+=rcv1<<8;
        rcv1=oSerial.mReadByte2(oI);   							//6 byte
        value+=rcv1;
        oElem.mDataWrite(idx,value);
        return true;										//Completed state
    }       //Candidate for substituting

    boolean mRX_GetReq2(InputStream oInputStream){		//Requires 1 byte in buffer, todo check before call
        cRelay2Client oRX = oSerial.oBTServer;
        if (mGetBytesAvailable(oInputStream)<1) return false;		//Cant process yet
        int nVarId = oSerial.mReadByte2(oInputStream);        //Expect ID and increase its send counter
        cProtElem oElem = mProtDataObjById(nVarId);
       // oElem.nSetReqCntr++; //Make it send  Obviously this is a problem because it makes it send to device not to client
        mSendElementData2Client(oSerial.oBTServer,nVarId);
        return true;										//Completed state
    }

    private int mGetBytesAvailable(InputStream oInputStream) {
        return oSerial.nBytesAvailable(oInputStream);
    }
    //  UNUSED STUFF
    private boolean mWait(int nCycles){         //Will return false after nCycles call (not threadsafe)
        nTimeOut--;
        if (nTimeOut<0) nTimeOut=nCycles;
        if (nTimeOut==0) return false;
        return true;
    }

    private void mAutoReconnect() {
        if (oSerial.mConnectionState()== kConnected) {
            if (mWait(60))                  //170929 Retry connection after some time
                mSetState(eProtState.kDoConnect1);
        }
    }
    private boolean mIsDeviceProtocolReady() {  //Returns true if the protocol is initialized from device
        for (int i = 0; i< nProtSize; i++){
            if (oaProtData[i].nVarId<64) {      //Protocol not from device if varid is less than 64
                return false;
            }
        }
        return true;
    }

}



