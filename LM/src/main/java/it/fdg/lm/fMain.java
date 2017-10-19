//Doc:  https://docs.google.com/document/d/1BpYIuSab_7Fx0CpxHqXR0yQIb85Qv8Js-EHtS6rRB0I/edit
//171006    Total Refactoring
package it.fdg.lm;

//170919        170912 kAutoconnect parameter on restart
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//170823    Rewriting loading of the widget settings

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;

import java.util.ArrayList;

import static it.fdg.lm.cAndMeth.mMatchChildVisibility;
import static it.fdg.lm.cAndMeth.mSetVisibility;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.mMsgDebug;
import static it.fdg.lm.cProgram3.mPersistAllData;
import static it.fdg.lm.cProgram3.mRefreshRate;
import static it.fdg.lm.cProgram3.mySignal;
import static it.fdg.lm.cProgram3.oGlobalGestureDetector;
import static it.fdg.lm.cProgram3.oProtocol;
import static java.lang.Integer.signum;


public class fMain extends BaseActivity {

    public static ViewGroup fPanelHorizSliders,fPanelVertSliders,fPanelSignals1;
    public static ViewGroup mySliderPane; //Panel of the sliders
    private static Button cmdCommand1;


    //******************************    MAIN METHODS  ENTRYPOINT
    private void mEntryPoint() {
        //!?mContext = this;
        fPanelHorizSliders = (ViewGroup)    findViewById(R.id.idContainer4HorizontalSliders);
        fPanelVertSliders = (ViewGroup)     findViewById(R.id.idVerticalSliderPane);
        mySliderPane = (ViewGroup)          findViewById(R.id.idVerticalSliderPane);        //Container for the sliders
        mySignal = (cSignalView2)           findViewById(R.id.idSignalView);
        cmdCommand1 = (Button)              findViewById(R.id.idCommand);
        fPanelSignals1 = (ViewGroup)        findViewById(R.id.idContainer4Signals1);
        cAndMeth.mInit(this);       //Initialize the general methods
        mInit(this);                //Will only run first time ignoring second calls
        mInitControls();            //Prepare widgets for display
        cUInput.mInit(this);         //  initialise the input module
        cProgram3.mRedraw();
        cKonst.nTextSize = (int) (mGetTextSize() );     //12 points
        cProgram3.oGraphText = new cGraphText();
        cProgram3.oGraphText.mInit((int) cKonst.nTextSize);         //Enable drawing of texts
    }
    public  static void mInit(Context mainContext){     //cProgram3
        if (oProtocol!=null)
            return;        //Return if its not the first call (second calls could be caused by rotating the device)
        cProgram3.mContext=mainContext;
        cFileSystem.mInit(mainContext);
        //Prepare the protocols
        mPersistAllData(true);         //onCreate
        cProgram3.nUserLevel(0);        //Remove privileges as default
        cProgram3.mAppPropsSet(cKonst.eAppProps.kRefreshRate,2000);
    }
    private void mInitControls() {      //Setup widget references for this display
  /*     initiate  views        */

//170920 did not work very well        nTextSize=cmdCommand1.getTextSize();    //170914 use this as a reference for the applciation font size
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
        mTouchListening((View) fPanelVertSliders);     //Set the touchhandler
        mTouchListening((View) fPanelVertSliders.getParent());     //Set the touchhandler
        mTouchListening((View) mySliderPane.getParent());     //Set the touchhandler
        mTouchListening((View) fPanelSignals1);     //Set the touchhandler
        mTouchListening((View) mySliderPane);     //Set the touchhandler
        mTouchListening((View) mySignal.getParent());     //Set the touchhandler
        mTouchListening((View) ((View) mySignal.getParent()).getParent());     //Set the touchhandler on topcontainer
    }       //Initialize controls on this view

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
                boolean bRetVal=false;
                if (view.equals(fPanelVertSliders)) {
                    bRetVal = oGlobalGestureDetector.mListen((View) view, event);
                    if (oGlobalGestureDetector.bFlingLR())
                        mShiftWatchPane(signum(oGlobalGestureDetector.nFlingDir[0]));
                } else if (view.equals(fPanelSignals1.getParent()) ){
                    bRetVal = oGlobalGestureDetector.mListen((View) view.getParent(), event);
                } else if (view instanceof HorizontalScrollView) {
                    //Scrollview is child of sliderpane
                    bRetVal= oGlobalGestureDetector.mListen( (View) view.getParent(), event);
                 } else if (view instanceof cSignalView2) {
                    bRetVal= oGlobalGestureDetector.mListen( (View) view, event);
                    if (oGlobalGestureDetector.bLongPress())
                        mRefreshRate(5000);      //Slow down the refresh rate
                    if (oGlobalGestureDetector.bSingleTap()) {
                        ((cSignalView2)view).mShowCoord(oGlobalGestureDetector.nX, oGlobalGestureDetector.nY);
                        mRefreshRate(100);       //Speed up refresh rates
                    }
                    if (oGlobalGestureDetector.bFlingLR())
                        mySignal.mShiftPane(signum(oGlobalGestureDetector.nFlingDir[0]));  //Change the pane in direction -1,1
                    if (oGlobalGestureDetector.bScaling())
                        mySignal.oElemViewProps().mZoom(oGlobalGestureDetector.nScaleCenterX, oGlobalGestureDetector.nScaleCenterY, oGlobalGestureDetector.nScaleFactor);

                }else if ((view instanceof cSliderView)) {
                    cSliderView V = (cSliderView) view;
                    bRetVal= oGlobalGestureDetector.mListen(view, event);
                    if (oGlobalGestureDetector.bScaling()) {       //Check global gestures first
                        mMsgDebug("Scaling gesture 171003");
                    } else if (oGlobalGestureDetector.bInputGesture()) {           //Long or doubletap a slider will activate value input
                        cUInput.mInputValue(true);
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

        if (doRedraw){
            cProgram3.mySignal.mInit(1);
        }
        if (cProgram3.oFocusdActivity instanceof cSetupFile){
//            ((cSetupFile)oFocusdActivity).mDispRefresh(doRedraw);
        } else if (cProgram3.oFocusdActivity instanceof cBitField) {
            ((cBitField) cProgram3.oFocusdActivity).mRefresh(doRedraw);
        } else {
            cUInput.mRefresh(doRedraw);
            for (int i = 0; i < cSlider.nSliderCount; i++) {
                cProgram3.oSlider[i].mRefresh(doRedraw);
            }
            if (doRedraw ) {
//                mMatchChildVisibility(fPanelVertSliders);    //Copy visibility from child views
                mMatchChildVisibility(fPanelHorizSliders);    //Copy visibility from child views
            }
            if (cProgram3.mySignal != null) {
                cProgram3.mySignal.mRefreshSignal(doRedraw);
            }

        }
    }

    public static void cmdText(String msg) {
        if (cmdCommand1==null) return;          //Only when visible
        if (msg=="")
            mSetVisibility(cmdCommand1,0);
        else {
            mSetVisibility(cmdCommand1, 1);
            cmdCommand1.setText(msg);
        }
    }

    //  *************************                    SYSTEM STUFF       *************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fmain);
        cProgram3.oFocusdActivity=this;             //Default value for acti
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cProgram3.mContext=this;
        mEntryPoint();

    }
    @Override
    public void onResume() {
        super.onResume();
        cProgram3.bDoRedraw=true;             //Update the screen
        cProgram3.mCommunicate(true);
        cProgram3.oFocusdActivity=this;
    }
    @Override
    protected void onDestroy() {            //Last event before program is killed, but also called sometimes when another activity is ending?
        cProgram3.mCommunicate(false);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {       //Some black magic asking user how to quit, saving/not saving data
        cProgram3.mEndProgram();
        mMessage("Ending program");
        finish();
        super.onBackPressed();

    }

    @Override

    protected void onPause() {
        cProgram3.mCommunicate(false);      //Slowdown the refresh rate
        super.onPause();
    }

    public int mGetTextSize() {
        return getResources().getDimensionPixelSize(R.dimen.myFontSize);

    }
}