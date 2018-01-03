//170919
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1CndBLzYtQ-NSb_twIb-Vuzcy_cPuDaNiYKxzpLclKs8/edit
//170823    made     Visibility methods
package it.fdg.lm;
/*
*       Element of data of the protocol
*/

import static it.fdg.lm.cFileSystem.mPrefs5;
import static it.fdg.lm.cFunk.mArrayRedim;
import static it.fdg.lm.cKonst.sKeyFieldSep;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mPalIdx2Col;

public class cProtElem {
    private cProtocol3 oProtocol;         //The parent of this protocol element
    String sVarName="";
    private String[] sLinkVarName;   //={"ProtNr","Varname","Index"};//171129
    int nVarNameLen=-1;
    int myVarId =-1;
    int nVarType=-1;
    private int[] aData={-1};               //Holding data from device
    private byte nGetReqCntr=0;            //8 bit signed counter for the number of times we read  a oProtData
    private int nSetReqCntr=0;            //8 bit signed counter for the number of times we write a oProtData
    int nDataTimeStamp=0;
//-161216 obsolete      private  int nVarCount;	//The size of the protocol i.e. the number of variables exposed  nVarCount

    //Additions 170529  android version LM2
    private String[] sAlias={""};						//Plain alias of the variable name
    private String[] sDescr={""};						//Plain description of the variable name
    public String sUnit="[]";                   //name of the unit (eg. mV, mA etc)
    public int nOffset=0;                      //Offset to subtract from int before converting to units u=(int-offs)/nFactor
    public float nFactor=1;                    //Conversion factor from int to units (e.g. x[mA]=(int-offset)/Factor
    public float[] nDisplayRange = {0, 50};    //Displayrange of the variable
    private int[] nProperties= {0,0,0,0,0,0,0,0};            //Properties 170904
    private String[] sBitNames={""};
    private static String sMyKey="";
    int [][] nColors=new int[2][4];  //Colors are kColorDim X  dataarraysize matrix
    private int[] nBitVisible=new int[32];
    private int myIndex=0;
    private cProtElem oLinkDest;    //Relay data to anoter ProtElement
    public int mBackColor(int myProtDataIdx) {
        return mPalIdx2Col(mColorIndex(myProtDataIdx, 1));
    }
    public int mForeColor(int myProtDataIdx) {
        int n = mColorIndex(myProtDataIdx, 0);
        return mPalIdx2Col(n);
    }

    public void mDeviceReadRequest() {      //Queues a request to read from device, use this to refresh data
        if (nSetReqCntr<1)          //Only read if youre not writing
            nGetReqCntr=1;                   //Request a update of the element from device if not writing
    }

    //  End 170529


    public cProtElem(cProtocol3 parentProtocol) {
        oProtocol =parentProtocol;
        mInit2();
    }

    public void mInit2() {            //Initialize protocol element
        nDataLength(1);
    }
    public void mSettings(boolean bGet) {        //mFileSave the
        if (sVarName.length()<1){
            mErrMsg("No value for element");
            return;
        }
        sMyKey = sProtName()+sKeyFieldSep + sVarName;
        mSettings1(bGet,sMyKey);
    }

    private String myDevName() {
        return  oProtocol.mDeviceNameGet();
    }

