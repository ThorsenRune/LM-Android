//170928  see server relay methods at the end
//Doc:  https://docs.google.com/document/d/18xL6H-XLR2rr2MjD_jXzKkW_k6YwTHRbHmcLJX7LdLU/edit
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//  Implementing relay in version 5

package it.fdg.lm;

import android.content.Context;

import java.io.InputStream;
import java.io.OutputStream;

import static it.fdg.lm.cAndMeth.mSleep;
import static it.fdg.lm.cFileSystem.mPrefs5;
import static it.fdg.lm.cFunk.mInt2ByteArray;
import static it.fdg.lm.cKonst.eProtState.kDoConnect1;
import static it.fdg.lm.cKonst.eProtState.kProtError;
import static it.fdg.lm.cKonst.eProtState.kProtInitDone;
import static it.fdg.lm.cKonst.eProtState.kProtInitFailure;
import static it.fdg.lm.cKonst.eProtState.kProtInitInProgress;
import static it.fdg.lm.cKonst.eProtState.kProtReady;
import static it.fdg.lm.cKonst.eProtState.kProtResetReq1;
import static it.fdg.lm.cKonst.eProtState.kUnconnected2;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.oaProtocols;
import static it.fdg.lm.cProgram3.sDevices2;


/*--------------------------------------------------------------
*  Main processing communication with device through the protcol
*  */
public  class cProtocol3 {                  //This was formerly just called protocol, renamed to cDevice
    public  cProtElem[] oaProtElem;         //Elements in this protocol
    private cProtElem oNewElement;  //A temporary new element when collecting elements from devices
    private  int nProtSize1;                //The size of the protocol i.e. the number of variables exposed  nProtSize private int  int nProtSize;          //The size of the protocol i.e. the number of variables exposed  nProtSize   (DONT use .nDataLength since it will be full of null elements
    //  IO Connection
    public cSerial5 oSerial;                //      The serial communication class
    public cFIFO oRXFIFO;    // RX circular buffer identical to cSerial5.oRXFIFO
    public cFIFO oTXFifo;    // TX do

    //      Finite state machine of the dispatcher
    public int nCmd[]=new int [1];      //Primary FSM pointer. (Dispatcher)
    public int zState[]=new int [1];    //Secondarey FSM pointer
    private int nServerState1= kReady2; //State of the server FSM
    //Flag for initializing protocol mTX_Dispatch:kCommInit
    //{kCommInit, kReady,kErrMsg} meaning: {reset the protocol, protocol is ready to be used, protocol is not initialized and not ready}


    //    Communication protocol headers(commands) shared with device Firmware
    private static final int kReady2=1;    //communication FSM is ready for a new pack
    private static final int kError= 13;      //Indicates an error (renamed from kErrMsg il 161202)
    private static final int kHandshake=15;      // Waiting for the protocol to settle
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
    cProtElem oRXGetReqElem;           //Currently processed element in oRXGetReqElem  161205/rt
    private int nProcessCycleCount=0; //A 'timer' for the number of times the protocol is processed. incremented in mUIProcess
    private cKonst.eProtState nProtState=kUnconnected2; //Start by resetting the protocol

    private int k32BitInt= 232;
	//			PROPERTIES
    public  Context mContext;
	private String myId;              //Identifier for the device protocol
    private int myIndex=0;              //Index in the array of protocols

    //	!- Deletable debug rubbish
	public int nTimeOut=0;          //170323    for each successful receive nTimeOut will reset to kTimeOut
    private boolean isRunning=false;
    private int[] n;
    private int nCallNumber=0;
    private int nRcvDataCount;
    private int nWordLen=0;
    private String sVarList="";         //List of element names
    private String sElemListKey;


    //******************************			IMPLEMENTATION     ************************
    public void mInit(int nClassIndex) {//Initializer/Constructor
        myIndex=nClassIndex;
        oSerial = new cSerial5(this);
        oSerial.mInit("Serial" + myIndex);
        oRXFIFO = oSerial.oRXFIFO;
        oTXFifo = oSerial.oTXFIFO;
        mInitProtArray(30);
        mStateSet(kDoConnect1);         //Request connection when possible
    }
    void mEnd(){
        oSerial.mBTClose1();             //Close ports when program quit
    }
    void mSettings(boolean bGet){//Load/mFileSave settings for the protocol defined by device
        /* Call:mPersistAllData->mSettings          */
        sElemListKey=sProtName()+cKonst.sKeyFieldSep+ "Elements";
        this.mSetProtElementList(mPrefs5(bGet,sElemListKey , this.mGetProtElementList()));   //
        for(int i = 0; i< mProtElemLength(); i++){
            oaProtElem[i].mSettings(bGet);      //Get settings for sKey=sVarName;
        }
        mMessage("Loaded elements:"+mProtElemLength());
    }       //Set/Get persistent data
    //******************************			PROPERTIES     ************************
            //      *** REQUESTS ***

