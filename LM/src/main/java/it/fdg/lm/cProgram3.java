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

import static it.fdg.lm.cDebug.nTestCount;
import static it.fdg.lm.cFileSystem.mPersistHex;
import static it.fdg.lm.cFileSystem.mPrefFileNameSet;
import static it.fdg.lm.cFileSystem.mPrefs5;
import static it.fdg.lm.cFunk.mArrayFind;
import static it.fdg.lm.cFunk.mArrayRedim;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cFunk.mTextLike;
import static it.fdg.lm.cKonst.eAppProps.kAutoConnect;
import static it.fdg.lm.cKonst.eAppProps.kZoomEnable;
import static it.fdg.lm.cKonst.nAppProps;



public final class cProgram3 {
    public static Context mContext;         //This instance
	private static cElemViewProps[] oaElementViews= new cElemViewProps[100];
    public static cSlider[] oSlider=new cSlider[20]  ;               //Slider widget controls 10 vertical and 10 horizontal
    public static cSignalView2 mySignal;        //The current signal view
    //Properties of the application

    public static BluetoothDevice[] oPairedDevices;
    public static Typeface oTextTypeface;
    private static String sMsgQueue="";
    public static boolean bDoRefresh=false;
    public static String sMsgLog="";          //String containin a all messages
    public static cGraphText oGraphText;
    private static Timer oTimer=new Timer();
    private static String[] sFileList;
    public static int mLoopTime;
    private static int _RefreshRate;            //Refresh rate of the screen and communication protocol
    private static int _Privileges;
    private static boolean _DesignMode;

    //SGetter for refresh rates
    public static void nRefreshRate(int i) {        //Adjust communication speed
        i=mLimit(60,i,1000);
        _RefreshRate=i;
        mMessage("New refresh rate " +i);
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
    private static String[] sBitViews;
    static cGestureListener oGlobalGestureDetector;   //170915

    public static float nSliderZoom=1f;
//  Protocol for devices
// public static SignalBase oSignalBase
    public static cProtocol3[] oaProtocols;
    public static String[] sDevices2={};           //Protocol names
    public static boolean bShowElemNameOnSlider=true;
    private static Handler handler= new Handler();

    public static boolean bDoRedraw;        //Request a redraw of visible screen
    private static boolean bRunning1=false;  //Flag the running state
    public static Object oFocusdActivity ;      //Currently focused activity
    public static String sFile_ProtCfg ="";
    public static String sFile_AppCfg ="lm_config";
    public static String[] sProtDesc={"Revision","Description","Remark"};
    private static long nStartTime;

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
        bDoRedraw=true;
    }

    public static int mPrivileges() {
        return _Privileges;
    }

    //          METHODS
    protected static void mCommunicate(boolean bRun) {       //Avoid that this is called multiple times
        if (bRunning1==bRun)    //No change, will assure only one thread
            mMsgLog("Already running mCommunicate");
        else if (bRun) {
            bRunning1=true;
            mPeriodicProcessing.run();      //Start the timer
            oTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    nStartTime = System.currentTimeMillis();
                    nTestCount[1]++;
                    Thread.currentThread().setName("Prot_Async");
                    if (oaProtocols!=null)
                    for (int i = 0; i <oaProtocols.length ; i++) {
                        if (oaProtocols[i] != null) oaProtocols[i].mProcess_Async();    //Non gui
                    }
                    mLoopTime =cFunk.mAverage(mLoopTime, (System.currentTimeMillis() - nStartTime));
                    if (bRunning1 == false) {
                        this.cancel();
                    }
                }
            }, 1000, nRefreshRate());
        } else {
            bRunning1=false;
            mMessage("Not running");
        }
    }       //set Run/Pause of device communication
    private static Runnable mPeriodicProcessing = new Runnable() {
        @Override
        public void run() {
            if(bRunning1 ) mPeriodicProcessingExec();
        }
        private void mPeriodicProcessingExec() {                    //This will repeat itself
            cProgram3.mMainProcess1();
            if (bRunning1)
                handler.postDelayed(this, nRefreshRate());//Set timer for next call of this process
            else
                mMessage("Stopped");
        }
    };


    public static void mRedraw() {  //// Redraw displays
        fMain.mRefresh_DispMain(true);     //principal redraw
    }

    public static boolean mMainProcess1() {
        nTestCount[0]++;
        //mMsgStatus(     "M:"+nTestCount[0]+"D:"+nTestCount[1]);
        if (bDoRefresh|bDoRedraw) {
            fMain.mRefresh_DispMain(bDoRedraw);       //Syncrhornize display update with async process
            bDoRedraw=false;    //Redrawing done
            bDoRefresh=false;
        }
        return (0< nRefreshRate());            //170315    Returns false when program is to be ended
    }    //  Main process for device communication entry

    public static void mEndProgram(){    //Shut down gracefully    */
        mAppSettings(false);
        if (            oaProtocols==null) return;
        for (int i = 0; i< oaProtocols.length; i++){        //For each device
            oaProtocols[i].mEnd();                          //Close bluetooth ports
            oaProtocols[i]=null;
        }
        oaProtocols=null;
        // mDefault4App(true);
    }           //(Meagre)   Gracefully shut down the application


