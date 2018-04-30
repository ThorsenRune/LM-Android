//171222
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
import static it.fdg.lm.cFunk.mArrayRedim;
import static it.fdg.lm.cFunk.mInt2str;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cFunk.mStr2Int;
import static it.fdg.lm.cProgram3.bDesignMode;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.oFocusdActivity;
import static it.fdg.lm.cProgram3.oaProtocols;
import static java.lang.Math.round;


public  class cElemViewProps {
    private cProtElem _oElement1;            //Pointer to an element, identified by _ProtName, _ElemName
    public String myId1;
    public View myView = null;               //The owner view
    private int nAutoRangeCount = 0;
    private int nProperties;                    //Properties of the view, enabled, ...
    public int nTypeId =0;                         //Shape of the view
    private String _ProtName="Null", _ElemName="None";        //identifiers for the element
    private int _ElemDataIdx = 0;           //If scalar this is the cProtData.aData[index] to show

    public cElemViewProps(String sId) {
        myId1=sId;
    }

    public void mZoom(float scalePointX, float scalePointY, float nFactor) {    //171003 implementing zoom
        float na = myProtElem().nDisplayRange[0];
        float nb = myProtElem().nDisplayRange[1];
        float mid = (na + nb) / 2;
        float newrange = (nb - na)/ nFactor;
        float newA = mid - newrange / 2;
        float newB = mid + newrange / 2;
        if (newB>20)
            newB=round(newB/5)*5;
        if (myProtElem().nDisplayRange[0]!=0) myProtElem().nDisplayRange[0]=newA;       //Don't change a zero offset
        myProtElem().nDisplayRange[1]=newB;
        bDoRedraw=true;
    }

    public String mScaleMaxStr() {
        if (myProtElem()==null) return "Null";
        float v = myProtElem().nDisplayRange[1];
        return mVal2Str(v);
    }
    public String mScaleMinStr() {
        if (myProtElem()==null) return "Null";
        return mVal2Str(myProtElem().nDisplayRange[0]);
    }
    public String mVal2Str(double v) {
        String u = myProtElem().getUnits();
        String s = String.format("%.2f ", v) + u;
        return s;
    }
    private cProtElem myProtElem() {
        if (_oElement1==null)
            mSetElement(_ProtName, _ElemName);
        return _oElement1;

    }
    public void mRefresh(boolean doRedraw) {
        if (doRedraw)       //Refresh element pointer
            mSetElement(_ProtName, _ElemName);
        if (doRedraw&&(myProtElem()!=null)){
            myProtElem().mLinkRefresh();
        }
        if (myView instanceof cData_View)
            ((cData_View) myView).mRefresh(bDoRedraw);

    }
    public String mGetProtName() {
        return _ProtName;
    }
    public String mGetElemName() {
        return _ElemName;
    }
    public void mSetElemIdx(int i) {
        i=mLimit(0, i,_oElement1.nDataLength() - 1);
        _ElemDataIdx=i;
    }
    public int mGetElemIdx(){
        return _ElemDataIdx;
    }

    public String sTypeList() {
        if (myView instanceof cSliderView)
            return cSliderHandle.sTypeList;
        else
            return "";
    }


    private enum ePropIdx {
        kVisible, kEditable, kWriteOnStart,kLimit2Siblings, kZoomAble;
    }

    //*****************+            IMPLEMENTATION  ***********************

    public void mViewSettings3(boolean bGetIt, String sWidgetId) { //Get/Set settings of this view with sId
        if (sWidgetId.length() < 1)
            return;          //Skip empty widgets
        myId1 = sWidgetId;
        _mElemBinding(bGetIt, sWidgetId + ".Name");
    }

    private void _mElemBinding(boolean bGetIt, String sKey) {
        //In the config file in format example    WV0A0.Name	=>	[P1,IMin, 0, 3];
        String[] s={"ProtIdx","VarName","DataIndex","Props","shape"};
        if (bGetIt==false) {
            if (myProtElem()==null) return;
            s[0] = _ProtName;
            s[1] = sElemName();
            s[2] = mInt2str(_ElemDataIdx);
            s[3] = mInt2str(nProperties);
            s[4] = mInt2str(nTypeId);

        }
        s = mPrefs5(bGetIt, sKey, s);
        s = mArrayRedim(s, 4);       //Make sure that it's there
        mSetElement(s[0],s[1]);
        _ElemDataIdx = mStr2Int(s[2]);
        nProperties = mStr2Int(s[3]);     //Visibility of the control
        nTypeId = mStr2Int(s[4]);
    }

    void mSetElement(String protName, String elemName) {
        _ProtName=protName;
        _ElemName=elemName;
        _oElement1 = cProgram3.mElementByName(_ProtName, _ElemName);
    }

    private String sElemName() {            //Safely get element name assigned to the control
        if (myProtElem() == null)
            return "";
        return _ElemName;
    }
    public void mAutoRange(boolean doit) {     //https://docs.google.com/document/d/11nCREUKJt2hxSaZ5-icjiOPtkRpfNy2ZHuoLSSgvyMk/edit#heading=h.kbe9499vxmh1
        //Swap hig and low
        float nX = myProtElem().nDisplayRange[0];
        myProtElem().nDisplayRange[0] = myProtElem().nDisplayRange[1];
        myProtElem().nDisplayRange[1] = nX;
        nAutoRangeCount = 20;  //Number of samples to autorange over
        mAutoRange();
    }