    public void mSettings1(boolean bGet,String sMyKey){        //mFileSave the
        sUnit= mPrefs5(bGet,sMyKey+".Unit",sUnit);
        nOffset= mPrefs5(bGet,sMyKey+".Offset",nOffset);
        nFactor = mPrefs5(bGet,sMyKey+".Factor", nFactor);
        nDisplayRange = mPrefs5(bGet, sMyKey + ".DispRange", nDisplayRange);
        sDescr= mPrefs5(bGet,sMyKey+".Descr",sDescr);
        sAlias= mPrefs5(bGet,sMyKey+".Alias",sAlias);
        nColors[0] = mPrefs5(bGet,sMyKey+".Color1", nColors[0]);    //Fore back color indexes
        nColors[1] = mPrefs5(bGet,sMyKey+".Color2", nColors[1]);    //Fore back color indexes
        nProperties = mPrefs5(bGet,sMyKey+".Properties",nProperties);
        sLinkVarName = mPrefs5(bGet,sMyKey+".RelayTo",sLinkVarName );   //R171130
        aData= mPrefs5(bGet,sMyKey+".Data",aData);
        if (mIsBitField()||(bGet)) {        //Do only save real bitfields
            sBitNames = mPrefs5(bGet, sMyKey + ".BitNames", sBitNames);
            nBitVisible = mPrefs5(bGet, sMyKey + ".BitDisp", nBitVisible);
        }
    }
    public void  mLinkRefresh() {       //Is this variable linked to another?
        if (sLinkVarName==null)
            oLinkDest=null;
        else if (sLinkVarName.length>1) {       //Initialize pointer
            oLinkDest = cProgram3.mElementByName(sLinkVarName[0], sLinkVarName[1]);
        }
    }
    public float getVal(int idx) {      //Return value as transformed raw data as units e.g. mA,mV etc
        if (idx<aData.length)
             return (float)(mDataRead(idx)-nOffset)/ nFactor;            //Convert integer to units
        //Catch errors
        mErrMsg("Index fault in "+ sProtName()+":"+sVarName);
        nDataLength(idx);
        return 0;
    }

    public void setVal(int idx, float val) {        //Set a value in the relative units
        if (aData.length<=idx) mErrMsg("Index fault");
          //Clip value to display range
        if (nDisplayRange[0]<nDisplayRange[1]) {     //Range control enabled
            if (val < nDisplayRange[0]) {
                val = nDisplayRange[0];
                mErrMsg("Clipping value to " + val);
            } else if (val > nDisplayRange[1]) {
                val = nDisplayRange[1];
                mErrMsg("Clipping value to " + val);
            }
        }
        mDataWrite(idx,(int) (val* nFactor+0.5)+nOffset);
//        aData[idx]=(int) (val* nFactor+0.5)+nOffset;    //Convert units to integer, rounding to nearest integer
    }

    //****************************EXCHANGE DATA WITH DEVICE *****************************
    public int mDataRead(int idx){      //Read data from device
        if (aData.length<=idx) mErrMsg("Index fault");
        mDeviceReadRequest();
        if (oLinkDest!=null)            //If this variable is linked to another
        {   //            mRelayTo(oLinkDest, aData[idx])
            oLinkDest.mDataWrite(idx,aData[idx]);
        }
        return aData[idx];
    }
    public void mData(int idx, int value) {      //Does not alter request state, just change the curren
        if (0<nSetReqCntr)      //Don't change values while writing to device
            return;
        if (idx<aData.length)
            aData[idx]=value;
        else
            mErrMsg("Index out of bounds");
    }
    public void mDataWrite(int idx, int value) {
        aData[idx]=value;
        nGetReqCntr=-2;     //Stop reading to be sure a write has been done
        nSetReqCntr=5;
    }

    //*******************************************************************************

    public String getUnits() {          //Get the name of the units for the variable
        return sUnit;
    }
    public void setScaling(int nOffset0, float nGain0, String sUnits0) {
        this.nOffset=nOffset0;
        this.nFactor =nGain0;
        sUnit= sUnits0;
    }


//Getters and setters

    public  int nDataLength(){
        return aData.length;
//        return nDataArraySize;
    }
    public void nDataLength(int nNewLength){
        if (nDataLength()!=nNewLength)
            aData=new int[nNewLength];
    }

    public String mGetValueText(int indexOfDataArray) {
        if (myVarId<0)
            return sVarName;
        if (mIsInt())       //Format as integer
            return  getVal(indexOfDataArray)+" "+getUnits();
        return String.format("%.1f ", getVal(indexOfDataArray))+getUnits();
    }

    public String sElemName() {
        return sVarName;
    }



