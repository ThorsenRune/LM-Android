//              USER INPUT using the build in Dialog

//170915        Added method of getting focused control
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1iuD2xAL0aTZgCWTZS4Z6E5y8QVqJ1_82keiaz6wcHO0/edit
package it.fdg.lm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import static it.fdg.lm.cAndMeth.mMakeSpinnerAdapter;
import static it.fdg.lm.cAndMeth.mTextViewSetTextSize;
import static it.fdg.lm.cDebug.nTestCount;
import static it.fdg.lm.cFunk.mArrayFind;
import static it.fdg.lm.cFunk.mIndexStringArry;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cFunk.mStr2Float;
import static it.fdg.lm.cFunk.mStr2Int;
import static it.fdg.lm.cFunk.mStr2StrArr;
import static it.fdg.lm.cFunk.mStrArr2Str;
import static it.fdg.lm.cKonst.eProtState.kProtInitDone;
import static it.fdg.lm.cKonst.eProtState.kProtInitInProgress;
import static it.fdg.lm.cKonst.eProtState.kProtResetReq1;
import static it.fdg.lm.cKonst.eProtState.kProtUndef1;
import static it.fdg.lm.cKonst.eSerial.kBT_BrokenConnection;
import static it.fdg.lm.cKonst.eSerial.kBT_ConnectReq1;
import static it.fdg.lm.cKonst.eSerial.kBT_Connected1;
import static it.fdg.lm.cKonst.eSerial.kBT_DevicePickerActive;
import static it.fdg.lm.cKonst.eSerial.kBT_InvalidDevice1;
import static it.fdg.lm.cKonst.eSerial.kBT_TimeOut;
import static it.fdg.lm.cKonst.eSerial.kDeviceWasSelected;
import static it.fdg.lm.cKonst.nAppProps;
import static it.fdg.lm.cProgram3.bAutoConnect;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mPersistAllData;
import static it.fdg.lm.cProgram3.nCurrentProtocol;
import static it.fdg.lm.cProgram3.oFocusdActivity;
import static it.fdg.lm.cProgram3.oProtocol;
import static it.fdg.lm.cProgram3.sDevices1;
import static it.fdg.lm.cProgram3.sErrMsg;
import static it.fdg.lm.fMain.cmdText;


/**
 * Method for getting user input the inputbox way.
 * The pattern:     call mEditxxx with arguments of the control etc. the function returns immediately
 * When the user accepts the mEditAccept will be called -> a second call to the mEditxxx which can now return the values
 * mInputBox(obj){datavalues -> textboxes, set oEditType to select the callback }->return
 * mInputAccept(){use oEditType to select the call:}->mInputBox(obj) {textboxes -> data} -> return
 */

public class cUInput {
    private static cElemViewProps oElemViewProps;  //Visual properties of focused control
    public static Context mContext;
    private static AlertDialog.Builder alert;
    private static AlertDialog mAlertDialog;
    private static eEditType oEditType;
        //      Controls for the alert myLayout
    static  Spinner[] inCB =new Spinner[6];
    static TextView[] lblLbl =new TextView[6];
    static View[] inpView =new View[6];

    static EditText[] txtEdit =new EditText[6];
    static CheckBox[] cbCheck =new CheckBox[6];

    private static LinearLayout myLayout;
    private static Button cmdCmd;
    private static String[] lstViewStr = {
            "Hidden",
            "Visible",   //
            "Placeholder" };
    private static int nEditIdx;
    private static boolean isDirty;
    private static boolean bHideKeyboard=true;      //Hide softkeyboard on opening a dialog, waiting for user to click
    private static boolean bReturnKeyPressed;
    public static View oFocusedView;
    private static int nButtonPressCount;
    //private static cBitField oBitField;         //When editing cBitField

    public static boolean mSelected() {         //Returns true if an element is selected
        return (oElemViewProps!=null);
    }

    public static String oCtrlID() {
        // id of focused control
        if (oElemViewProps==null) return "";
        String s = oElemViewProps.myId1;
        if (mGetElemObj()!=null)
            s=s+" - " +oElemViewProps.oGetElem().sElemName();
        return  s;
    }

    public static String sValue() {
        return (""+oElemViewProps.mGetValue());
    }

    public static cElemViewProps mGetViewProps() {
        return  oElemViewProps;
    }

