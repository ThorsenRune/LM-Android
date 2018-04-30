//Doc:  https://docs.google.com/document/d/1BpYIuSab_7Fx0CpxHqXR0yQIb85Qv8Js-EHtS6rRB0I/edit
//171006    Total Refactoring
package it.fdg.lm;

//170919        170912 kAutoconnect parameter on restart
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//170823    Rewriting loading of the widget settings

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;

import java.util.ArrayList;

import static it.fdg.lm.cAndMeth.mSetVisibility;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.bDoRefresh;
import static it.fdg.lm.cProgram3.mAlert2;
import static it.fdg.lm.cProgram3.mAppSettings;
import static it.fdg.lm.cProgram3.mCommunicate;
import static it.fdg.lm.cProgram3.mEndProgram;
import static it.fdg.lm.cProgram3.mPersistAllData;
import static it.fdg.lm.cProgram3.mySignal;
import static it.fdg.lm.cProgram3.nPanelSizes;
import static it.fdg.lm.cProgram3.oGlobalGestureDetector;
import static it.fdg.lm.cProgram3.oUInput;
import static it.fdg.lm.cProgram3.oaProtocols;
import static it.fdg.lm.cProgram3.sDevices2;
import static it.fdg.lm.cProgram3.sFile_ProtCfg;
import static java.lang.Integer.signum;


public class fMain extends BaseActivity {

    public static ViewGroup fPanelHorizSliders,fPanelVertSliders,fPanelSignals1,fPanelData, fVertSliderPane1;
    public static ViewGroup mySliderPane; //Panel of the sliders
    private static Button cmdCommand1;
     private int  nTextSize=0;


    //******************************    MAIN METHODS  ENTRYPOINT
    public void mEntryPoint() {
        //!?mContext = this;
        fVertSliderPane1 = (ViewGroup)    findViewById(R.id.idHorizontalScroll);
        fPanelHorizSliders = (ViewGroup)    findViewById(R.id.idContainer4HorizontalSliders);
        fPanelVertSliders = (ViewGroup)     findViewById(R.id.idVerticalSliderPane);
        mySliderPane = (ViewGroup)          findViewById(R.id.idVerticalSliderPane);        //Container for the sliders
        mySignal = (cSignalView2)           findViewById(R.id.idSignalView);
        cmdCommand1 = (Button)              findViewById(R.id.idCommand);
        cmdCommand1.setOnClickListener(v->{cUInput.mCommand(true);});
        cmdCommand1.setOnLongClickListener((View v) -> oUInput.mCommandLongPress());
        fPanelSignals1 = (ViewGroup)        findViewById(R.id.idContainer4Signals1);
        fPanelData = (ViewGroup)            findViewById(R.id.idContainer4Data1);
        cAndMeth.mInit(this);       //Initialize the general methods
        mInit(this);                //Will only run first time ignoring second calls
        mInitControls();                    //Register  widgets in cProgram3.oaWidgetList();
        oUInput.mInit(this);         //  initialise the input module
        cKonst.nTextSize = (int) (mGetTextSize() );     //12 points
        cProgram3.oGraphText = new cGraphText();
        cProgram3.oGraphText.mInit((int) cKonst.nTextSize);         //Enable drawing of texts
        cmdText("Command/information button");
        //cProgram3.mPrivileges(0);     //180328B
        if (sDevices2[0].length()<1) cProgram3.mLoadFactorySettings(sFile_ProtCfg);          //first run
        if (sDevices2[0].length()<1) mLoadFactoryDefault();          //first run
    }
    public  static void mInit(Context mainContext){     //cProgram3
        if (oaProtocols !=null)
            return;        //Return if its not the first call (second calls could be caused by rotating the device)
        cProgram3.mContext=mainContext;
        cFileSystem.mInit(mainContext);
        //Prepare the protocols
        mAppSettings(true);
        mPersistAllData(true,sFile_ProtCfg);         //onCreate
    }

    private static void mLoadFactoryDefault() {
        oUInput.mSelectFactoryFile();
    }