    void mRequestConnection() {
        mStateSet(kDoConnect1);
    }
    void mBT_PickDevice2() {
        if (oSerial==null) return;
        oSerial.mBT_PickDevice2();

    }


            //     *** GETTERS SETTERS  ***
    void mInitProtArray(int nNewSize) {
        oaProtElem =new cProtElem[nNewSize];      //Reserve room for the protocol elements
        for(int i = 0; i< oaProtElem.length; i++){               //Initialize elements
            oaProtElem[i]=new cProtElem(this);
        }
        nProtSize1=0;
    }
    int mProtElemLength(){        //Don't use oaProtElem.nDataLength because it has null elements
        return nProtSize1;
    };





    //  Processing in background thread the serial communications
    public void mAsyncProcessing_Call() {  //A thread connecting to the device
        if (isRunning==false) {
            isRunning = true;
            new Thread(new Runnable() {
            public synchronized void run() {
                Thread.currentThread().setName("mAsyncProcessing_Call");
//                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
             //   mProcess_Async();
                cProgram3.bDoRefresh = true;
                }
            }).start();         //Important. actually starts the process
            isRunning = false;
        }
    }
    public void mProcess_Async() {
        if (getState()== cKonst.eProtState.kProtReady) {            //Protocol statemachine
            mRelay();       //Relay old data, get changes from client
            boolean ret = mTX_Dispatch();      //170605    Rewritten to process only datarequests
            oSerial.mProcessSerial();    //Send the request immediately
            mDispatchRX(nCmd, zState);
            if (!oSerial.mIsConnected())
                mStateSet(kUnconnected2);   //180328    detect broken connection disconnected
        } else if (mIsState(kDoConnect1)){
            if (oSerial.isConnected()) {
                oSerial.mDisconnect();
                return;
            }
            oSerial.mConnect(mDeviceNameGet());
            if (oSerial.isConnected())
                mStateSet(kProtResetReq1);
            else
                mStateSet(kUnconnected2);
        } else if (mIsState(kProtResetReq1)) {    //Request the protocol setup
            //We will assume that the protocol has been preinitialized from file, we look for ID's of variables
            mMessage("Resetting: "+sProtName());
            mStateSet(kProtInitInProgress);      //Idle mode awaiting initialization of the protocol
            mTX_ProtReset();                //Set a protocol reset command
        } else if (getState()==  kProtInitInProgress) {
             bDoRedraw=true;
            if (mVerifyInit()) {
                mMessage( sDeviceName()+ " Ready");
                bDoRedraw=true;
                mStateSet(cKonst.eProtState.kProtInitDone);    //Protocol good to use
            }else {
                mStateSet(kProtInitFailure); //No data was received
                mTX_ProtReset();                //Set a protocol reset command
            }
        } else         if (getState()== kProtInitDone) {
            mStateSet(kProtReady);   //Now the UI knows the protocol is ready  so display can be refreshed
            bDoRedraw=true;
         }
        cProgram3.bDoRefresh = true;
    }

    private String sDeviceName() {
        return  oaProtocols[myIndex].mDeviceNameGet();
    }