    public static void mInputSelectDevice(boolean bAsk) {
        if (bAsk) {
            oEditType = eEditType.kInputSelectDevice;
            mLinLayPrep2();
            mLayoutAddDropDown1(1,"Device",sDevices1,nCurrentProtocol);
//            mLayoutAddCheckbox(2, "Relay", nCurrentProtocol==nRelayProtocol);
            mLayoutAddCmd(2,"Other devices").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    oProtocol[nCurrentProtocol].mBT_PickDevice1();
                    mAlertDialog.dismiss();
                }
            });
            mDialogSetup2("Select device", oEditType);             //Prepare dialog layout
        }else {       // Callback Get the fields from the editbox
            String devName = mGetText(inCB[1]);
            mProtocol().mConnectNamedDevice(devName);
            oEditType = eEditType.kNull;
            return;
        }
    }

    public static void mElemViewProps(cElemViewProps oElem) {       //170912        Sets the current view element
        oElemViewProps=oElem;
    }

    public static void mRefresh(boolean doRedraw) {         //Refresh/draw the ui interface
        //Command selector
        if (doRedraw) {
            oElemViewProps=mGetActiveControl(oFocusedView);     //170915    Return active control of active view
            mCommand(false);            //Redraw the command button
        }
    }

    public static ViewGroup mFocusedPanel() {
        if (oFocusedView==null) return null;
        return (ViewGroup) oFocusedView.getParent();

    }


    public enum eEditType {
        kNull,
        kValue,

        kSelProtDataElem,       //Select data element associated with the control
        kProtDataCalib, kInputRange,
        kConnect, kViewSettingsEdit, kBitNames, kUserLevel, kInputSelectDevice, kEditDescription
    }

    // --------------                   ************    INTERFACE  *******************
    private static String mGetText(Spinner inCB) {
        if (inCB==null) {mErrMsg("null pointer");return "";}
        if (inCB.getSelectedItem()==null) {mErrMsg("null pointer 170623");return "";}
        return inCB.getSelectedItem().toString();
    }
    private static String mGetText(EditText txtEdit) {         //Get the return value for an input
        return txtEdit.getText().toString();
    }

    private static void mRedrawParent() {
        if (mContext instanceof cBitField) {
            //((cBitField) mContext).mRedrawMenu();
        } else if (mContext instanceof fMain){
            cProgram3.mRedraw();
        } else mErrMsg("Not implemented");
    }

    private static cProtocol3 mProtocol() {     //Return the current protocol
        if (oElemViewProps==null)
            return  oProtocol[nCurrentProtocol];
        return oElemViewProps.mProtocol();
    }

                    //......................        INPUT A DISPLAY RANGE   ................
    public static void mInputRange(boolean bAsk) {     //if called with true it set up the dialog
        if (oElemViewProps==null) return;
        if (bAsk){            //First call set the inputboxes
            mLinLayPrep2();     //Prepare mInputRange
            mLayoutAddTextEdit(0,"Minimum visible value", mGetElemObj().nDisplayRange[0]);
            mLayoutAddTextEdit(1,"Maximum visible value", mGetElemObj().nDisplayRange[1]);
            mLayoutAddCmd(3,"Do autoscaling").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    oElemViewProps.mAutoRange(true);
                    mEditAccept(false);
                }
            });
            mDialogSetup1(oElemViewProps.mAlias());          //Set dialog boxes
            oEditType = eEditType.kInputRange;                                   //Start editing data
        }
        else if ((oEditType == eEditType.kInputRange)){      //Accept data
            mGetElemObj().nDisplayRange[0] =mStr2Float(nTextEditGet(0), mGetElemObj().nDisplayRange[0]);
            mGetElemObj().nDisplayRange[1]=mStr2Float(nTextEditGet(1),  mGetElemObj().nDisplayRange[1]);
//!-            oElemViewProps.mSetSlider(mStr2Float(nTextEditGet(2)));
            mPersistAllData(false);
            oEditType = eEditType.kNull;
            return;
        }

    }

    public static void mEditBitDesc(boolean bAsk, int index) {
        if (oElemViewProps==null)
            return;
        if (bAsk) {
            oEditType = eEditType.kBitNames;
            mLinLayPrep2();
            nEditIdx=index;
            if (oElemViewProps==null)
                return;
            String sTitle=oElemViewProps.mGetName();
            String s = mGetElemObj().mBitName(nEditIdx);
            mLayoutAddTextEdit(0,"Bit "+nEditIdx+" description",s);
            index= mGetElemObj().mBitVisible(nEditIdx);
            mLayoutAddDropDown1(1,"Visibility of control",lstViewStr,lstViewStr[index]);
            mDialogSetup1(sTitle);             //Prepare dialog layout

        }else {			//	Callback Get the fields from the editbox
            String s = mGetText(txtEdit[0]);
            mGetElemObj().mBitName(nEditIdx,s);
            index= mLayout_DropDown_SelIdx(1);
            mGetElemObj().nBitVisible(nEditIdx,index);
            oEditType = eEditType.kNull;
            mPersistAllData(false);
            return;
        }

    }

    private static int mLayout_DropDown_SelIdx(int i) {
        if (inCB[i]==null) return -1;
        return inCB[i].getSelectedItemPosition();
    }



    public static void mInputViewSettings1(boolean bAsk) {     //if called with true it set up the dialog
        if (oElemViewProps==null) return;
        if (bAsk) {
            oEditType = eEditType.kViewSettingsEdit;
            mLinLayPrep2();         //prepare mInputViewSettings1
            String s="empty";
            mLayoutAddDropDown1(0,"Element   ",mElemNameList(),mElemName());
            if (mGetElemObj()!=null) {
                mLayoutAddDropDown1(1, "Index     ", mIndexStringArry(mGetElemObj()),mFocusedIndex());
                mLayoutAddTextEdit(0, "Minimum visible value", mGetElemObj().nDisplayRange[0]);
                mLayoutAddTextEdit(1, "Maximum visible value", mGetElemObj().nDisplayRange[1]);
                mLayoutAddCheckbox(3, "Visible for userinput", mFocusedViewProps().bVisible());
                mLayoutAddCheckbox(2, "Enabled for userinput", mFocusedViewProps().bEnabled());
                mLayoutAddCheckbox(4, "Enabled WriteDeviceOnStart", mFocusedViewProps().bWriteOnStart());
            }
            mLayoutAddCmd(2,"Edit colors").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    mEditAccept(false); mStartColorPicker(oElemViewProps);
                }
            });
            mLayoutAddCmd(3,"Edit description and alias").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    mEditAccept(false);                    mEditDescription(true);
                }
            });
            mLayoutAddCmd(3,"Edit unit and conversion").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    mEditAccept(false);                    mInputProtData(true);
                }
            });
            mButton_Scaling(bAsk);

            if ( oElemViewProps.oGetElem()!=null)s = oElemViewProps.mAlias();
            mDialogSetup1(s);             //Prepare dialog layout, needs to be last

        }else        if ((oEditType == eEditType.kViewSettingsEdit)){			//	Callback
            //Get the fields from the editbox
            String sRetVal =mGetText(inCB[0]);//
            if (mGetElemObj()!=null) {
                int idx = mLayout_DropDown_SelIdx(1);
                oElemViewProps.nCurrDataIndex = mLimit(0, idx, oElemViewProps.oGetElem().nDataLength() - 1);
                mGetElemObj().nDisplayRange[0] = mStr2Float(nTextEditGet(0), mGetElemObj().nDisplayRange[0]);
                mGetElemObj().nDisplayRange[1] = mStr2Float(nTextEditGet(1), mGetElemObj().nDisplayRange[1]);
            }
            if (mGetElemObj()!=null) {
                mFocusedViewProps().bEnabled( cbCheck[2].isChecked());
                mFocusedViewProps().bVisible( cbCheck[3].isChecked());
                mFocusedViewProps().bWriteOnStart(cbCheck[4].isChecked());
                mGetElemObj().mSettings(false);         //mFileSave settings
            }
            oElemViewProps.setElementByName(sRetVal);
            oEditType = eEditType.kNull;
            mPersistAllData(false);
        }
    }

    private static void mButton_Scaling(boolean bAsk) {     //Add a scaling command button
        if (oFocusedView instanceof  cSignalView2) {
            mLayoutAddCmd(4, "Do autoscaling").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    oElemViewProps.mAutoRange(true);
                    mEditAccept(false);
                }
            });
            return;
        }
        if (oFocusedView instanceof  cSliderView) {
            mLayoutAddCmd(5, "Apply Same scaling").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    mApplySameScale();
                }
            });
        }
    }

     private static void mApplySameScale() {        //Apply the same scaling to other handles in the sliderview
        if (oFocusedView instanceof  cSliderView) {
            cSlider o = ((cSliderView) oFocusedView).oParent;
            if (o==null ) return;
            o.mApplySameScale();                mEditAccept(false);
        }
    }

    private static cElemViewProps mFocusedViewProps() {
        return oElemViewProps;
    }
    private static cProtElem mGetElemObj() {
        if (oElemViewProps==null) return null;
        if (oElemViewProps.oGetElem()==null)            return null;
        return oElemViewProps.oGetElem();

    }
    private static int mFocusedIndex() {
        return  oElemViewProps.nCurrDataIndex;
    }

    private static String[] mElemNameList() {
        String[] sList = mStr2StrArr("None," + mStrArr2Str(mProtocol().mGetProtElementList()));
        return sList;
    }

    private static String mElemName() {     //Return name of current element
        if (mGetElemObj()==null)
                return                "Tomt";
        return mGetElemObj().sElemName();
    }

    private static void mStartColorPicker(cElemViewProps oProtElem) {
        Intent intent = new Intent(mContext, colorpick.class);
        mContext.startActivity(intent);
    }

