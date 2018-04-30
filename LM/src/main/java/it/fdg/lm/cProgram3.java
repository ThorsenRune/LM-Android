//171003    Refactoring and cleaning
//170825
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1IE3Ob0GmIqLZTRY9gOeGrDasWwAm9jiih5rrskf6Q2U/edit

package it.fdg.lm;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static it.fdg.lm.cAndMeth.mSleep;
import static it.fdg.lm.cFileSystem.mPersistHex;
import static it.fdg.lm.cFileSystem.mPrefFileNameSet;
import static it.fdg.lm.cFileSystem.mPref_Clear;
import static it.fdg.lm.cFileSystem.mPrefs5;
import static it.fdg.lm.cFileSystem.mStr2Pref;
import static it.fdg.lm.cFunk.mArrayFind;
import static it.fdg.lm.cFunk.mArrayRedim;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cFunk.mTextLike;
import static it.fdg.lm.cKonst.eAppProps.kAutoConnect;
import static it.fdg.lm.cKonst.eAppProps.kZoomEnable;
import static it.fdg.lm.cKonst.nAppProps;



public final class cProgram3 {
    public static Context mContext;         //This instance
	private static cElemViewProps[] oaElementViews;    //
    public static cSlider[] oSlider;               //Slider widget controls 10 vertical and 10 horizontal
    private static int nTestCount=0;
    public static cSignalView2 mySignal;        //The current signal view
    //Properties of the application
    public static BluetoothDevice[] oPairedDevices;
    public static Typeface oTextTypeface;
    public static boolean bDoRefresh=false;
    public static cGraphText oGraphText;
    private static Timer oTimer=new Timer();
    public static int mLoopTime;
    private static int _RefreshRate;            //Refresh rate of the screen and communication protocol
    private static int _Privileges;
    private static boolean _DesignMode;
    public static String sConnectStatus ="";    //todo 180308
    public static cUInput oUInput;
    public static boolean bDoSavePersistent=false;  //Flag that data has changed and should be saved on exit
    public static int nMsgVerboseLevel=5;  //How many messages are shown

    public static int nPanelSizes[]={2,1,1,1,0};              //Weights of the panels
    public static boolean bPanelData;
    public static boolean bWriteOnReset=false;   //r180420 Write to device on init flag bWriteOnReset
    private static String sMsgLog="";           //Log of messages

    public static String sFileName_Help="help";

    public static void mInit(Context ctx) {     //Initializations
        oSlider = new cSlider[20];
        oUInput = new cUInput(ctx);
    }

    //SGetter for refresh rates
    public static void nRefreshRate(int i) {        //Adjust communication speed
        i=mLimit(60,i,1000);
        _RefreshRate=i;
        mMsgLog(10,"New refresh rate " +i);
    }
    public  static int nRefreshRate(){return _RefreshRate;}
    //Other application properties
    public static int mAppProps(cKonst.eAppProps kEnum) {
        int n=kEnum.ordinal();
        if (nAppProps.length<=n)
            nAppProps=mArrayRedim(nAppProps,n);     //Make sure that there is room of index n
        return  nAppProps[n];
    }
    public static void mAppPropsSet(cKonst.eAppProps kEnum, int i) {
        nAppProps[kEnum.ordinal()]=i;
    }
    public static void mAppPropsToggle(cKonst.eAppProps kEnum) {
        nAppProps[kEnum.ordinal()]=(nAppProps[kEnum.ordinal()]+1)%2;
    }

    public static String getMyIdName(View oView) {
        String s = oView.getResources().getResourceEntryName(oView.getId());
        return s;
    }


    public static void mShiftPanel(int nPageChange) {        //Change a page of the sliders
        nWatchPage=mLimit(0,nWatchPage+nPageChange,1);
        bDoRedraw=true;
    }


    public static boolean bDesignMode(){
        return _DesignMode;
    }
    public static void bDesignMode(boolean b) {
        _DesignMode=b;
    }
    static int[] nPalette={0};
    public static int nWatchPage=0;
    public static int nSignalPage=0;
    private static String[] sWidgetIds={"W0"};
    static cGestureListener oGlobalGestureDetector;   //170915
    public static float nSliderSize =1.2f;           //Size of the sliders
//  Protocol for devices
// public static SignalBase oSignalBase
    public static cProtocol3[] oaProtocols;
    public static String[] sDevices2={};           //Protocol names
    private static Handler handler= new Handler();
    public static boolean bDoRedraw;        //Request a redraw of visible screen
    public static boolean bRunning2=false;  //Flag the running state
    public static Object oFocusdActivity ;      //Currently focused activity
    public static String sFile_ProtCfg ="";
    public static String sFile_AppCfg ="config";
    public static String[] sProtDesc={"Revision","Description","Remark"};
    private static long nStartTime=System.currentTimeMillis();