    private void mAutoRange() { //Autoscalin performed by calling mGetValue and having doAutoScale=true
        if (nAutoRangeCount < 1) return;
        for (int i = 0; i < myProtElem().nDataLength(); i++) {
            float nX = myProtElem().getVal(i);
            if (myProtElem().nDisplayRange[0] > nX) {
                myProtElem().nDisplayRange[0] = nX;
            }
            if (myProtElem().nDisplayRange[1] < nX) {
                myProtElem().nDisplayRange[1] = nX;
            }
        }
        nAutoRangeCount = nAutoRangeCount - 1;  //Decrease the autorange counter
    }

    public cProtElem myProtElem1() {
        return myProtElem();
    }

    public float mGetValue() {                      //Get current datavalue
        return myProtElem().getVal(_ElemDataIdx);
    }

    public void mSetValue(float mRetVal) {          //Set current datavalue
        myProtElem().setVal(_ElemDataIdx, mRetVal);
    }


    private cProtElem getByName(int nProtNr,String sVarName) {
        cProtElem ret=null;
        if (nProtNr>=oaProtocols.length) return  oaProtocols[0].oaProtElem[0];
        sVarName = sVarName.replaceAll("\\s", "");
        ret = oaProtocols[nProtNr].mGetElemByName(sVarName);
        if (ret != null)
            return ret;
        if (ret == null)
            mErrMsg("Not found in protocol:"+ nProtNr+" - "+ sVarName);
        return ret;
    }

    public void mRawValue(int i) {      //Set the raw value of the currently active index
        myProtElem().mDataWrite(_ElemDataIdx,i);  //Request a write to the device
    }

    public int mRawValue() {            //Get the raw value
        if (myProtElem() == null) return -1;
        return myProtElem().mDataRead(_ElemDataIdx);
    }



    public Context mContext() {
        if (myView != null)
            return myView.getContext();
        return (Context) oFocusdActivity;
    }

    public String mGetName() {
       return _ElemName;
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
    public boolean bLimit2Siblings() {
        return 0 < cFunk.bitstate(nProperties, ePropIdx.kLimit2Siblings.ordinal());
    }
    public void bLimit2Siblings(boolean checked) {
        nProperties=cFunk.nBitMask(nProperties, ePropIdx.kLimit2Siblings.ordinal(),checked);
    }
    public boolean bZoomAble() {
        return 0 < cFunk.bitstate(nProperties, ePropIdx.kZoomAble.ordinal());
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
        if (bDesignMode()) {
            v.setBackgroundResource(R.drawable.backborder);
        }
    }

    public int nColorGet1(int i) {
        if (myProtElem1() == null) return 0;
        return myProtElem1().mColorIndex(_ElemDataIdx, i);
    }

    public void nColorSet(int nBackground, int c) {  //Compresss 32bit color to 8bit
        myProtElem1().mColorIndexSet(_ElemDataIdx, nBackground, (byte) c);
    }

    //Getters and setters of alias and description for the element @ index
    public String mAlias() {
        if (myProtElem1() == null)            return myId1;
        return myProtElem1().mAlias(_ElemDataIdx);
    }

    public void mAlias(String s) {
        myProtElem1().mAlias(_ElemDataIdx, s);
    }

    public String mDescr() {
        return myProtElem1().mDescr(_ElemDataIdx);
    }
    public void mDescr(String s) {
        myProtElem1().mDescr(_ElemDataIdx, s);
    }

    public int mForeColor() {
        return myProtElem1().mForeColor(_ElemDataIdx);
    }
    public int mBackColor() {
        return myProtElem1().mBackColor(_ElemDataIdx);
    }

    public String mGetValueText() {
        return myProtElem().mGetValueText(_ElemDataIdx);
    }

    public void mWxValue2Device(float dvval, float nWxRange) {     //Convert a widget to a device value
        if (myProtElem() == null) {
            mErrMsg("170912B error");
            return;
        }
        //Normalize value and multiply by range
        myProtElem().setVal(_ElemDataIdx, (nRange() * dvval) / nWxRange);
    }
    public  int unit2dispVal(float unitval, float nViewScale) {
        //Convert data units to display values,
        float[] nDisplayRange = myProtElem().nDisplayRange;
        unitval = unitval - nDisplayRange[0];   //Subtract offset since slider ranges from 0 to nDispMax
        unitval = unitval / (nDisplayRange[1] - nDisplayRange[0]);   //Scale to display range
        int i = (int) (unitval * nViewScale);      //Expand to slider range
        return i;                 //Add display offset
    }
    public float mDevice2WxValue(float nWxRange) {      //Get device value and convert it to widget value
        if (myProtElem() == null) {
            mErrMsg("170912 error");
        }
        float unitval = myProtElem().getVal(_ElemDataIdx);    //value in units
        unitval = unitval - myProtElem().nDisplayRange[0];   //Subtract offset since slider ranges from 0 to mySliderMax
        unitval = unitval / nRange();   //Scale to display range
        float nPct = (unitval * nWxRange);      //Expand to slider range
        return nPct;
    }

    private float nRange() {            //Return a non null  max-min range of the display
        if (_oElement1==null){
            _oElement1=myProtElem();
            return 1;
        }
        float nRange = (_oElement1.nDisplayRange[1] - _oElement1.nDisplayRange[0]);
        if (nRange==0) nRange=1;
        return  nRange;
    }

    public float mDispRange() {                           //Return the display range 170914
        return nRange();
    }
}

