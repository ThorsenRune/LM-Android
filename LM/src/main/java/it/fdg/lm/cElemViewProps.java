//170914
// 170912 new methods for getting setting data to device mWxValue2Device
//Rev 170822
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1PmV3QAI-XorvkpzFb9hwlBTSepLwzUU1ByUdpHtwy5E/edit
//170823    Moved bit visibility to cProtElem

package it.fdg.lm;
/*
  https://docs.google.com/document/d/1eeGwrdE59S3Z3p1D9HQ6eu-xdvaFKQ8z3KVqJv5zgWU/edit
  Object defining properties of visual representation of a protcol element
 */

import android.content.Context;
import android.view.View;

import static it.fdg.lm.cFileSystem.mPrefs5;
import static it.fdg.lm.cProgram3.bDesignMode;
import static it.fdg.lm.cProgram3.mAppProps;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.oFocusdActivity;
import static it.fdg.lm.cProgram3.oProtocol;
import static it.fdg.lm.cFunk.*;


public  class cElemViewProps {
    public String myId1;
    private View oOwner = null;               //The owner view
    private cProtElem oProtElem;
    public int nCurrDataIndex = 0;           //If scalar this is the cProtData.aData[index] to show
    private int nAutoRangeCount = 0;
    private float nRange;
    public int nIndexInContainerArray = 0;    //170914    index in the containing array
    private int nProperties;                //Properties of the view, enabled, ...



    public void mZoom(float scalePointX, float scalePointY, float nFactor) {    //171003 implementing zoom, todo implement centerpoint
        float na = oProtElem.nDisplayRange[0];
        float nb = oProtElem.nDisplayRange[1];
        float mid = (na + nb) / 2;
        float newrange = (nb - na)/ nFactor;
        float newA = mid - newrange / 2;
        float newB = mid + newrange / 2;
        oProtElem.nDisplayRange[0]=newA;
        oProtElem.nDisplayRange[1]=newB;
    }

    public String mScaleMaxStr() {
        float v = oProtElem.nDisplayRange[1];
        return mVal2Str(v);
    }
    public String mScaleMinStr() {
        return mVal2Str(oProtElem.nDisplayRange[0]);
    }
    public String mVal2Str(double v) {
        String u = oProtElem.getUnits();
        String s = String.format("%.1f ", v) + u;
        return s;
    }



    private enum ePropIdx {
        kVisible, kEditable, kWriteOnStart
    }
    //*****************+            IMPLEMENTATION  ***********************

    public void mViewSettings3(boolean bGetIt, String sWidgetId) { //Get/Set settings of this view with sId
        if (sWidgetId.length() < 1)
            return;          //Skip empty widgets
        myId1 = sWidgetId;
        _mElemBinding(bGetIt, sWidgetId + ".Name");
    }

    private void _mElemBinding(boolean bGetIt, String sKey) {
        String[] s = mPrefs5(bGetIt, sKey, mStr2StrArr(sElemName() + "," + nCurrDataIndex + "," + nProperties));
        s = mArrayRedim(s, 2);       //Make sure that it's there
        if (s[0].length() > 1)     //170914    If there is a name then find the relative object
            oProtElem = getByName(s[0]);
        if (oProtElem != null) {
            nCurrDataIndex = mStr2Int(s[1]);
            nProperties = mStr2Int(s[2]);     //Visibility of the control
            nRange = (oProtElem.nDisplayRange[1] - oProtElem.nDisplayRange[0]);
        }
    }


    private String sElemName() {            //Safely get element name assigned to the control
        if (oProtElem == null)
            return "";
        return oProtElem.sElemName();
    }


    public void mAutoRange(boolean doit) {     //https://docs.google.com/document/d/11nCREUKJt2hxSaZ5-icjiOPtkRpfNy2ZHuoLSSgvyMk/edit#heading=h.kbe9499vxmh1
        //Swap hig and low
        float nX = oProtElem.nDisplayRange[0];
        oProtElem.nDisplayRange[0] = oProtElem.nDisplayRange[1];
        oProtElem.nDisplayRange[1] = nX;
        nAutoRangeCount = 20;  //Number of samples to autorange over
        mAutoRange();
        nRange = (oProtElem.nDisplayRange[1] - oProtElem.nDisplayRange[0]);
    }

    private void mAutoRange() { //Autoscalin performed by calling mGetValue and having doAutoScale=true
        if (nAutoRangeCount < 1) return;
        for (int i = 0; i < oProtElem.nDataLength(); i++) {
            float nX = oProtElem.getVal(i);
            if (oProtElem.nDisplayRange[0] > nX) {
                oProtElem.nDisplayRange[0] = nX;
            }
            if (oProtElem.nDisplayRange[1] < nX) {
                oProtElem.nDisplayRange[1] = nX;
            }
        }
        nAutoRangeCount = nAutoRangeCount - 1;  //Decrease the autorange counter
    }

    public cProtElem oGetElem() {
        return oProtElem;
    }

    public float mGetValue() {                      //Get current datavalue
        return oProtElem.getVal(nCurrDataIndex);
    }

    public void mSetValue(float mRetVal) {          //Set current datavalue
        oGetElem().setVal(nCurrDataIndex, mRetVal);
    }

    public void setElementByName(String sVarName) {
        this.oProtElem = getByName(sVarName);
    }

    private cProtElem getByName(String sVarName) {
        sVarName = sVarName.replaceAll("\\s", "");
        cProtElem ret = mProtocol().mGetElemByName(sVarName);
        if (ret == null) mErrMsg("Not found in getByName:" + sVarName);
        return ret;
    }