//              EDIT THE CONTROL ELEMENT
    public static void mInputProtData(boolean bAsk) {     //if called with true it set up the dialog  170627B
        if (oElemViewProps==null)            return;
        if (mGetElemObj()==null) return;
        if (bAsk){
            oEditType = eEditType.kProtDataCalib;
            mLinLayPrep2();
            //Now set the fields
            mLayoutAddTextEdit(0, mGetElemObj().sElemName()+" integer offset", mGetElemObj().nOffset);
            mLayoutAddTextEdit(1,"integer conversion factor", mGetElemObj().nFactor);
            mLayoutAddTextEdit(2,"Units of conversion ", mGetElemObj().sUnit);             //input units
            int i = oElemViewProps.mRawValue();
            mLayoutAddTextEdit(3, mGetElemObj().mGetValueText(oElemViewProps.nCurrDataIndex)+" = Int:",i);
            mDialogSetup1(oElemViewProps.mAlias());             //protocol data setup
        }else if ((oEditType == eEditType.kProtDataCalib)){
            //Get the fields from the editbox
            mGetElemObj().nOffset= mStr2Int(nTextEditGet(0));
            mGetElemObj().nFactor=mStr2Float(nTextEditGet(1), mGetElemObj().nFactor);
            mGetElemObj().sUnit=(nTextEditGet(2));
            mGetElemObj().mSettings(false);       //mFileSave settings
            oEditType = eEditType.kNull;
            return;
        }
    }


    public static void mInputValue(boolean bAsk) {        //Input a single value,   R170727
        if (oElemViewProps==null)            return;
        if (mGetElemObj()==null) return;
        if(bAsk){
            cProtElem myProtData= oElemViewProps.oGetElem();
            oEditType = eEditType.kValue;
            mLinLayPrep2();
            if (mGetElemObj().mIsInt())
                mLayoutAddTextEdit(0,"Value",(int)oElemViewProps.mGetValue());
            else
                mLayoutAddTextEdit(0,"Value",oElemViewProps.mGetValue());
            if (cProgram3.nUserLevel()>0) {
                mLayoutAddTextEdit(1, "Raw Data", oElemViewProps.mRawValue());
                txtEdit[1].setEnabled(false);
            }
            bHideKeyboard=false;
            mDialogSetup1(oElemViewProps.mAlias());          //Set dialog boxes
        }
        else if ((oEditType == eEditType.kValue)) {      //Accept data
            float mRetVal = mStr2Float(nTextEditGet(0), oElemViewProps.mGetValue());
            oElemViewProps.mSetValue( mRetVal);
            oEditType = eEditType.kNull;
            return;
        }
    }
    //************   END  INTERFACE  *******************
    public static void mSetColorIndex(int nColorIndex, int nColorLayer) {      //Set first/second color
        cProtElem e = oElemViewProps.oGetElem();
        int idx = oElemViewProps.nCurrDataIndex;
        e.mColorIndexSet(idx,nColorLayer, nColorIndex);
    }

    //........................          HANDLING THE FOCUS OF VIEW CONTROLS................
    public static void setFocus(Object obj) {            //Set the focus on this control. !!! to implement some focus rectangle
        oElemViewProps=mGetActiveControl(obj);
        oFocusedView=(View)obj;
        /*!-    cleaning 170919
        if (obj instanceof cVertSlider2){
            mContext=((cVertSlider2) obj).getContext();
        }  */
        if (obj instanceof cSignalView2){
            mContext=((cSignalView2) obj).getContext();
        } else if (obj instanceof cElemViewProps){
            mContext=oElemViewProps.mParent();
        } else if (obj instanceof cSliderView){
            mContext=((cSliderView)obj).getContext();
        }
        mRefresh(true);
    }

    private static cElemViewProps mGetActiveControl(Object oView) {//170915 Return the focused ViewProps of the given View
        if (oView instanceof cSignalView2){
            return ((cSignalView2)oView).oElemViewProps();

        } else if (oView instanceof cElemViewProps){
            return((cElemViewProps)oView);
        } else if (oView instanceof cSliderView){
            return ((cSliderView)oView).oParent.mGetCurrentElementView();
        }
        return oElemViewProps;          //170915    Dont change if not found
    }

    //*****************************commands for the command button***********************
    public static void mCommand(boolean bExecute  ) {   //redraw command button or execut the command
  	/*	STATEMACHINE
	Order of priorities
		kProtUndef:			Undefined protocol (not connected)
		kBT_ConnectReq:        Waiting for device to connect
		error:				Take some action to resolve error
		active:				edit vars etc
	*/
        if (!(oFocusdActivity instanceof fMain)) return;	//Only on mainscreen
//	Undefined protocol
        if (!mIsStateSerial1(kBT_Connected1)) {               //171012    Not connected
            if (mIsStateSerial1(kBT_ConnectReq1)) {                //Timeout
                cmdText(cKonst.eTexts.txtDevice_Connecting);
                if (bExecute) mProtocol().oSerial.mBT_PickDevice1();
            }else if (mIsStateSerial1(kBT_InvalidDevice1)){                //Invalid device
                cmdText("Select a device - press");
                if (bExecute) mProtocol().oSerial.mBT_PickDevice1();
            }else if (mIsStateSerial1(kBT_TimeOut)){                //Timeout
                cmdText(cKonst.eTexts.txtDevice_TimeOut);
                if (bExecute) mProtocol().mProtResetReq();
            }else if (mIsStateSerial1(kBT_BrokenConnection)){                //Timeout
                cmdText("Lost connection");
                if (bAutoConnect())
                    mTryToConnect();
                if (bExecute)mTryToConnect();
            }else if ( mIsStateSerial1(kBT_DevicePickerActive)) {
                cmdText("Select a device ");
                if (bExecute) mTryToConnect();
            }else if ( mIsStateSerial1(kDeviceWasSelected)) {
                String s=mProtocol().mDeviceNameGet();
                mProtocol().mConnectNamedDevice(s);          //  Will initiate a connection
            } else {
                cmdText(txtConnectionError());
                if (bExecute) mTryToConnect();
            }
        }else if ( mIsStateProtocol(kProtResetReq1)) {
            cmdText("Resetting Protocol "+ nTestCount[0]);

        }else if ( mIsStateProtocol(kProtInitDone)) {
            cmdText("Protocol Ready "+ nTestCount[0]);
        }else if ( mIsStateProtocol(kProtUndef1)) {
            cmdText("Undefined Protocol "+ nTestCount[0]);
            if (bExecute) mTryToConnect();
        }else if (mIsStateProtocol(kProtInitInProgress)) {
            cmdText(cKonst.eTexts.txtDevice_Initializing+" "+  oProtocol[nCurrentProtocol].mGetProtSize() );
            if (bExecute) mTryToConnect();
        }else if (sErrMsg!=null) {                          //Error was encountered tell the user
            cmdText(sErrMsg);
            if (bExecute) mProtocol().mProtResetReq();
        }else if (mGetElemObj()!= null) {
            cmdText(oElemViewProps.mDescr());
            if (bExecute) mInputValue(true);
        }else {
            cmdText("Select control, change value here");
        }
    }

    private static String txtConnectionError() {
        if (mIsStateSerial1(kBT_BrokenConnection)){
            return cKonst.eTexts.txtDevice_LostContact;
        }else if (mIsStateSerial1(kBT_TimeOut)){
            return (cKonst.eTexts.txtDevice_TimeOut);
        } else {
            return cKonst.eTexts.txtDevice_DoConnect;
        }
    }

    protected static boolean mIsStateSerial1(cKonst.eSerial nSerialState) {
        cKonst.eSerial v = oProtocol[nCurrentProtocol].oSerial.mStateGet();
        return nSerialState== v;
    }

    private static void mTryToConnect() {

        mProtocol().mProtResetReq();
    }

    private static void mProtStateSet(cKonst.eProtState nNewState) {
        oProtocol[nCurrentProtocol].mStateSet(nNewState);
    }

    private static boolean mIsStateProtocol(cKonst.eProtState nProtState) {
        if (mIsStateSerial1(kBT_Connected1))
            return nProtState==oProtocol[nCurrentProtocol].getState();
        else {        //DO a check of states
            if (mIsStateSerial1(kBT_BrokenConnection))
                mProtocol().mProtResetReq();
        }
        return nProtState==oProtocol[nCurrentProtocol].getState();
    }


