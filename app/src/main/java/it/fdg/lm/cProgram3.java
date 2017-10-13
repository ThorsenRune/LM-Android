//171003    Refactoring and cleaning
//170825
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1IE3Ob0GmIqLZTRY9gOeGrDasWwAm9jiih5rrskf6Q2U/edit

package it.fdg.lm;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;

import static it.fdg.lm.cAndMeth.mGetDateStr;
import static it.fdg.lm.cDebug.nTestCount;
import static it.fdg.lm.cFileSystem.mPersistHex;
import static it.fdg.lm.cFileSystem.mPrefs5;
import static it.fdg.lm.cFileSystem.mStr2Pref;
import static it.fdg.lm.cFileSystem.readRawTextFile;
import static it.fdg.lm.cFileSystem.sCurrPrefFileName;
import static it.fdg.lm.cFileSystem.setsPrefFileName;
import static it.fdg.lm.cFunk.mArrayFind;
import static it.fdg.lm.cFunk.mArrayRedim;
import static it.fdg.lm.cFunk.mInt2Bool;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cKonst.eNum.kPrivileges;
import static it.fdg.lm.cKonst.eNum.kRefreshRate;
import static java.lang.Thread.currentThread;

;

public final class cProgram3 {
    public static Context mContext;         //This instance
	private static cElemViewProps[] oaElementViews= new cElemViewProps[100];
    public static cSlider[] oSlider=new cSlider[20]  ;               //Slider widget controls 10 vertical and 10 horizontal
    public static cSignalView2 mySignal;        //The current signal view
    //Properties of the application
    public static int[] nAppProps={0,0,0,0,0}; //170904level of permissions given to user
    public static BluetoothDevice[] oPairedDevices;

    public static Typeface oTextTypeface;
    private static int nRefreshRate=1000;
    public static int debug=10;
    private static String sMsgQueue;
    public static boolean bDoRefresh=false;
    public static String sMsgLog;          //String containin a all messages
    public static String sErrMsg;