//  *******************************Settings management*********************
public static void mAppSettings(boolean bGet) {
    // Persistent data for the application. Get/Set current values to static storage
    mMessage("App settings:"+ sFile_AppCfg);
    int nKeys = mPrefFileNameSet(sFile_AppCfg);
    if (nKeys<1) {    //File not exising
        mMessage("Reading factory settings for :"+ sFile_AppCfg);
        String s =cFileSystem.mFileRead_Raw(sFile_AppCfg);
        cFileSystem.mStr2Pref(s);
    }
    //   A: Application settings 170825
    //Viewing
    nPalette =mPersistHex(bGet,"Palette", nPalette);   //Color palette for controls
    nSliderZoom= mPrefs5(bGet, "SliderSize", nSliderZoom);   //Reduction factor for views
    _Privileges = mPrefs5(bGet,"Privileges", _Privileges);   //Permissions of the user
    nAppProps= mPrefs5(bGet,"Properties",nAppProps);   //Permissions of the user
    nRefreshRate( mPrefs5(bGet,"RefreshRate", nRefreshRate()));   //Permissions of the user
    _DesignMode= ( mPrefs5(bGet,"DesignMode", _DesignMode));   //Permissions of the user
    sFile_ProtCfg =mPrefs5(bGet,"CurrentProtocolFile", sFile_ProtCfg);
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
        for (int i = 0; i< oaProtocols.length; i++) {
          oaProtocols[i].mSettings(bGet);      //Load settings for each protocol
        }
        //Load/save    View settings
        for (int i = 0; i < sWidgetIds.length; i++){       //170823 revised with a list of used widgets
            cElemViewProps oa = getElementViewById(sWidgetIds[i]);
            oa.mViewSettings3(bGet,sWidgetIds[i]);
        }
  }             //Save/Load settings from persistent storage


    //          MESSAGES
    public static void mMessage(String msg){
        mMsgLog(msg);
        mAlert1(msg);
    }



    static void mMsgLog(String msg){        //Adds message to a log
        if (mPrivileges()>0)
            sMsgLog =sMsgLog+ msg+'\n';
    }

    public static void mErrMsg(String msg) {
        mMsgLog(msg);
        mAlert1(msg);
        bDoRedraw=true;
    }

    public static void mMsgStatus(String msg){      //Display current status of protocol
        mMsgLog(msg);
        BaseActivity.mStatus(msg);
        bDoRefresh=true;
    }
    public static void mMsgDebug(String msg){      //Display current status of protocol
        mMsgLog(msg);
        if (nUserLevel1(cKonst.bitmask.kDebug)) mMessage(msg);
    }
    static void mAlert1(final String msg){
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

    public static cElemViewProps getElementViewById(String sId) {       //170727 return a valid static EV object by ID
        int idx=0;
        for (int i = 0; i<oaElementViews.length; i++){
                if (oaElementViews[i]==null) {
                    idx = i;
                    break;
                } else if (cFunk.mTextLike(oaElementViews[i].myId1,sId)){
                    idx = i;
                    break;
                }
            }
        if (oaElementViews [idx]==null){
            cElemViewProps oa = new cElemViewProps();
            oa.myId1=sId;
            oaElementViews [idx]=oa;
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

    public static cElemViewProps[] oControls() {
        return oaElementViews;
    }   //Return element views

    public static void mRedraw0() {  //// Redraw displays
        fMain.mRefresh_DispMain(true);     //principal redraw
    }

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
        cElemViewProps oa = getElementViewById(sId);     //Get or make oaElementViews[i]
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
            if (oaElementViews[i] == null) break;
            oaElementViews[i].mRefresh( doRedraw);
        }
    }



    public static cProtElem mElementByName(String protName, String elemName) {
        int i = nProtocol_IdxByName(protName);
        if (i<0){                      return null;}
        cProtElem e = oaProtocols[i].mGetElemByName(elemName);
        return e;
    }

    private static int nProtocol_IdxByName(String protName) {
        for (int i = 0; i <oaProtocols.length ; i++) {
            if (mTextLike( oaProtocols[i].sProtName(),protName))
                return i;
        }
        return 0;
        }


}
