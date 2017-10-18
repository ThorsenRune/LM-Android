//170915//170913
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1HifKNCBKfcND_UEkMDpHDncXhb8YZQW3u3QEk5SZsp8/edit
/**
 * A  slider control with up to 4 handles handle
 * Created by rthorsen on 170909.
 */
package it.fdg.lm;
//   Controls showing  for controlling variables as  horizontal and vertical sliders with up to 4 handles .
//      Can be enabled or hidden, each handle has its own range and is connected to an element&index

import android.content.Context;
import android.view.View;

import static it.fdg.lm.cAndMeth.mSetVisibility;
import static it.fdg.lm.cFunk.mStr2Int;
import static it.fdg.lm.cProgram3.bDesignMode;
import static it.fdg.lm.cProgram3.getElementViewById;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.nWatchPage;
import static it.fdg.lm.cProgram3.oSlider;


public class cSlider {
    public static int nSliderCount = 0;
    public cSliderView oSliderView = null;                //The slider control
    cElemViewProps[] oElemViewProps;            //Visual properties for the handles of this control
    int nCurrentViewPropsIdx = 0;
    private String[] sElemViewPropsIds;         //Identifiers for retrieving objects
    // to check
    private Context mContext;
    private int myIndex;                            //Index in sliderarray
    private int bVisibility;
    private int mySliderMax;
    private String mySliderTypeName;            //Slider identification name WS,WH,WV
    //PROPERTIES
    private String myId1;                            //Unique id of this control

    //..................................Getters and setters.................................
    private cProtElem oElement(int i) {             //170914    Safe return of the element object
        if (oElemViewProps[i] == null) return null;
        return oElemViewProps[i].oGetElem();
    }       //Return the i'th element

    public cSlider(Context context) {
        mContext = context;
    }       //Constructor

    //  A method that constructs a new a new slider
    public static void mAddSlider(Context mContext, cSliderView newSliderView) {
        oSlider[nSliderCount] = new cSlider(mContext);        //Create a new class holding the control
        oSlider[nSliderCount].myIndex = nSliderCount;      //170914 give it a unique index identifying it in oSlider array
        oSlider[nSliderCount].oSliderView = newSliderView;
        oSlider[nSliderCount].oSliderView.oParent = oSlider[nSliderCount];    //170914    Set the parent
        nSliderCount = nSliderCount + 1;                    //170914    Finally incremente the count of sliders
        if (nSliderCount >= oSlider.length) mErrMsg("170915 Fatal error, redim your slider");
        // to retrieve the entry name associated to a resId  (getIDName)    ref:https://stackoverflow.com/questions/14647810/easier-way-to-get-views-id-string-by-its-id-int
        //String s= newSliderView.getResources().getResourceEntryName(newSliderView.getId())
    }   //Make a new slider

    //Getters and Setters
    public cElemViewProps mGetCurrentElementView() {    //Return a reference to the current views properties
        nCurrentViewPropsIdx = oSliderView.nActiveIdx;
        return oElemViewProps[nCurrentViewPropsIdx];
    }
    public void mApplySameScale(){      //Apply the same scale to all
        if (oElemViewProps[oSliderView.nActiveIdx].oGetElem()==null ) return;
        float[] r = oElemViewProps[oSliderView.nActiveIdx].oGetElem().nDisplayRange;
        for (int i=0;i<oElemViewProps.length;i++){
            if (oElemViewProps[i].oGetElem()!=null)
            oElemViewProps[i].oGetElem().nDisplayRange=r;
        }
    }

    // get viewsettings objects associated with this control
    private void mInitElements(String[] sElemViewPropsIds) {
        oElemViewProps = new cElemViewProps[sElemViewPropsIds.length];
        oSliderView.mSetHandles(sElemViewPropsIds.length);        //Set number of handles
        for (int i = 0; i < oElemViewProps.length; i++) {
            oElemViewProps[i] = getElementViewById(sElemViewPropsIds[i]);     //Fetch a view properties object with this id
        }
//!- 170913 handle visibility in mRedraw        oElemViewProps[0].mRedraw(oSliderView);	//
        mSetVisibility(oSliderView, oElemViewProps[0].bVisible());    //170913 First element determines the visibility of the control
    }

    private void mGetMyId() {
        String s = cProgram3.getMyIdName(oSliderView);  //Get the $$# expression identifying the control
        if (s.matches("W[HV][0-9]*")) {
            myIndex = mStr2Int(s.substring(2, 3));
            mySliderTypeName = s.substring(0, 2);
        }
        myId1 = mySliderTypeName + myIndex;    //Create ID for the control adding the page
        sElemViewPropsIds = new String[]{myId1 + "A" + nWatchPage, myId1 + "B" + nWatchPage, myId1 + "C" + nWatchPage, myId1 + "D" + nWatchPage};   //Make a unique string of view props names
    }               //Get the id of the control

    private void mSetcontrolScale() {
        if (oElement(0) == null) return;
        float nRange = oElemViewProps[0].mDispRange();
        if (nRange > 0) oSliderView.nScaleMax = nRange;
    }

    ////            DRAWING
    void mRedraw() {    //170913  Get view properties for up to 4 handles
        mGetMyId();       //Will change ID to be given by XML ID
        mInitElements(sElemViewPropsIds);        //Find view properties object by id 170913
        oSliderView.bDesignMode = bDesignMode();
        for (int i = 0; i < oElemViewProps.length; i++) {
            if (oElemViewProps[i].oGetElem() != null) {
                oSliderView.mSetHandleColor(i, oElemViewProps[i].mForeColor());
                oSliderView.mSetIntervalColor(i, oElemViewProps[i].mBackColor());
                oSliderView.mSetDescription(i, oElemViewProps[i].mAlias());
                oSliderView.mEnabled(i, oElemViewProps[i].bEnabled());
                oSliderView.mVisible(i, oElemViewProps[i].bVisible());
            }
        }
        mSetcontrolScale();
        oSliderView.mRedraw();

    }                               //Redraw the control

    public void mRefresh(boolean doRedraw) {        //Get the data from device and update the slider
        if (doRedraw) mRedraw();
        if (oSliderView.getVisibility() != View.VISIBLE) return;    //Dont update invisible objects
        //Move this to event !!!
        if (oSliderView.bChangedByUser) {       //Write dirty data to device
            oSliderView.bChangedByUser = false;
            mCtlValueWrite2Device();                  //Write control data to device
        }
        for (int i = 0; i < oElemViewProps.length; i++) {
            if (oElemViewProps[i] == null) return;       //No view properties
            if (oElemViewProps[i].oGetElem() != null) {
                float v = oElemViewProps[i].mDevice2WxValue(oSliderView.nScaleMax);      //170910    Get value in pixels

                oSliderView.mValueInPx(i, v);            //170910 Set the slider value
                String s = oElemViewProps[i].mGetValueText();
                oSliderView.mValueText(i, s);        //170910 Set the text of this value
            }
        }
        oSliderView.invalidate();   //Will launch onDraw->mRefresh
    }       //Refresh the control

    private void mCtlValueWrite2Device() {
        float[] v = oSliderView.mGetAllValues();
        for (int i = 0; i < oElemViewProps.length; i++) {
            if (oElemViewProps[i] != null)
                if (oElemViewProps[i].bVisible())
                    if (oElemViewProps[i].bEnabled())      //No view properties
                        oElemViewProps[i].mWxValue2Device(v[i], oSliderView.nScaleMax);
        }
    }           ////Write control data to device


}