    //SGetter for refresh rates
    public static void mRefreshRate(int i) {        //Adjust communication speed
        if (i<10000){
            nRefreshRate=i;
//            mMessage("New refresh rate " +nRefreshRate);
    }}
    public  static int mRefreshRate(){return nRefreshRate;}
    public static int mAppProps(cKonst.eNum kEnum) {
        int n=kEnum.ordinal();
        if (nAppProps.length<=n)
            nAppProps=mArrayRedim(nAppProps,n);     //Make sure that there is room of index n
        return  nAppProps[n];
    }
    public static void mAppPropsSet(cKonst.eNum kEnum, int i) {
        nAppProps[kEnum.ordinal()]=i;
    }
    public static void mAppPropsToggle(cKonst.eNum kEnum) {
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
        return mInt2Bool(mAppProps(cKonst.eNum.kShowHidden));
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
    public static cProtocol3[] oProtocol;
    public static String[] sDevices1={};           //Protocol names
    public static int nCurrentProtocol=0;
    public InputStream oRelayIn = null;

    private static int nProcessCycleCount=0;
    public static boolean bShowElemNameOnSlider=true;
    private static Handler handler= new Handler();
    private static Toast mToast;
    public static boolean bDoRedraw;        //Request a redraw of visible screen
    public static boolean bRunning;
    public static Object oFocusdActivity ;      //Currently focused activity
    private static String sProtFileName="LM_5";
    public static String[] sProtDesc={"Revision","Description","Remark"};
    private static long nNextRunTime;

    //--------------------------------- MAIN METHODS------------------------------
    //          PROPERTIES
    public static boolean bAdmin() {
        return ( nAppProps[kPrivileges.ordinal()]>0);
    }
    public static int nUserLevel() {
        return ( nAppProps[kPrivileges.ordinal()]);
    }
    public static boolean nUserLevel1(int level) {
        int bitmask = nAppProps[kPrivileges.ordinal()];
        return (0<( bitmask&level ));
    }
    //          METHODS

    public  static void mInit(Context mainContext){     //cProgram3
        if (oProtocol!=null)
            return;        //Return if its not the first call (second calls could be caused by rotating the device)
        mContext=mainContext;
        cFileSystem.mInit(mainContext);
        //Prepare the protocols
        mPersistAllData(true);         //onCreate
        //!-if (mAppProps(cKonst.eNum.kAutoConnect)>0)      moved to cProtocol3 171012
            oProtocol[nCurrentProtocol].mDoConnectRequest(null);      //170912 Autoconnect at user privileges
        cProgram3.mAppPropsSet(kRefreshRate,2000);
    }

    protected static void mCommunicate(boolean bRunPause) {       //Avoid that this is called multiple times
        if (bRunPause)    //Enable
            bRunning=bRunPause;
            mPeriodicProcessing.run();      //Start the timer
    }       //set Run/Pause of device communication

    public static void mRedraw() {  //// Redraw displays
        fMain.mRefresh_DispMain(true);     //principal redraw
    }

    public static boolean mMainProcess1() {
        nTestCount[0]++;
        if (bDoRefresh)
            fMain.mRefresh_DispMain(bDoRedraw);       //Syncrhornize display update with async process
        bDoRefresh=false;
        bDoRedraw=false;    //Redrawing done
        for (int i=0;i<oProtocol.length;i++){       //For each device
            oProtocol[i].mAsyncProcessing_Call();        //Asynchroneous processing
        }
        return (0< mAppProps(kRefreshRate));            //170315    Returns false when program is to be ended
    }    //  Main process for device communication entry

    public static void mEndProgram(){    //Shut down gracefully    */
        for (int i=0;i<oProtocol.length;i++){       //For each device
            oProtocol[i].mEnd();                    //Close bluetooth ports
        }
        // mDefault4App(true);
    }           //(Meagre)   Gracefully shut down the application

    public static void mPersistAllData (boolean bGet){
       /*   Persistent data for the application. Get/Set current values  to static storage
       *  setsPrefFileName("LM_AppSetting");     //Global settings
       *   sProtFileName=mPrefs5(bGet,"Protocol_Settings_File", sProtFileName);   //Current file for  protocol settings
       * */

        setsPrefFileName(sProtFileName); //Protocol settings clear old if writing
        if (false == bGet)
            cProgram3.sProtDesc[0]="LM Settings file: "+sCurrPrefFileName+"  Date:" + mGetDateStr() +"\n";
        else if (mPrefs5(true,"App.Widgets","").length()<=1)            //If file non existing
            mStr2Pref(readRawTextFile( R.raw.lm_5));       //Load factory setup

       //   A: Application settings 170825
        sProtDesc= mPrefs5(bGet,"App.Description",sProtDesc);
        sDevices1 = mPrefs5(bGet,"App.Devices", sDevices1);   //Device names (protocol names)  170823
        nAppProps= mPrefs5(bGet,"App.Properties",nAppProps);   //Permissions of the user
        sWidgetIds = mPrefs5(bGet,"App.Widgets", mGetListOfControls());   //IDs for widgets used (S# signal W# slider B# biteditor, # index
        //Viewing
       nPalette =mPersistHex(bGet,"App.Palette", nPalette);   //Color palette for controls
       nSliderZoom= mPrefs5(bGet, "App.SliderSize", nSliderZoom);   //Reduction factor for views

	//170913		Load/save the protocol elements
       if (bGet)mProtocolPrepare(sDevices1.length);
        for (int i=0;i<oProtocol.length;i++) {
            String sKey = sDevices1[i];
            if (sKey!="") {
                sKey = "Device." + sKey;         //Use protocol number & device as key to load
                oProtocol[i].mSetProtElementList(mPrefs5(bGet, sKey + ".Elements", oProtocol[i].mGetProtElementList()));   //
                oProtocol[i].mPersist_Protocol(bGet);      //Load settings for each protocol
            }
        }
        //Load/save    View settings
        for (int i = 0; i < sWidgetIds.length; i++){       //170823 revised with a list of used widgets
            cElemViewProps oa = getElementViewById(sWidgetIds[i]);
            oa.mViewSettings3(bGet,sWidgetIds[i]);
        }
  }             //Save/Load settings from persistent storage




    //          MESSAGES
    public static void mMessage(String msg){             mAlert(msg);    }
    static void mMsgLog(String msg){             mAlert(msg);    }
    public static void mErrMsg(String msg) {
        sErrMsg =msg;  bDoRedraw=true;      mAlert(msg);    }
    public static void mMsgStatus(String msg){      //Display current status of protocol
        if (bAdmin())
            mMessage(msg);
    }
    public static void mMsgDebug(String msg){      //Display current status of protocol
        if (nUserLevel1(cKonst.bitmask.kDebug)) mMessage(msg);
    }
    private static void mAlert(String msg){        //Worker function for messages
        if (bAdmin())
            sMsgLog+="/n"+msg;
        Context mContext= (Context) oFocusdActivity;
        if (msg.length()>0)
            sMsgQueue = sMsgQueue +"/n"+msg;       //Add  to message queue
        else
            msg="nothing";
        try {
            String[] sa = sMsgQueue.split("/n");
            for (int i=1;i<sa.length;i++) {
                Toast.makeText(mContext, sa[i], Toast.LENGTH_LONG).show();
                Log.d("LM:", sa[i]);
                if (sa[i].length()<2)
                    mErrMsg("Fejl");
            }
            sMsgQueue = "";
        }catch (Exception e)        {
            sMsgQueue +=" - T:"+currentThread().getName();
        }
    }

 //****************+            HELPERS   **************************

    private static Runnable mPeriodicProcessing = new Runnable() {
        @Override
        public void run() {
            if(bRunning ) mPeriodicProcessingExec();
        }
        private void mPeriodicProcessingExec() {                    //This will repeat itself
            if (nNextRunTime <System.currentTimeMillis()){      //Seems like postdelayed is not very accurate
                nNextRunTime =System.currentTimeMillis()+ nRefreshRate;
                cProgram3.mMainProcess1();
            }
            handler.postDelayed(this, nRefreshRate);//Set timer for next call of this process
        }
    };

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

    public static void mProtocolPrepare(int kMaxDevices) {
        oProtocol= new cProtocol3[kMaxDevices];
        for (int i=0;i<oProtocol.length;i++) {
            oProtocol[i] = new cProtocol3();
            oProtocol[i].mInit(i);
        }
    }

    public static cElemViewProps getElementViewById(String sId) {       //170727 return a valid static EV object by ID
        int idx=0;
        for (int i = 0; i<oaElementViews.length; i++){
                if (oaElementViews[i]==null) {
                    idx = i;
                    break;
                } else if (oaElementViews[i].myId1.equalsIgnoreCase(sId)){
                    idx = i;
                    break;
                }
            }
        if (oaElementViews [idx]==null){
            cElemViewProps oa = new cElemViewProps();
            oa.nIndexInContainerArray=idx;
            oa.myId1=sId;
            oa.mViewSettings3(true,sId);           //Load settings (shold already be there )
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

    public static cElemViewProps[] mAllElemViewProps() {
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
}
/*
    Call chain
    oProtocol[i].mInit(i)->kBT_ConnectReq
 */