    private boolean mIsBitField() {
        if (sBitNames==null)
            return  false;
        else if (sBitNames.length<2)
            return (sBitNames[0]!="");
        else return true;
    }
    public void mBitName(int i, String s) {    //setter for bitname  170727
        if (sBitNames==null)    sBitNames=new String[32];;
        if (sBitNames.length<=i) sBitNames=mArrayRedim(sBitNames,i);  //Redimension
        sBitNames[i]=s;
    }
    public String mBitName(int i){      //170727    return the description of the bit
        if (sBitNames.length<=i)
            return "Error 170904";
        if (sBitNames[i]==null)
            return "Error 170904B";
        if (sBitNames[i]=="null")
            return "Error 170904C";
        return sBitNames[i];
    }


    public int mColorIndex(int myProtDataIdx, int nForeBackLayer) {
        if (myProtDataIdx>=nColors[nForeBackLayer].length) nColors[nForeBackLayer]=mArrayRedim(nColors[nForeBackLayer],myProtDataIdx);
        int n = nColors[nForeBackLayer][myProtDataIdx];
        return n;
    }

    public void mColorIndexSet(int nDataIndex, int nForeBackLayer, int nColIdx) {
        if (nDataIndex>=nColors[nForeBackLayer].length) nColors[nForeBackLayer]=mArrayRedim(nColors[nForeBackLayer],nDataIndex);
        nColors[nForeBackLayer][nDataIndex]=nColIdx;
    }
    public String mAlias(int nArrayIdx) {
        return _mGetStr(sAlias,nArrayIdx);
    }

    private String _mGetStr(String[] sStr, int nArrayIdx) {
        String sd;
        if (nArrayIdx<sStr.length)
            sd=sStr[nArrayIdx];
        else
            sd = sStr[0];      //Default to first description
        if (sd=="") sd=sVarName;    //or variable name
        return sd;
    }

    public void mAlias(int nArrayIdx,String sNewText) {
        if (sAlias.length<=nArrayIdx) sAlias= mArrayRedim(sAlias,nArrayIdx);
        sAlias[nArrayIdx]=sNewText;
    }

    public String mDescr(int nArrayIdx) {
        return  _mGetStr(sDescr,nArrayIdx);
    }
    public void mDescr(int nArrayIdx,String sNewText) {
        if (sDescr.length<=nArrayIdx) sDescr= mArrayRedim(sDescr,nArrayIdx);
        sDescr[nArrayIdx]=sNewText;
    }

    //Visibility methods
    public int mBitVisible(int i) {
        if (nBitVisible==null)
            nBitVisible= new int[32];
        if (nBitVisible.length<=i)
            nBitVisible=mArrayRedim(nBitVisible,i);
        return  nBitVisible[i];
    }
    public void nBitVisible(int i, int nVisibility) {
        nBitVisible[i]=nVisibility;
    }

    public boolean mIsInt() {
        return nFactor==1;
    }

    public void mCopyElement(cProtElem oNewElement) {       //Clone the new element
        sVarName=oNewElement.sVarName;
        nVarId(oNewElement.nVarId());

        nDataLength(oNewElement.nDataLength());       //Redundant with setting data
        nVarType =oNewElement.nVarType;
    }

    public void nVarId(int nId) {
        if ((nId<64)||(nId>100))
            mErrMsg("Var id out of range");
        myVarId=nId;
    }

    public int nVarId() {
        return myVarId;
    }


    //***********************               FLAGS FOR WRITE/READ TO/FROM DEVICE
    public boolean mIsSetRequest() {
        return nSetReqCntr>0;
    }

    public void mSetRequest(int i) {
        nSetReqCntr=nSetReqCntr+i;
    }

    public void mGetReqDone() {
        nGetReqCntr=0;
    }

    public boolean mHasGetRequest() {
        return 0<nGetReqCntr;
    }

    public int nProtIndexGet() {
        return oProtocol.nIndex();
    }


    public String sProtName() {
        if (oProtocol ==null) return "";
        return oProtocol.sProtName();
    }

    public cProtocol3 myProtocol() {
        return oProtocol;
    }
}