    public void mInitControls() {      //Setup widget references for this display
  /*     initiate  views        */
       cProgram3.oTextTypeface =cmdCommand1.getTypeface();
        mySignal.mInit(1);      //One signal pane
//       mCloneView(view ,7);   //Make add 8 sliders 170914
        mSetTouchListeners();
        ArrayList<View> aW = cAndMeth.mAllChildViews(null, (ViewGroup) findViewById(android.R.id.content));
        cSlider.nSliderCount=0;         //Initialize the sliderarray 170915
        for (int i=0;i<aW.size();i++){      //Iterate trhoug all views in activity to assign controls
            if (aW.get(i) instanceof cSliderView ){
                cSlider.mAddSlider(this,(cSliderView) aW.get(i)); //Add new slider to sliderarray
                mTouchListening( aW.get(i));     //Set the touchhandler
            }

        }
        mAddControl("WD1",R.id.idWD1);          //Set Data views
        mAddControl("WD2",R.id.idWD2);
        mAddControl("WD3",R.id.idWD3);

        mTouchListening((View) fPanelVertSliders);     //Set the touchhandler
        mTouchListening((View) fPanelVertSliders.getParent());     //Set the touchhandler
        mTouchListening((View) mySliderPane.getParent());     //Set the touchhandler
        mTouchListening((View) mySliderPane);     //Set the touchhandler
// Signal touch
        mTouchListening((View) fPanelSignals1);     //Set the touchhandler
        mTouchListening((View) mySignal);     //Set the touchhandler
    }       //Initialize controls on this view

    private void mAddControl(String sID, int idWD) {   //171130    Add a control to the control array
        cData_View v = (cData_View) findViewById(idWD);
        v.mInit(this,sID    );
        cProgram3.oControls_Add(v,sID);
        mTouchListening(v);
    }