    //--------------------------------- MAIN METHODS------------------------------
    //          PROPERTIES
    public static boolean bAdmin() {        return  mPrivileges()>0;    }
    public static boolean bAutoConnect() {
        return 0<nAppProps[kAutoConnect.ordinal()];

    }
    public static boolean bZoomEnable() {
        nAppProps=mArrayRedim(nAppProps,kZoomEnable.ordinal()+5);
        nAppProps[kZoomEnable.ordinal()]=1;     //!-Temporaryly permanently enabled
        return ( nAppProps[kZoomEnable.ordinal()]>0);
    }

    public static boolean nUserLevel1(int level) {
        return (0<( _Privileges&level ));
    }
    public static void mPrivileges(int i) {
        _Privileges=i;
        nMsgVerboseLevel=i+1;
        bDoRedraw=true;
    }

    public static int mPrivileges() {
        return _Privileges;
    }

    //          METHODS
    protected static void mCommunicate(boolean bRun) {       //Avoid that this is called multiple times
        if (oaProtocols==null) return;  //Wait until protocol is ready
        if (bRun==false) {             //Shut down processing
            bRunning2 = false;
            mSleep(2000);
        }
        else if (bRunning2==bRun) {    //No change, will assure only one thread
            mMsgLog(8, "mCommunicate is running");
        }
        else if (bRun) {
            bRunning2=true;
            mPeriodicProcessing.run();      //Start the timer
            oTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    nStartTime = mNowMs();
                    Thread.currentThread().setName("Prot_Async");
                    if (oaProtocols!=null)
                    for (int i = 0; i <oaProtocols.length ; i++) {//180103 index fix
                        if (oaProtocols[i] != null) oaProtocols[i].mProcess_Async();    //Non gui
                    }
                    mLoopTime =cFunk.mAverage(mLoopTime, (System.currentTimeMillis() - nStartTime));
                    if (bRunning2 == false) {
                        this.cancel();
                    }
                }
            }, 1000, nRefreshRate());
        } else {
            bRunning2=false;
            oTimer.cancel();
            mMessage("Not running");
        }
    }       //set Run/Pause of device communication

    public static long mNowMs() {
        return System.currentTimeMillis()-nStartTime;
    }

    private static Runnable mPeriodicProcessing = new Runnable() {
        @Override
        public void run() {
            if (bRunning2) mPeriodicProcessingExec();
        }
        private void mPeriodicProcessingExec() {                    //This will repeat itself
            cProgram3.mMainProcess1();
            if (bRunning2)
                handler.postDelayed(this, nRefreshRate());//Set timer for next call of this process
            else
                mMessage("Stopped");
        }
    };

    public static void mRedraw() {  //// Redraw displays
        fMain.mRefresh_DispMain(true);     //principal redraw
    }

    public static boolean mMainProcess1() {
        if (bRunning2==false) return false ;
        if (bDoRefresh|bDoRedraw) {
            fMain.mRefresh_DispMain(bDoRedraw);       //Syncrhornize display update with async process
            bDoRedraw=false;    //Redrawing done
            bDoRefresh=false;
        }
        return (0< nRefreshRate());            //170315    Returns false when program is to be ended
    }    //  Main process for device communication entry

    public static void mEndProgram(){    //Shut down gracefully    */
        mAlert2("Shutting down");
        mCommunicate(false);  //Stop communication
        if (cProgram3.bDoSavePersistent)  {
            mPersistAllData(false,sFile_ProtCfg);//Save data if dirty 180417
        }
        mAppSettings(false);
        if (            oaProtocols==null) return;
        for (int i = 0; i< oaProtocols.length; i++){        //For each device
            oaProtocols[i].mEnd();                          //Close bluetooth ports
        }
    }