    public String sProtName() {
        return "P"+(1+myIndex);         //Naming protocols as P1, P2 etc
     //   return sDevices1[myIndex];
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
        for(int i = 0; i< mProtElemLength(); i++) {           //Loop the Protocol objects to see if there are actions to take
            cProtElem oElem = oaProtElem[i];
            if (oElem.mIsSetRequest()) {    //There are data to set in devices
                mTX_SetReq(oElem);
                bRet=true;
            } //Don't read while writing
            else if (oElem.mHasGetRequest()) {        //Request data from device
                mTX_GetReq(oElem);
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
        oSerial.mProcessSerial();    //Send the request immediately
        for (int i=0;i<nProtSize1;i++) {       //number of elements
            mSleep(100);                //Now device will start sending data
            oSerial.mProcessSerial();    //Send the request immediately
            if (oSerial.oRXFIFO.nBytesAvail<1)
                oTXFifo.mFIFOpush((byte) kCommInit);  //Send the reset request to device
            mDispatchRX(nCmd, zState);
        }

        return true;            //expect a response
    }

    private void mTX_GetReq(cProtElem oProtElem){        //TX Get request
        oTXFifo.mFIFOpush((byte) kGetReq);    //+161216   BUGFIX: corrected constant
        oTXFifo.mFIFOpush((byte) oProtElem.nVarId());
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
            oTXFifo.mFIFOpush((byte) oProtData.nVarId());             //array identifier
            oTXFifo.mFIFOpush((byte) offs);                         //offset in array
            // [] to implement the case for different datatypes k32bit
            byte b[]= mInt2ByteArray(oProtData.mDataRead(offs));
            //+161216   bugfix push high to low bytes
            oTXFifo.mFIFOpush(b[0]);        //value of the array[offset]
            oTXFifo.mFIFOpush(b[1]);        //value of the array[offset]
            oTXFifo.mFIFOpush(b[2]);        //value of the array[offset]
            oTXFifo.mFIFOpush(b[3]);        //value of the array[offset]
            oProtData.mSetRequest(-1);
        }
    }
    //              --------------------------       RX Dispatcher of data packages -----------------------------------
    public void mDispatchRX(int[] nCmd, int[] zState){                   //Rev 161202/RT
        for (int loop=0;loop<oRXFIFO.nBytesAvail;loop++) {         //!+       refactor  canpop to take the minimum number of bytes avaiable for popping
            if (oRXFIFO.mCanPop(1) == false) {
                mSleep(1);
                oSerial.mProcessSerial();    //Process serial port
            }  else if (nCmd[0] == kReady2) {
                nCmd[0] = oRXFIFO.mFIFOpop();
            }
            if (nCmd[0] == k32Bit) {       //Read data from stream
                if (mRXGetReq2(nCmd, zState) == false)
                    mSleep(1);
            } else if (nCmd[0] == kCommInit) {       //Get initialization data from stream
                oSerial.mProcessSerial();    //Process serial port
                mStateSet(kProtInitInProgress);
                mRX_ProtInit(nCmd, zState);
            } else {
                mErrMsg("Error in protocol");
                mSleep(200);    //Gather all and flush
                oSerial.mFlushInput();
                oRXFIFO.mFlush();
                oTXFifo.mFlush();
                nCmd[0] = kReady2;
            }
        }
    }


//              ***COMMAND PACKINGS OF THE PROTOCOL

    private void mRX_ProtInit(int[] nCmd, int[] zState){    //it is called inside mRXdispatch when nCmd=kCommInit
/*     its a FSM therefore it is called with the static state variables using zero lenght array casting
//!+161202     int  zState=0;            zState needs to be static (remebered between calls, therefore it is passed as a parameter)
//!-       int nStringLength=oRXFIFO.mFIFOpop();    //Cant do that yet
// 170822  revision to use a new element and then insert it in existing protocol loaded from disk
*/
        int nBytes = oRXFIFO.nBytesAvail;
    if (nBytes<2) {
        mErrMsg("171218 error");
        return;
    }
    oNewElement=new cProtElem(this);        //Make a new protocol element  R170822
    oNewElement.mInit2();                     //Reset its data
    oNewElement.nVarNameLen= oRXFIFO.mFIFOpop();  //Length of expected characters
    oNewElement.sVarName="";                                  //Initialize sVarName
    for (int i=0;i<oNewElement.nVarNameLen;i++) {
        oNewElement.sVarName = oNewElement.sVarName + (char) oRXFIFO.mFIFOpop();
    }
    int i= oRXFIFO.mFIFOpop();
    oNewElement.nVarId(i);
    oNewElement.nDataLength((int) oRXFIFO.mFIFOpop());
    oNewElement.nVarType= oRXFIFO.mFIFOpop();
    mSetElem(oNewElement);
        //Reset state machines
        zState[0]   =kReady2;                                       //Process is completed, reset the FSM
        nCmd[0]     =kReady2;                                        //Prepare to receive a new command from device
}


  //!+161203-----------------INITIALIZATION OF PROTOCOL RECEIVING EXPOSED VARIABLES---------

    private void mSetElem(cProtElem oNewElement) { //Insert a named element in the protocol
        int idx;
        idx= mRXCommInit_Idx4Elem(oNewElement); //R171212
        sVarList=sVarList+oNewElement.sVarName+",";
        if (idx<0){     //Element not found in protocol
            mErrMsg("Error in protocol, add "+sElemListKey+"  "+oNewElement.sVarName);
        } else {
            oaProtElem[idx].mCopyElement(oNewElement);
        }
    }



    private void mRXCommInit_AddElem0(cProtElem oNewElement) { //Insert a named element in the protocol
        int idx = nProtSize1;
        oaProtElem[idx].mCopyElement(oNewElement);
        oaProtElem[idx].mSettings(true);
        nProtSize1=nProtSize1+1;
    }
    private int mRXCommInit_Idx4Elem(cProtElem oE) {
        for (int idx = 0; idx< mProtElemLength(); idx++){
            if (oaProtElem[idx]==oE){
                return idx;
            }
            if (cFunk.mTextLike(oE.sVarName,oaProtElem[idx].sVarName)){
                return idx;
            }
        }
        return -1;
    }
    private boolean mVerifyInit() {  //Returns true if the protocol is initialized from device
        for (int i = 0; i< mProtElemLength(); i++){
            if (oaProtElem[i].nVarId()<64) {      //Protocol not from device if varid is less than 64
                mErrMsg(oaProtElem[i].sVarName+ "Not found device: "+sProtName());
                return false;
            }
        }
        return ( 2<mProtElemLength());
    }

    private boolean mRXGetReq2(int[] nCmd,int[] nState){
        int value=0;
        if (oRXFIFO.nBytesAvail < 2) return false;
        int nId = oRXFIFO.mPeek(0);          //1 byte         Read the var  ID that is coming
        oRXGetReqElem = mProtDataObjById(nId);
        mDebugNull(oRXGetReqElem);
        nRcvDataCount = oRXFIFO.mPeek(1);       //2 byte  size of datarray to expect
        if (k32Bit==oRXGetReqElem.nVarType) nWordLen=4;               //other if statements for other datatypes
        int nPackSize = 1 + 1 + (nWordLen * nRcvDataCount);     //Expected packet size
        if (oRXFIFO.nBytesAvail<nPackSize) return false;
        oRXFIFO.mFIFOpop();     //Pop the bytes
        oRXFIFO.mFIFOpop();
        for (int i = 0; i< nRcvDataCount; i++) {   //+161216
           if (nWordLen == 4) { //Perform bitwise or with the data shifted
                value = oRXFIFO.mFIFOpop() << 24;
                value |= oRXFIFO.mFIFOpop() << 16;
                value |= oRXFIFO.mFIFOpop() << 8;
                value |= oRXFIFO.mFIFOpop();
                }
           oRXGetReqElem.mData(i,  value);
        }
        nCmd[0]=kReady2;    //Reset FSM
        nState[0]=kReady2;      //Reset statemachine
        oRXGetReqElem.mGetReqDone();             //Decrease the request counter
        return true;                            //  The data has been received
    } //    RX a Get request, process the received data

    private void mDebugNull(cProtElem obj) {
        if (obj==null){
            mErrMsg("Null pointer");
        }

    }

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
            mStateSet(kProtError);  //Set a request to device for the protocol  R170314
            return false; //this is an error
        }
        return true;
    }       //IS the element valid

    public cProtElem mProtDataObjById(int nVarId){  //Return the protocol data object by ID
        for(int i = 0; i< mProtElemLength(); i++){
            if(nVarId== oaProtElem[i].nVarId()) return oaProtElem[i]; //I find oData with the corresponding Id
        }
        return null;       //DataObject not found return null
    }


    public int nIndex4VarName(String sVarName) {       //171204 return element position by name
        for (int idx = 0; idx<= mProtElemLength(); idx++){
            if (cFunk.mTextLike(sVarName,oaProtElem[idx].sVarName)){
                return idx;
            }
        }
        return -1;
    }
    public cProtElem mGetElemByName(String sVarName) {    //Returns the data element element by its variable name
        if (sVarName=="") return null   ;
        for (int i = 0; i < mProtElemLength(); i++) {
            if (cFunk.mTextLike(sVarName,oaProtElem[i].sVarName)) {
                return oaProtElem[i];
            }
        }
        return oaProtElem[0];
    }


    public String[] mGetProtElementList(){        //Return a list of element names
        String[] sVarList = new String[this.mProtElemLength()];
        for (int i = 0; i < this.mProtElemLength(); i++) {
            sVarList[i] = oaProtElem[i].sVarName;
        }
        return  sVarList;
    } //Getter setter for protocol elements as a list
    public void mSetProtElementList(String[] sElList) {   //Define a protocol by a list of element names, 171129 redefined as 4 element vectro
        if (sElList.length<1)
        { mErrMsg("err171219"); return;}
        nProtSize1= sElList.length;
        for (int i = 0; i < sElList.length; i++) {
            if (sElList[i].length()>1) {
                oaProtElem[i].sVarName = sElList[i];
            }
        }
    }

    //      Get set the status of the protocol, ready, idle etc.
    public cKonst.eProtState getState() {
        if (nProtSize1==kReady2)
            if (oSerial.mIsConnectionError())
                nProtState=kUnconnected2;
        return nProtState;
    }
    public void mStateSet(cKonst.eProtState newState) {      //!!!Refactor to enum
        if (nProtState!=newState)           //180404
            bDoRedraw=true;             //171012    A changed state will require a display redraw
        nProtState=newState;
    }
    public boolean mIsState(cKonst.eProtState nCheckState) {
        return nCheckState==nProtState;
    }

    void mSendElementData2Client(cRelay2Client oTX, int nVarId){    //Send 32 bit data from element[nVarId].data to client
        cProtElem oElem = mProtDataObjById(nVarId);
        if (oElem==null) {mErrMsg("mSendElementData2Client empty");return;}
        OutputStream os = oTX.oOutputStream;
        int nCount = oElem.nDataLength();
        oTX.mWriteByte(oTX.oOutputStream, k32BitInt); // xmit header cProtocol.h {   k32BitInt=232}
        // all mFIFO_push ===oTX.mWriteByte
        oTX.mWriteByte(os,nVarId);          // xmit VarId
        oTX.mWriteByte(os, nCount);         // xmit Count
        for (int i=0; i< nCount; i++){    //!!!implement overflow check
            int data = oElem.mDataRead(i);
            oTX.mWriteByte(os, data>>24);       //RT:Send high octet
            oTX.mWriteByte(os, data>>16);       //RT:Send 2 high octet
            oTX.mWriteByte(os, data>>8);        //RT:Send 3 octet
            oTX.mWriteByte(os, data>>0);        //RT:Send low octet
        }
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
        for (int i = 0; i< mProtElemLength(); i++){
            cProtElem oElem = oaProtElem[i];
            oTX.mWriteByte(os,kCommInit);          // Header
            int nLen = oElem.sVarName.length();                         //Send symbolic name
            oTX.mWriteByte(os,nLen);						//Send String mInitProtArray
            for ( int j=0;j<nLen;j++){
                oTX.mWriteByte(os,oElem.sVarName.charAt(j));  	// Transmit characters
            }
            int n=oElem.nVarId();
            if (n<0) n=64+i;         //Maybe offline so simulate varids
            oTX.mWriteByte(os,n);                   // Send identifier
            oTX.mWriteByte(os,oElem.nDataLength());            //Send ArrLen
            oTX.mWriteByte(os,oElem.nVarType);                 //Send VarType
        }
        return true;
    }

    boolean mTX_SetReq2(InputStream oI){		//Check that you can receive 6  bytes before a call
        if (mGetBytesAvailable(oI)<6) return false;		//Cant process
        int nId = oSerial.mReadByte2(oI);                //1 byte
        int idx = oSerial.mReadByte2(oI);                    //2 byte Index of array to write to
        // Get variable
        cProtElem oElem = mProtDataObjById(nId);
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

    boolean mRX_GetReq2(InputStream oInputStream){		//Requires 1 byte in buffer
        cRelay2Client oRX = oSerial.oBTServer;
        if (mGetBytesAvailable(oInputStream)<1) return false;		//Cant process yet
        int nVarId = oSerial.mReadByte2(oInputStream);        //Expect ID and increase its send counter
        cProtElem oElem = mProtDataObjById(nVarId);
        if (oElem==null) {mErrMsg("mSendElementData2Client empty");return false;}
       // oElem.nSetReqCntr++; //Make it send  Obviously this is a problem because it makes it send to device not to client
        mSendElementData2Client(oSerial.oBTServer,nVarId);
        return true;										//Completed state
    }

    private int mGetBytesAvailable(InputStream oInputStream) {
        return oSerial.nBytesAvailable(oInputStream);
    }



    public String mDeviceNameGet() {
        return sDevices2[myIndex];
    }


    public int nIndex() {
        return myIndex;
    }

    public static cProtElem mElem(int myProtIdx, int myElemIdx) {
        if (myProtIdx < oaProtocols.length)
            if ((0<myElemIdx)&(myElemIdx < oaProtocols[myProtIdx].nProtSize1))
                return oaProtocols[myProtIdx].oaProtElem[myElemIdx];
        return oaProtocols[0].oaProtElem[0];
    }

    public  cProtElem mElem4Name(String myVarName) {
        int i=nIndex4VarName(myVarName);
        try {
            return oaProtocols[i].mGetElemByName(myVarName);
        } catch (Exception e){
            return null;
        }
    }


    public void mDeviceNameSet(String s) {      //Set a new device name and request a connection
        mMessage("Pinging "+s);
        sDevices2[myIndex]=s;
        cProgram3.bDoSavePersistent=true;
        mStateSet(kDoConnect1);
    }
}