    private void mSetTouchListeners() {
        oGlobalGestureDetector =new cGestureListener(this);   //170915
        mTouchListening(mySliderPane);
        mTouchListening(mySignal);
        int count = mySliderPane.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mySliderPane.getChildAt(i);
            mTouchListening(child);
            if (child instanceof HorizontalScrollView){
                mTouchListening(child);
            }
        }
    }

    private void mTouchListening(final View view) {     //Activated for each control that can be activated by touch
        view.setOnTouchListener(new View.OnTouchListener() {         //Touch event for the watch pane

            public boolean onTouch(View v, MotionEvent event) {
                // ... Respond to touch events
                //If a false is returned the event will stop and not progress
                boolean bRetVal=false;
                if (view.equals(fPanelVertSliders)) {
                    bRetVal = oGlobalGestureDetector.mListen((View) view, event);
                    if (oGlobalGestureDetector.bFlingLR())
                        mShiftWatchPane(signum(oGlobalGestureDetector.nFlingDir[0]));
                } else if (view.equals(fPanelSignals1.getParent()) ) {
                    //              bRetVal = oGlobalGestureDetector.mListen((View) view.getParent(), event);
                } else if (view.equals(fPanelSignals1)){
                    bRetVal = oGlobalGestureDetector.mListen((View) mySignal, event);
                } else if (view instanceof HorizontalScrollView) {
                    //Scrollview is child of sliderpane
                    bRetVal= oGlobalGestureDetector.mListen( (View) view.getParent(), event);
                 } else if (view instanceof cSignalView2) {
                    bRetVal= oGlobalGestureDetector.mListen( (View) view, event);
                    if (oGlobalGestureDetector.bLongPress())
                        //nRefreshRate(5000);      //Slow down the refresh rate
                    if (oGlobalGestureDetector.bSingleTap()) {
                        ((cSignalView2)view).mShowCoord(oGlobalGestureDetector.nX, oGlobalGestureDetector.nY);

                    }
                    if (oGlobalGestureDetector.bInputGesture()) {           //Long or doubletap a slider will activate value input
                        oUInput.mInputRange();
                    }else if (oGlobalGestureDetector.bFlingLR()){
                        mySignal.mShiftPane(signum(oGlobalGestureDetector.nFlingDir[0]));  //Change the pane in direction -1,1
                    }else if (oGlobalGestureDetector.bScaling()) {
                        mySignal.oElemViewProps().mZoom(oGlobalGestureDetector.nScaleCenterX, oGlobalGestureDetector.nScaleCenterY, oGlobalGestureDetector.nScaleFactor);
                        mySignal.oElemViewProps().myProtElem1().mCentreAround(0);
                    }

                }else if ((view instanceof cSliderView)) {
                    cSliderView V = (cSliderView) view;
                    bRetVal= oGlobalGestureDetector.mListen(view, event);
                    if (oGlobalGestureDetector.bScaling()) {       //Check global gestures first
                        if (cUInput.mGetViewProps().bZoomAble())
                        cUInput.mGetViewProps().mZoom(oGlobalGestureDetector.nScaleCenterX, oGlobalGestureDetector.nScaleCenterY, oGlobalGestureDetector.nScaleFactor);
                    } else if (oGlobalGestureDetector.bInputGesture()) {           //Long or doubletap a slider will activate value input
                        oUInput.mInputValue1();
                    } else if (oGlobalGestureDetector.bFlingDown()){
                        if (V.bRotate)  //Only fling zero vertical sliders. 180417A
                         oUInput.mZero();
                    } else
                        bRetVal = V.onTouchEvent(event);              //Perform sliding movements
                    return  bRetVal;           //The event was processed
                }
                return bRetVal;
            }
        });
    }
    private void mShiftWatchPane(int signum) {
        cProgram3.nWatchPage=mLimit(0,(cProgram3.nWatchPage+signum),1);
        cProgram3.bDoRedraw=true;
    }

    public void cmdCommand1(View v){
        cUInput.mCommand(true);
    }

    public static void mRefresh_DispMain(boolean doRedraw) {       //Refresh controls on display
        if (cProgram3.mySignal==null) return;   //Display not ready
        if (doRedraw) mRedraw();
        if (doRedraw){
            cProgram3.mySignal.mInit(1);
        }
        if (cProgram3.oFocusdActivity instanceof cSetupFile){
//            ((cSetupFile)oFocusdActivity).mDispRefresh(doRedraw);
        } else if (cProgram3.oFocusdActivity instanceof cBitField) {
            ((cBitField) cProgram3.oFocusdActivity).mRefresh(doRedraw);
        } else {
            cProgram3.mControlsRefresh(doRedraw);
//            cUInput.mRefresh(doRedraw);       //Chrashes
            for (int i = 0; i < cSlider.nSliderCount; i++) {
                cProgram3.oSlider[i].mRefresh(doRedraw);
            }

            if (cProgram3.mySignal != null) {
                cProgram3.mySignal.mRefreshSignal(doRedraw);
            }
            if (bDoRedraw) oUInput.mCommand(false);
        }
    }

    private static void mRedraw() {     //Redrawing the main window
        cAndMeth.mLayoutWeightSet(fVertSliderPane1, nPanelSizes[0]);

        cAndMeth.mLayoutWeightSet(fPanelHorizSliders, nPanelSizes[1]);

        mSetVisibility(fPanelSignals1,cProgram3. nPanelSizes[2]>0);
        cAndMeth.mLayoutWeightSet(fPanelSignals1, nPanelSizes[2]);
        mySignal.setVisibility(fPanelSignals1.getVisibility());

        mSetVisibility(fPanelData,cProgram3.bPanelData);
        cAndMeth.mLayoutWeightSet(fPanelData, nPanelSizes[3]);

        //mScrollViewBlock(fVertSliderPane1);

        mStatusRedraw();
    }



    public static void mCommandSet( String sMsgCmd) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                if (sMsgCmd == "")
                    mSetVisibility(cmdCommand1, 0);
                else {
                    mSetVisibility(cmdCommand1, 1);
                    cmdCommand1.setText(sMsgCmd);
                }
            }
        });
    }


    //  *************************                    SYSTEM STUFF       *************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fmain);
        cProgram3.mInit(this);
        mCommandSet("Starting LM");
        mInitWindow();
    }

    public void mInitWindow() {
        cProgram3.oFocusdActivity=this;             //Default value for acti
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mEntryPoint();
    }

    @Override
    public void onResume() {
        super.onResume();
        cProgram3.bDoRedraw=true;             //Update the screen
        cProgram3.oFocusdActivity=this;
        mCommunicate(true);
    }
    @Override
    protected void onDestroy() {            //Last event before program is killed, but also called sometimes when another activity is ending?
//        if (isFinishing())             mAlert2("Finishing onDestroy");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {       //Some black magic asking user how to quit, saving/not saving data
        mEndProgram();
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {

        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            mAlert2("Bye from LM" );
            mEndProgram();
        }
    }


    public int mGetTextSize() {
        if (nTextSize==0)
        nTextSize= (int) cmdCommand1.getTextSize();
        //nTextSize= getResources().getDimensionPixelSize(R.dimen.myFontSize);
        return nTextSize;
    }

    public static void cmdText(String s) {      //Set text for command button
        mCommandSet(s);
        bDoRefresh=true;
    }


}