//  *******************************Settings management*********************
public static void mAppSettings(boolean bGet) {
    // Persistent data for the application. Get/Set current values to static storage
    mMsgLog("App settings:"+ sFile_AppCfg);
    int nKeys = mPrefFileNameSet(sFile_AppCfg);
    if (nKeys<1) {    //File not exising
        mMessage("Reading factory settings for :"+ sFile_AppCfg);
        String s =cFileSystem.mFileRead_Raw(sFile_AppCfg);
        cFileSystem.mStr2Pref(s);
    }
    //   A: Application settings 170825
    //Viewing
    nPalette =mPersistHex(bGet,"Palette", nPalette);   //Color palette for controls
    nSliderSize = mPrefs5(bGet, "SliderSize", nSliderSize);   //Reduction factor for views
    _Privileges = mPrefs5(bGet,"Privileges", _Privileges);   //Permissions of the user
    nAppProps= mPrefs5(bGet,"Properties",nAppProps);   //Permissions of the user
    nRefreshRate( mPrefs5(bGet,"RefreshRate", nRefreshRate()));   //Permissions of the user
    _DesignMode= ( mPrefs5(bGet,"DesignMode", _DesignMode));   //Permissions of the user
    sFile_ProtCfg =mPrefs5(bGet,"CurrentProtocolFile", sFile_ProtCfg);
    nMsgVerboseLevel=mPrefs5(bGet,"MsgVerboseLevel", nMsgVerboseLevel);
}

    public static void mMsgLog(String s) {
        mMsgLog(2,s);
    }


    public static void mPersistAllData (boolean bGet,String sProtocolConfigFile){
       /*   Persistent data for the application. Get/Set current values  to static storage
       *  mPrefFileNameSet("LM_AppSetting");     //Global settings
       *   sFile_ProtCfg=mPrefs5(bGet,"Protocol_Settings_File", sFile_ProtCfg);   //Current file for  protocol settings
       * */
        sFile_ProtCfg=sProtocolConfigFile;
        mPrefFileNameSet(sFile_ProtCfg); //Protocol settings clear old if writing
        sProtDesc= mPrefs5(bGet,"Description",sProtDesc);
        sWidgetIds = mPrefs5(bGet,"Widgets", mGetListOfControls());   //IDs for widgets used (S# signal W# slider B# biteditor, # index
        sDevices2 = mPrefs5(bGet,"Devices", sDevices2);   //Device names (protocol names)  170823
        if (bGet)mProtocolPrepare(sDevices2.length);
        if (oaProtocols==null) return;  //If program is shutting down
        for (int i = 0; i< oaProtocols.length; i++) {
          oaProtocols[i].mSettings(bGet);      //Load settings for each protocol
        }
        //Load/save    View settings
        if (bGet) oaElementViews= new cElemViewProps[100];
        for (int i = 0; i < sWidgetIds.length; i++){       //170823 revised with a list of used widgets
            cElemViewProps oa = mGetViewProps(null,sWidgetIds[i]);
            oa.mViewSettings3(bGet,sWidgetIds[i]);
        }
        //  GUI Visibility Settings

        bPanelData=mPrefs5(bGet,"bPanelData", bPanelData);

        nPanelSizes=mPrefs5(bGet,"nPanelSizes", nPanelSizes);   //Sizes of the panels

        bWriteOnReset=mPrefs5(bGet,"WriteOnReset", bWriteOnReset);
        cProgram3.bDoSavePersistent=false;      //Clear the save persistent flag
  }             //Save/Load settings from persistent storage




    //          MESSAGES
    public static void mMessage(String msg){
        mWaitMsg(msg,1000);
    }



    static void mMsgLog(int nVerbosity,String msg){        //Adds message to a log
        if (mPrivileges()<1) return;
        mMsgLogAdd(msg);
        if (nVerbosity<nMsgVerboseLevel) oUInput.mMessage(msg,0);
    }

    private static void mMsgLogAdd(String msg) {
        int k = 1000;
        sMsgLog=sMsgLog+"\n"+msg;
        int l=sMsgLog.length();
        if (l>k)
            sMsgLog=sMsgLog.substring(l-k,l);
    }

    public static void mErrMsg(String msg) {
        mMsgLog(0,msg);
    }

    public static void mCommandTxt(String msg){      //Display current status of protocol
        mMsgLog(10,msg);
        fMain.cmdText(msg);
        bDoRefresh=true;
    }
    public static void mMsgDebug(String msg){      //Display current status of protocol
        mMsgLog(8,msg);
    }
    static void mAlert2(final String msg){
        if (msg.length()<3) return;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            }
        });
    }


 //****************+            HELPERS   **************************


    private static String[] mGetListOfControls() {     //Return a string array of
        if (oaElementViews==null) return new String[]{""};
        ArrayList<String> lst = new ArrayList<String>();
        for (int i = 0; i<oaElementViews.length; i++){
            if (oaElementViews[i]!=null)
                if (oaElementViews[i].myId1!="")
                if (oaElementViews[i].bVisible())
                    lst.add(oaElementViews[i].myId1);
        }
        if (lst.size()>0) lst.add("");
        String[] s = lst.toArray(new String[0]);
        return s;
    }

    public static void mProtocolPrepare(int nDevices) {
        oaProtocols = new cProtocol3[nDevices];
        for (int i = 0; i< oaProtocols.length; i++) {
            oaProtocols[i] = new cProtocol3();
            oaProtocols[i].mInit(i);
        }
    }

    public static cElemViewProps mGetViewProps(View view, String sId) {       //180424 link elementview to view
        int idx=0;      //First is a dummy null element
        for (int i = 1; i<oaElementViews.length; i++){
            if (oaElementViews[i]==null) {
                oaElementViews[i]=new cElemViewProps(sId);     //Create non registered views
                oaElementViews[i].myView=view;
                return oaElementViews [i];
            } else if (cFunk.mTextLike(oaElementViews[i].myId1,sId)){
                    oaElementViews[i].myView=view;
                    return oaElementViews[i];
            }
        }
        if (oaElementViews [idx]==null){
            cElemViewProps oa = new cElemViewProps(sId);
            oa.myId1=sId;
            int j = mArrayFind(sWidgetIds, sId);
            if (j<0) {      //170904    Add the ID to widget id only if visible or placeholder
                if (oa.bVisible()) {
                    sWidgetIds = mArrayRedim(sWidgetIds, sWidgetIds.length);
                    sWidgetIds[sWidgetIds.length - 1] = sId;
                }
            }
        }
        return oaElementViews[idx];
    }

    public static cElemViewProps[] oaWidgetList() {
        return oaElementViews;
    }   //Return element views



    public static int mPalIdx2Col(int nPalIdx) {     //Compress color
        if (nPalIdx>=nPalette.length)nPalIdx=0;
        return nPalette[nPalIdx];
    }
    public static byte mCol2PalIdx(int nColor) {  //8bit to color
        int kPatern = 0xFFFFFFFF;       //Only strong colors
        int i;
        for (i = 0; i < nPalette.length; i++) {
            if ((nPalette[i] & kPatern) == (nColor& kPatern))       //Similar?
                return (byte) i;
        } nPalette=mArrayRedim(nPalette,i);
        nPalette[i]= nColor;
        return (byte) (i);
    }
    public static int[] mGetPalette() {
        return nPalette;
    }


    public static void oControls_Add(View v, String sId) {
        int i=oControlsCount();
//        oaElementViews[i]= new cElemViewProps();
        cElemViewProps oa = mGetViewProps(v,sId);     //Get or make oaElementViews[i]
        oa.myId1=sId;
        oa.myView=v;
        ((cData_View) v).mElemViewProps(oa);
    }

    private static int oControlsCount() {
        for (int i = 0; i<oaElementViews.length; i++) {
            if (oaElementViews[i] == null) return i;
        }
        return 0;
    }

    public static void mControlsRefresh(boolean doRedraw) {
        for (int i = 0; i<oaElementViews.length; i++) {
            if (oaElementViews[i] == null) return;
            oaElementViews[i].mRefresh( doRedraw);
        }
    }



    public static cProtElem mElementByName(String protName, String elemName) {
        int i = nProtocol_IdxByName(protName);
        if (i<0){
            return null;}
        cProtElem e = oaProtocols[i].mGetElemByName(elemName);
        return e;
    }

    private static int nProtocol_IdxByName(String protName) {
        if (oaProtocols==null) return -1;
        for (int i = 0; i <oaProtocols.length ; i++) {
            if (mTextLike( oaProtocols[i].sProtName(),protName))
                return i;
        }
        return 0;
        }

    public static void mLoadFactorySettings(String sFileName) {
        String s = cFileSystem.mFileRead_Raw(sFileName);
        mPref_Clear();            //Remove old preferences
        mStr2Pref(sFileName,s);                      //Transfer to preferences
        mPersistAllData(true,sFileName);  //Load setup
        mAppSettings(false);
    }

    public static void mWaitMsg(String msg,int nTimeOut) {
        if (msg==""){ oUInput.mClose1();}
        else if (Looper.myLooper() == Looper.getMainLooper()) {   //Main thread
            oUInput.mMessage((nTestCount++)+"-"+msg,nTimeOut);
        } else {
            handler.post(new Runnable() {
                public void run() {
                    oUInput.mMessage((nTestCount++)+":"+msg,nTimeOut);
                }
            });

        }
    }

    public static void mWaitMsg(String wait, String s) {
        mWaitMsg(s,0);
    }

    public static String mMsgLog() {
        return sMsgLog;
    }
}