//............................      HANDLING EDITING    ............................











    //  ------------- Element scaling properties



    private static TextView mNewLabel(String sDesc) {
        TextView lbl = new TextView(mContext);
        lbl.setText(sDesc);
        mTextViewSetTextSize(lbl);
        return lbl;
    }

    //170728    More recent drop down list accepting and index as selected item
    private static void mLayoutAddDropDown1(int i, String sDesc, String[] sList, int nSelected) {
        if (nSelected<0) nSelected=0;   //Limit imput range to be safe
        if (nSelected>=sList.length) nSelected=sList.length-1;
        lblLbl[i] = new TextView(mContext);
        lblLbl[i].setText(sDesc);
        inCB[i] =new Spinner(mContext);
        ArrayAdapter<String> adapter1=mMakeSpinnerAdapter(mContext,sList);
        inCB[i].setAdapter(adapter1);
        inCB[i].setSelection(nSelected);
        inCB[i].setEnabled(false);
        inCB[i].setOnItemSelectedListener(new OnItemSelectedListener() {    //These will fire automatically
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (adapterView.isEnabled()) {      //NOw see if user has changed it
                    if (mStr2Int(adapterView.getTag().toString())!=i)
                        isDirty = true;
                        mRedrawInput();  //Accept and Reopen the editor
                }
                else {
                    adapterView.setEnabled(true);
                }
                adapterView.setTag(i);      //Set the last index
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mAddLine2Layout(lblLbl[i],inCB[i]);
    }
    private static void mLayoutAddDropDown1(int i, String sDesc, String[] sList, String sSelected) {
        int idx=mArrayFind(sList,sSelected);
        mLayoutAddDropDown1(i,sDesc,sList,idx);
    }
    private static void mLayoutAddTextEdit(int i, String sDesc, Object Value) {
        lblLbl[i] = mNewLabel(sDesc);
        txtEdit[i] = new EditText(mContext);
        txtEdit[i].setText(""+Value);
        txtEdit[i].setMaxLines(1);
     //   txtEdit[i].setImeOptions(EditorInfo.IME_ACTION_DONE);   // set goto next textEdit on return, but it doesnt woprk on some android versions se 171018
        //alternative could be IME_ACTION_NEXT
        if (Value instanceof Integer) {
            txtEdit[i].setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        }
        else if (Value instanceof Float)
            txtEdit[i].setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
       else {
            txtEdit[i].setRawInputType(InputType.TYPE_CLASS_TEXT);
        }
        mAddLine2Layout((View) lblLbl[i],(View) txtEdit[i]);
        txtEdit[i].setFocusableInTouchMode(true);
 /*InputFilter[] filters = new InputFilter[1];filters[0] = new InputFilter.LengthFilter(8);editText.setFilters(filters);This sets the max characters to 8 in that EditText. Hope all this helps you.*/
//        txtEdit[i].selectAll();              //Select the text when control receives focus
 /*           txtEdit[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        */
        txtEdit[i].setHighlightColor(Color.BLACK);
        if (!bHideKeyboard)
        txtEdit[i].setOnFocusChangeListener(new View.OnFocusChangeListener(){
            public void onFocusChange(View v, boolean hasFocus){
                if (hasFocus)
                       ((EditText)v).selectAll();        //But it's not easy to read then
                if (v.isEnabled()==false){
                    if (bReturnKeyPressed)
                        mEditAccept(false);     //accept and close
                }
            }
        });
        //Handle the users next click on keyboard
        txtEdit[i].setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mEditAccept(false);

                    bReturnKeyPressed=true;
                    return true;
                }
                bReturnKeyPressed=false;
                return false;
            }
        });

    }


    private static CheckBox mLayoutAddCheckbox(int i, String sDesc, boolean b) {
        lblLbl[i] = new TextView(mContext);
        lblLbl[i].setText(sDesc);
        cbCheck[i] =new CheckBox(mContext);
        cbCheck[i].setChecked(b);
        cbCheck[i].setOnClickListener(new View.OnClickListener() {             public void onClick(View v) //R170627C
        {
//            mRedrawInput();
        }});
        mAddLine2Layout(cbCheck[i],lblLbl[i]);
        return cbCheck[i];
    }

    private static void mRedrawInput() {
        eEditType et = oEditType;
        mEditAccept(false);
        oEditType =et;
        mEditAccept(true);          //Reopen with new values when checked
    }

    private static Button mLayoutAddCmd(int i, String sDesc) {
        /* add a command button to the layout see: R170627  for using        */
        cmdCmd = new Button(mContext);
        cmdCmd.setText(sDesc);
        mAddLine2Layout(null,cmdCmd);
        return cmdCmd;
    }

    private static String nTextEditGet(int i) {
        return txtEdit[i].getText().toString();
    }


    private static void mAddLine2Layout(View view1, View view2) {   //170612    Make a horizontal row of two elements to left-right
        LinearLayout horizontalLayout = new LinearLayout(mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(  LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);;
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        if (view1!=null){
            lp.gravity= Gravity.LEFT;
            lp.weight=0.25f;
            view1.setLayoutParams(lp);
            horizontalLayout.addView(view1);
        }
        if (view2!=null) {
            lp.gravity = Gravity.RIGHT;
            lp.weight = 0.25f;
            view2.setLayoutParams(lp);
            horizontalLayout.addView(view2);
        }
        myLayout.addView(horizontalLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }
    //  ---------           EDIT DESCRIPTIONS OF THE ELEMENT        ---------
    public static void mEditDescription(boolean bAsk) {     //if called with true it set up the dialog
        if (oElemViewProps==null) return;
        if (bAsk){
            oEditType = eEditType.kEditDescription;
            mLinLayPrep2();             //protocol data setup
            mLayoutAddTextEdit(0,"Descriptor of the variable      ", oElemViewProps.mDescr());
            mLayoutAddTextEdit(1,"Short alias name of the variable", oElemViewProps.mAlias());
            mDialogSetup1(mGetElemObj().sElemName());
        }
        else if ((oEditType == eEditType.kEditDescription)){
            //Get the fields from the editbox
            oElemViewProps.mDescr(nTextEditGet(0));                 //Input description
            oElemViewProps.mAlias(nTextEditGet(1));
            mGetElemObj().mSettings(false);       //mFileSave settings
            oEditType = eEditType.kNull;
            mRedrawParent();
            return;
        }
    }


    private static void mEditAccept(boolean bOpen) {  //R170522 open/close and accept a dialog b
        //Dispatcher for the response

        if (eEditType.kValue== oEditType){
            mInputValue(bOpen );
        }   else if (eEditType.kProtDataCalib== oEditType){
            mInputProtData(bOpen);
        }   else if (eEditType.kViewSettingsEdit== oEditType){
            mInputViewSettings1(bOpen);
        }  else if (eEditType.kInputRange== oEditType) {
            mInputRange(bOpen);
        }  else if (eEditType.kInputSelectDevice== oEditType) {
            mInputSelectDevice(bOpen);
        }  else if (eEditType.kEditDescription== oEditType) {
            mEditDescription(bOpen);
        }  else if (eEditType.kSelProtDataElem== oEditType){
            //mSelProtElem(bOpen);
        }  else if (eEditType.kUserLevel== oEditType){
            mUserLevel(bOpen);
        }  else if (eEditType.kBitNames== oEditType){      //Accept mEditBitDesc
            mEditBitDesc(bOpen,0);
        }

      //  if (bOpen==false) mAlertDialog.dismiss();
        bDoRedraw=true;             //Redraw all windows
    }



//  HELPER FUNCTIONS

    private static cUInput myInstance=new cUInput();
    private cUInput(){};
    public static cUInput mInit(Context mThis) {
        mContext=mThis;
        return myInstance;
    }



///////////////////////////////////////////////////////////////////////////////////////////////////////

    //-----------------implementations----------------------------
    public static void mShowHtml(String sTitle,String sDesc) {     //Alertbox with information
        /* supported html http://stacktips.com/tutorials/android/display-html-in-android-textview*/
        oEditType = null;
        mLinLayPrep2();             //protocol data setup
        WebView lblLbl1=new WebView(mContext);
        lblLbl1.loadData(sDesc, "text/html; charset=utf-8", "UTF-8");
        //lblLbl1.setMovementMethod(LinkMovementMethod.getInstance());    //Somehow makes links work as clickable
        //lblLbl1.setMovementMethod(new ScrollingMovementMethod());
        mAddLine2Layout((View) lblLbl1,null);
        mDialogSetup2(sTitle, oEditType);
    }
    private static void mDialogSetup2(String sTitle,  eEditType myEditType) {
//          SETUP the dialog

        if (alert==null)
            alert = new AlertDialog.Builder(mContext);
        // Set an EditText view to get user inText
        alert.setView(myLayout);
        alert.setCancelable(true);
        alert.setTitle(sTitle);

        alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {//171018  only caught if NOT setImeOptions(EditorInfo.IME_ACTION_DONE);
                    mEditAccept(false);
                    return true;
                }
                return false;
            }
        });
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mEditAccept(false);
                    mClose(dialog);
                }
        });
        if (cUInput.oEditType !=null) {
            alert.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mClose(dialog);

                }
            });
        }
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mClose(dialog);
            }
        });

        try {
            if(mAlertDialog==null)
                mAlertDialog = alert.create();
            if ((false==mAlertDialog.isShowing())) {
                mAlertDialog.show();
            }
            if (bHideKeyboard)
                mAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        } catch (Exception e) {
            Log.e("log_tag", "Error " + e.toString());
        }
        isDirty=false;      //set to true if some control is changed
    }
    private static void mDialogSetup1(String sTitle) {
//          SETUP the dialog for userinput
        mDialogSetup2(sTitle, oEditType);
        if (mAlertDialog!=null) return;
        //Rest is obsoleted
        if (alert==null)
            alert = new AlertDialog.Builder(mContext);
        // Set an EditText view to get user inText
        alert.setView(myLayout);
        alert.setCancelable(true);
        alert.setTitle(sTitle);

        alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    dialog.dismiss();                 //dismiss the alert. Will GC clean up??
                    mEditAccept(false);
                    return true;
                }
                return false;
            }
        });
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mEditAccept(false);
                mClose(dialog);
            }
        });
        alert.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mClose(dialog);

            }
        });
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mClose(dialog);
            }
        });
        try {
            if(mAlertDialog==null)
                mAlertDialog = alert.create();
            if ((false==mAlertDialog.isShowing())) {
                mAlertDialog.show();
            }
            if (bHideKeyboard)
                mAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        } catch (Exception e) {
            Log.e("log_tag", "Error " + e.toString());
        }
        isDirty=false;      //set to true if some control is changed
    }
    private static cElemViewProps    myViewData(View myEditControl) {         //Return the viewdata object of a view control
/*!-    cleaning 170919        if (myEditControl instanceof cVertSlider2) {
            return ((cVertSlider2) myEditControl).oElemViewProps;
        }
        else
        */
        if (myEditControl instanceof  cSignalView2)
            return ((cSignalView2) myEditControl).oElemViewProps();
        else
            return null;
    }
    public static void mClose(DialogInterface dialog) {
        oEditType = eEditType.kNull;
        bDoRedraw=true;
        dialog.dismiss();
        alert=null;
        mAlertDialog=null;
    }

    private static void mLinLayPrep2(){  //Prepare a layout of the UserInput
        mContext=(Context) oFocusdActivity;     //So it openes in all activities
        myLayout = new LinearLayout(mContext);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        myLayout.setOrientation(LinearLayout.VERTICAL);
        myLayout.setLayoutParams(parms);
    }
    public static void mUserLevel(boolean bAsk) {       //V170728
        String[] lstStr = {
                "User",
                "Advanced",   //
                "Admin" };
        if (bAsk) {
            oEditType = eEditType.kUserLevel;
            mLinLayPrep2();
            mLayoutAddDropDown1(1,"Privileges",lstStr,nAppProps[cKonst.eAppProps.kPrivileges.ordinal()]);
            mDialogSetup1("Select user level");             //Prepare dialog layout
        }else {       // Callback Get the fields from the editbox
            nAppProps[cKonst.eAppProps.kPrivileges.ordinal()]= mLayout_DropDown_SelIdx(1);
            oEditType = eEditType.kNull;
            mPersistAllData(false);         //Remember new setting
            return;
        }


    }
//............................      HANDLING EDITING    ............................
    /**
     * Show the soft keyboard
     */
    public static void showSoftKeyboard(View view){
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
//        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    /*

        view.requestFocus();
        imm =(InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.showSoftInput(view,InputMethodManager.SHOW_IMPLICIT);
        imm.showSoftInput(view,0);
        */
    }

//*********RUBBISH*********
public boolean isAlertDialogShowing(AlertDialog thisAlertDialog){
    if(thisAlertDialog != null){
        return thisAlertDialog.isShowing();
    }
    return false;
}
}