    public void mRawValue(int i) {      //Set the raw value of the currently active index
        oProtElem.mDataWrite(nCurrDataIndex,i);  //Request a write to the device
    }

    public int mRawValue() {            //Get the raw value
        if (oProtElem == null) return -1;
        oProtElem.mDeviceReadRequest();
        return oProtElem.mDataPeek(nCurrDataIndex);
    }

    public cProtocol3 mProtocol() {     //Return the current protocol or the first
        int myProtocolNr = 0;
        return oProtocol[myProtocolNr];
    }

    public Context mParent() {
        if (oOwner != null)
            return oOwner.getContext();
        return (Context) oFocusdActivity;
    }

    public String mGetName() {
        if (oProtElem == null)
            return "";
        return oProtElem.sElemName();
    }
//  ****************************        PROPERTIES

    public void bVisible(boolean checked) {
        if (checked)
            nProperties = cFunk.bitset(nProperties, ePropIdx.kVisible.ordinal());
        else
            nProperties = cFunk.bitclear(nProperties, ePropIdx.kVisible.ordinal());
    }

    public boolean bVisible() {
        if (bDesignMode()) return true;
        if (0 < cFunk.bitstate(nProperties, ePropIdx.kVisible.ordinal())) return true;
        return false;
    }

    public void bEnabled(boolean checked) {
        //Clear bit bits= bits & ~(1L << n)
        if (checked)
            nProperties = cFunk.bitset(nProperties, ePropIdx.kEditable.ordinal());
        else
            nProperties = cFunk.bitclear(nProperties, ePropIdx.kEditable.ordinal());
    }

    public boolean bEnabled() {
        return 0 < cFunk.bitstate(nProperties, ePropIdx.kEditable.ordinal());
    }

    public void bWriteOnStart(boolean checked) {
        //Clear bit bits= bits & ~(1L << n)
        if (checked)
            nProperties = cFunk.bitset(nProperties, ePropIdx.kWriteOnStart.ordinal());
        else
            nProperties = cFunk.bitclear(nProperties, ePropIdx.kWriteOnStart.ordinal());
    }

    public boolean bWriteOnStart() {
        return 0 < cFunk.bitstate(nProperties, ePropIdx.kWriteOnStart.ordinal());
    }    //


    public void mUpdate(View v) {
        if (bDesignMode())  v.setVisibility(View.VISIBLE);
        else if( bVisible())v.setVisibility(View.VISIBLE);
        else v.setVisibility(View.GONE);
        if (mInt2Bool(mAppProps(cKonst.eAppProps.kShowHidden))) {
            v.setBackgroundResource(R.drawable.backborder);
        }
    }

    public int nColorGet1(int i) {
        if (oGetElem() == null) return 0;
        return oGetElem().mColorIndex(nCurrDataIndex, i);
    }

    public void nColorSet(int nBackground, int c) {  //Compresss 32bit color to 8bit
        oGetElem().mColorIndexSet(nCurrDataIndex, nBackground, (byte) c);
    }

    //Getters and setters of alias and description for the element @ index
    public String mAlias() {
        if (oGetElem() == null)
            return myId1;
        return oGetElem().mAlias(nCurrDataIndex);
    }

    public void mAlias(String s) {
        oGetElem().mAlias(nCurrDataIndex, s);
    }

    public String mDescr() {
        return oGetElem().mDescr(nCurrDataIndex);
    }
    public void mDescr(String s) {
        oGetElem().mDescr(nCurrDataIndex, s);
    }

    public int mForeColor() {
        return oGetElem().mForeColor(nCurrDataIndex);
    }
    public int mBackColor() {
        return oGetElem().mBackColor(nCurrDataIndex);
    }

    public String mGetValueText() {
        return oProtElem.mGetValueText(nCurrDataIndex);
    }

    public void mWxValue2Device(float dvval, float nWxRange) {     //Convert a widget to a device value
        if (oProtElem == null) {
            mErrMsg("170912B error");
            return;
        }
        //Normalize value and multiply by range
        oProtElem.setVal(nCurrDataIndex, (nRange * dvval) / nWxRange);
    }
    public  int unit2dispVal(float unitval, float nViewScale) {
        //Convert data units to display values,
        float[] nDisplayRange = oProtElem.nDisplayRange;
        unitval = unitval - nDisplayRange[0];   //Subtract offset since slider ranges from 0 to nDispMax
        unitval = unitval / (nDisplayRange[1] - nDisplayRange[0]);   //Scale to display range
        int i = (int) (unitval * nViewScale);      //Expand to slider range
        return i;                 //Add display offset
    }
    public float mDevice2WxValue(float nWxRange) {      //Get device value and convert it to widget value
        if (oProtElem == null) {
            mErrMsg("170912 error");
        }
        float unitval = oProtElem.getVal(nCurrDataIndex);    //value in units
        unitval = unitval - oProtElem.nDisplayRange[0];   //Subtract offset since slider ranges from 0 to mySliderMax
        if (nRange > 0) {
            unitval = unitval / nRange;   //Scale to display range
        }
        float nPct = (unitval * nWxRange);      //Expand to slider range
        return nPct;
    }

    public float mDispRange() {                           //Return the display range 170914
        if (oGetElem() == null) return 1000;            //Default range
        float v = (oGetElem().nDisplayRange[1] - oGetElem().nDisplayRange[0]);
        return v;
    }
}

