//              USER INPUT using the build in Dialog
//170915        Added method of getting focused control
//Doc:  https://docs.google.com/document/d/1iuD2xAL0aTZgCWTZS4Z6E5y8QVqJ1_82keiaz6wcHO0/edit
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
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
import static it.fdg.lm.cFileSystem.mFileRead;
import static it.fdg.lm.cFileSystem.mGetDownloadFileNames;
import static it.fdg.lm.cFileSystem.mGetPrefFileNames;
import static it.fdg.lm.cFileSystem.mGetRawFileNames;
import static it.fdg.lm.cFileSystem.mPref2Str;
import static it.fdg.lm.cFunk.mArrayFind;
import static it.fdg.lm.cFunk.mIndexStringArry;
import static it.fdg.lm.cFunk.mStr2Float;
import static it.fdg.lm.cFunk.mStr2Int;
import static it.fdg.lm.cKonst.eProtState.kDoConnect1;
import static it.fdg.lm.cKonst.eProtState.kProtInitDone;
import static it.fdg.lm.cKonst.eProtState.kProtInitInProgress;
import static it.fdg.lm.cKonst.eProtState.kProtReady;
import static it.fdg.lm.cKonst.eProtState.kProtUndef1;
import static it.fdg.lm.cKonst.eSerial.kBT_BrokenConnection;
import static it.fdg.lm.cKonst.eSerial.kBT_ConnectReq1;
import static it.fdg.lm.cKonst.eSerial.kBT_Connected1;
import static it.fdg.lm.cKonst.eSerial.kBT_DevicePickerActive;
import static it.fdg.lm.cKonst.eSerial.kBT_InvalidDevice1;
import static it.fdg.lm.cKonst.eSerial.kBT_TimeOut;
import static it.fdg.lm.cKonst.eSerial.kDeviceWasSelected;
import static it.fdg.lm.cProgram3.bAutoConnect;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.mCommunicate;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mMsgStatus;
import static it.fdg.lm.cProgram3.mPersistAllData;
import static it.fdg.lm.cProgram3.mPrivileges;
import static it.fdg.lm.cProgram3.nRefreshRate;
import static it.fdg.lm.cProgram3.oFocusdActivity;
import static it.fdg.lm.cProgram3.oaProtocols;
import static it.fdg.lm.cProgram3.sDevices2;
import static it.fdg.lm.cProgram3.sFile_ProtCfg;



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
    public static View oFocusedView;
    //      Controls for the alert myLayout
    static  Spinner[] inCB =new Spinner[6];
    static  Spinner cbProtNr;
    static  Spinner cbElemNr;
    static  Spinner cbElemIndex;
    static  Spinner cbSettingsFile;

    static  EditText txtRefresh;
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
    private static boolean isDirty;                 //Data has been edited and persistalldata should be called
    private static boolean bHideKeyboard=true;      //Hide softkeyboard on opening a dialog, waiting for user to click
    private static boolean bReturnKeyPressed;
    private static int myProtIdx=0;
    private static String sCmdTxt="";

    private static boolean bAccepted;
    private static cOptionBox oOptionBox;       //Location for loading files in mSettingsFile_Select
    private static int nSettingsDestIdx;

    private static String sDialogTitle="";

    //private static cBitField oBitField;         //When editing cBitField

    public static boolean mSelected() {         //Returns true if an element is selected
        return (oElemViewProps!=null);
    }

    public static String oCtrlID() {
        // id of focused control
        if (oElemViewProps==null) return "";
        String s = oElemViewProps.myId1;
        if (mElement()!=null)
            s=s+" - " +oElemViewProps.myProtElem1().sElemName();
        return  s;
    }
    static String mGetCtrlDescr(){      //171213
        String s = "";
        if (oElemViewProps==null) return s;
        s=oElemViewProps.myId1;
        if (mElement()!=null)
            s=s+" - " +oElemViewProps.myProtElem1().sElemName();
        return s;
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
            for (int i = 0; i < oaProtocols.length; i++) {
                final int finalI = i;
                mLayoutAddCmd2(oaProtocols[i].sProtName(),oaProtocols[i].getState().toString()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mDoConnect(finalI);
                        mClose(mAlertDialog);
                    }
                });

            }
            mDialogSetup2("Device status", oEditType);             //Prepare dialog layout
        }else {
            oEditType = eEditType.kNull;
            return;
        }
    }

    private static void mStartServerService() {
        if (mProtocol()!=null) {
            mProtocol().oSerial.mStartServerService();
        }
    }

    public static void mSettingsFile_Select() {  //return arguments via cbSettingsFile
        mLinLayPrep2();
        oOptionBox = new cOptionBox(mContext);
        oOptionBox.createRadioButton("Type  ", "Factory;Downloads;Internal",nSettingsDestIdx,true);
        oOptionBox.setListener(new cOptionBox.ChangeListener() {
                @Override
                public void onChange(int nIndex) {
                    nSettingsDestIdx = nIndex;
                    mSettingsFile_Select();
                }
            });
            mAddLine2Layout(oOptionBox, null);
        if (nSettingsDestIdx==0)
            cbSettingsFile=mLayoutAddDropDown3("Factory file ", mGetRawFileNames(), sFile_ProtCfg.replace(".xml",""));
        else if (nSettingsDestIdx==1)
            cbSettingsFile=mLayoutAddDropDown3("Downloads file ", mGetDownloadFileNames(".txt"), sFile_ProtCfg.replace(".txt",""));
        else
            cbSettingsFile=mLayoutAddDropDown3("Settings file ", mGetPrefFileNames(".xml"), sFile_ProtCfg.replace(".xml",""));
        mDialogSetup2("Settings status", oEditType);             //Prepare dialog layout
    }
    private static void mSettingsFile_Set(String sFileName) {
        String s="//"+sFileName+"\n";
        if (oOptionBox.mGetCheckedIndex()==0) {
            s =s+ cFileSystem.mFileRead_Raw(sFileName);
            ((cSetupFile)oFocusdActivity).mEditText(s);
        }
        else if (oOptionBox.mGetCheckedIndex()==1){
            s = s+mFileRead(mContext,sFileName );
            ((cSetupFile)oFocusdActivity).mEditText(s);
        } else {
            cFileSystem.mPrefFileNameSet(sFileName);
            s = s+mPref2Str("");
            ((cSetupFile)oFocusdActivity).mEditText(s);
        }
        mPersistAllData(true,sFileName);  //Load setup
    }

    private static void mVal(CheckBox cbDownloadsFolder, boolean b) {
        if (cbDownloadsFolder==null) return;
        cbDownloadsFolder.setChecked(b);
    }

    private static boolean mVal(CheckBox cb) {
        if (cb==null) return false;
        return cb.isChecked();
    }

    private static void mDoConnect(int i) {
        oaProtocols[i].mStateSet(kDoConnect1);
    }

    private static int nMyProtNr() {
        if (myProtIdx<0) myProtIdx=0;
        if (myProtIdx>oaProtocols.length)myProtIdx=oaProtocols.length-1;
        return myProtIdx;
    }

    public static void mRefresh(boolean doRedraw) {         //Refresh/draw the ui interface
        //Command selector
        if (doRedraw) {
            oElemViewProps=mGetActiveControl(oFocusedView);     //170915    Return active control of active view
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
        kConnect, kViewSettingsEdit, kBitNames, kUserLevel, kInputSelectDevice, kSelectProtocol, kSelectElement, kEditDescription
    }

    // --------------                   ************    INTERFACE  *******************
    private static String mGetText(Spinner inCB) {
        if (inCB==null) {mErrMsg("null pointer");return "";}
        if (inCB.getSelectedItem()==null) {mErrMsg("null pointer 170623");return "";}
        return inCB.getSelectedItem().toString();
    }
    private static String mGetText(EditText txtEdit) {         //Get the return value for an input
        if(txtEdit==null) return "";
        return txtEdit.getText().toString();
    }

    private static void mRedrawParent() {
        if (mContext instanceof cBitField) {
            //((cBitField) mContext).mRedrawMenu();
        } else if (mContext instanceof fMain){
            cProgram3.mRedraw();
        } else mErrMsg("Not implemented");
    }

    private static cProtocol3 oProtocol() {     //Return the current protocol
        if (mElement()!=null)
            return mElement().myProtocol();
        return  oaProtocols[0];
    }

                    //......................        INPUT A DISPLAY RANGE   ................
    public static void mInputRange(boolean bAsk) {     //if called with true it set up the dialog
        if (oElemViewProps==null) return;
        if (bAsk){            //First call set the inputboxes
            mLinLayPrep2();     //Prepare mInputRange
            mLayoutAddTextEdit(0,"Minimum visible value", mElement().nDisplayRange[0]);
            mLayoutAddTextEdit(1,"Maximum visible value", mElement().nDisplayRange[1]);
            mLayoutAddCmd(3,"Do autoscaling").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    oElemViewProps.mAutoRange(true);
                    mEditRedraw(false);
                }
            });
            mDialogSetup1(oElemViewProps.mAlias());          //Set dialog boxes
            oEditType = eEditType.kInputRange;                                   //Start editing data
        }
        else if ((oEditType == eEditType.kInputRange)){      //Accept data
            mElement().nDisplayRange[0] =mStr2Float(nTextEditGet(0), mElement().nDisplayRange[0]);
            mElement().nDisplayRange[1]=mStr2Float(nTextEditGet(1),  mElement().nDisplayRange[1]);
//!-            oElemViewProps.mSetSlider(mStr2Float(nTextEditGet(2)));
            isDirty=true;
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
            String s = mElement().mBitName(nEditIdx);
            mLayoutAddTextEdit(0,"Bit "+nEditIdx+" description",s);
            index= mElement().mBitVisible(nEditIdx);
            mLayoutAddDropDown1(1,"Visibility of control",lstViewStr,lstViewStr[index]);
            mDialogSetup1(sTitle);             //Prepare dialog layout

        }else {			//	Callback Get the fields from the editbox
            String s = mGetText(txtEdit[0]);
            mElement().mBitName(nEditIdx,s);
            index= mLayout_DropDown_SelIdx(1);
            mElement().nBitVisible(nEditIdx,index);
            oEditType = eEditType.kNull;
            isDirty=true;
            return;
        }

    }

    private static int mLayout_DropDown_SelIdx(int i) {
        if (inCB[i]==null) return -1;
        return inCB[i].getSelectedItemPosition();
    }

    private static void mButton_Scaling(boolean bAsk) {     //Add a scaling command button
        if (oFocusedView instanceof  cSignalView2) {
            mLayoutAddCmd(4, "Do autoscaling").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    oElemViewProps.mAutoRange(true);
                    mEditRedraw(false);
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



    public static void mInputViewSettings1(boolean bAsk) {     //if called with true it set up the dialog
        if (oElemViewProps==null) return;
        if (bAsk) {
            oEditType = eEditType.kViewSettingsEdit;
            mLinLayPrep2();         //prepare mInputViewSettings1
            mLayoutAddDropDown2(cbProtNr,"Device    ",sDevices2, sProtName());
            mLayoutAddDropDown2(cbElemNr, "Element   ", mElemNameList(), sElemName());
            if (mElement()!=null) {
                mLayoutAddDropDown2(cbElemIndex, "Index     ", mIndexStringArry(mElement()), oElemViewProps.mGetElemIdx());
                mLayoutAddTextEdit(0, "Minimum visible value", mElement().nDisplayRange[0]);
                mLayoutAddTextEdit(1, "Maximum visible value", mElement().nDisplayRange[1]);
                mLayoutAddCheckbox(3, "Visible for userinput", mFocusedViewProps().bVisible());
                mLayoutAddCheckbox(2, "Enabled for userinput", mFocusedViewProps().bEnabled());
                mLayoutAddCheckbox(4, "Enabled WriteDeviceOnStart", mFocusedViewProps().bWriteOnStart());
            }
            mLayoutAddCmd(2,"Edit colors").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    mStartColorPicker(oElemViewProps);  //Close dialog and open another
                }
            });
            mLayoutAddCmd(3,"Edit description and alias").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    mClose(mAlertDialog);
                    mEditDescription(true);
                }
            });
            mLayoutAddCmd1("Edit unit and conversion").setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {       //R170627
                    mClose(mAlertDialog);
                    mInputProtData(true);
                }
            });
            mButton_Scaling(bAsk);

            mDialogSetup2("Edit "+oCtrlID(),oEditType);             //Prepare dialog layout, needs to be last
        }else        if ((oEditType == eEditType.kViewSettingsEdit)){			//	Callback
            //Get the fields from the editbox
            if (mElement()!=null) {
                int idx = mLayout_DropDown_SelIdx(1);
                mElement().nDisplayRange[0] = mStr2Float(nTextEditGet(0), mElement().nDisplayRange[0]);
                mElement().nDisplayRange[1] = mStr2Float(nTextEditGet(1), mElement().nDisplayRange[1]);
            }
            if (mElement()!=null) {
                mFocusedViewProps().bEnabled( cbCheck[2].isChecked());
                mFocusedViewProps().bVisible( cbCheck[3].isChecked());
                mFocusedViewProps().bWriteOnStart(cbCheck[4].isChecked());
            }

            oEditType = eEditType.kNull;
            isDirty=true;
        }
    }



    private static void mApplySameScale() {        //Apply the same scaling to other handles in the sliderview
        if (oFocusedView instanceof  cSliderView) {
            cSlider o = ((cSliderView) oFocusedView).oParent;
            if (o==null ) return;
            o.mApplySameScale();
            mEditRedraw(false);
        }
    }

    private static cElemViewProps mFocusedViewProps() {
        return oElemViewProps;
    }
    private static cProtElem mElement() {
        if (oElemViewProps==null)
            return null;
        return oElemViewProps.myProtElem1();
    }


    private static String[] mElemNameList() {
        String[] sList = oProtocol().mGetProtElementList();
        return sList;
    }

    static String sElemName() {     //Return name of current element
        if (mElement()==null) return "";
        return mElement().sElemName();
    }

    private static void mStartColorPicker(cElemViewProps oProtElem) {
        Intent intent = new Intent(mContext, colorpick.class);
        mContext.startActivity(intent);
    }

//              EDIT THE CONTROL ELEMENT
    public static void mInputProtData(boolean bAsk) {     //if called with true it set up the dialog  170627B
        if (oElemViewProps==null)            return;
        if (mElement()==null) return;
        if (bAsk){
            oEditType = eEditType.kProtDataCalib;
            mLinLayPrep2();
            //Now set the fields
            mLayoutAddTextEdit(0, mElement().sElemName()+" integer offset", mElement().nOffset);
            mLayoutAddTextEdit(1,"integer conversion factor", mElement().nFactor);
            mLayoutAddTextEdit(2,"Units of conversion ", mElement().sUnit);             //input units
            int i = oElemViewProps.mRawValue();
            mLayoutAddTextEdit(3, mElement().mGetValueText(oElemViewProps.mGetElemIdx())+" = Int:",i);
            mDialogSetup1(oElemViewProps.mAlias());             //protocol data setup
        }else if ((oEditType == eEditType.kProtDataCalib)){
            //Get the fields from the editbox
            mElement().nOffset= mStr2Int(nTextEditGet(0));
            mElement().nFactor=mStr2Float(nTextEditGet(1), mElement().nFactor);
            mElement().sUnit=(nTextEditGet(2));
            isDirty=true;
            oEditType = eEditType.kNull;
            return;
        }
    }

    public static void mInputRefresh(boolean doOpen) {        //Input a single value,   R170727
        if (doOpen) {
            mCommunicate( false);
            mLinLayPrep2();
            mLayoutAddTextEdit2("Refresh time", cProgram3.mLoopTime);
            txtRefresh=mLayoutAddTextEdit2("Current rate", nRefreshRate());
            mDialogSetup1("Refresh rate");          //Set dialog boxes
        } else {
            if (txtRefresh == null) return;
            if (mGetText(txtRefresh )!= "") {
                int n = mStr2Int(mGetText(txtRefresh));
                nRefreshRate(n);
            }
            mCommunicate(true);
            oEditType = eEditType.kNull;
            txtRefresh = null;       //Remove as it is done
        }
    }

    public static void mInputValue(boolean bAsk) {        //Input a single value,   R170727
        if (oElemViewProps==null)            return;
        if (mElement()==null) return;
        if(bAsk){
            cProtElem myProtData= oElemViewProps.myProtElem1();
            if (myProtData.myVarId <=0) return;
            oEditType = eEditType.kValue;
            mLinLayPrep2();
            if (mElement().mIsInt())
                mLayoutAddTextEdit(0,"Value",(int)oElemViewProps.mGetValue());
            else
                mLayoutAddTextEdit(0,"Value",oElemViewProps.mGetValue());
            if (cProgram3.mPrivileges()>0) {
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
        cProtElem e = oElemViewProps.myProtElem1();
        int idx = oElemViewProps.mGetElemIdx();
        e.mColorIndexSet(idx,nColorLayer, nColorIndex);
    }

    //........................          HANDLING THE FOCUS OF VIEW CONTROLS................
    public static void mSetFocus(Object obj) {            //Set the focus on this control. !!! to implement some focus rectangle
        oFocusedView=null;
        if (obj instanceof View) {
            oElemViewProps = mGetActiveControl(obj);
            oFocusedView = oElemViewProps.myView;
            if (oFocusedView==null) oFocusedView= (View) obj;
            mContext = ((View) obj).getContext();
        }else{
            mErrMsg("To implement");
        }
        mCommand(false);
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
     if (mIsStateSerial1(kBT_ConnectReq1)) {                //Timeout
            }else if (mIsStateSerial1(kBT_InvalidDevice1)){                //Invalid device
                fMain.cmdText("Select a device - press");
                if (bExecute)
                    mInputSelectDevice(true);

            }else if (mIsStateSerial1(kBT_BrokenConnection)){                //Timeout
                fMain.cmdText("Lost connection");
                if (bAutoConnect())
                    mTryToConnect();
                if (bExecute)mTryToConnect();
            }else if ( mIsStateSerial1(kBT_DevicePickerActive)) {
                fMain.cmdText("Select a device ");
                if (bExecute) mTryToConnect();
            }else if ( mIsStateSerial1(kDeviceWasSelected)) {       //To obsolete
                String s= oProtocol().mDeviceNameGet();
                oProtocol().mDeviceNameSet(s);          //  Will initiate a connection

        }else if ( mIsStateProtocol(kProtInitDone)) {
            fMain.cmdText("Protocol Ready "+ nTestCount[0]);
        }else if ( mIsStateProtocol(kProtUndef1)) {
            fMain.cmdText("Undefined Protocol "+ nTestCount[0]);
            if (bExecute) mTryToConnect();
        }else if (mIsStateProtocol(kProtInitInProgress)) {
            fMain.cmdText(cKonst.eTexts.txtDevice_Initializing+" "+  oaProtocols[nMyProtNr()].mProtElemLength() );
            if (bExecute) mTryToConnect();
        }else if (mConnectionFault()) {                          //Error was encountered tell the user
            fMain.mCommandSet("Try Reset "+sProtName());
            mMsgStatus("Not Connected");
            if (bExecute) mProtStateSet(kDoConnect1);
        }else if (mElement()!=null) {
            fMain.cmdText(mGetCtrlDescr());
            mMsgStatus(mElement().sProtName());
            if (bExecute) cUInput.mInputValue(true);
        }else {
            mMsgStatus("Connect device");
            if (bExecute) mDoConnect(0);
     }


    }

    private static boolean mConnectionFault() {
        if(mElement()==null) return false;
        if(mElement().myProtocol()==null) return false;
        return (mElement().myProtocol().getState()!=kProtReady);
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
        if (nMyProtNr() > 0){
            cKonst.eSerial v = oaProtocols[nMyProtNr()].oSerial.mStateGet();
            return nSerialState == v;
        } else
            return false;
    }

    private static void mTryToConnect() {
        for (int i = 0; i< oaProtocols.length; i++)
            oaProtocols[i].mRequestConnection();
    }

    private static void mProtStateSet(cKonst.eProtState nNewState) {
        if (mProtocol()!=null)
            mProtocol().mStateSet(nNewState);
    }

    private static cProtocol3 mProtocol() {
        if (mElement()==null) return null;
        return mElement().myProtocol();
    }

    private static boolean mIsStateProtocol(cKonst.eProtState nProtState) {
        if (mIsStateSerial1(kBT_Connected1))
            return nProtState== oaProtocols[nMyProtNr()].getState();
        else {        //DO a check of states
            if (mIsStateSerial1(kBT_BrokenConnection))
                oProtocol().mRequestConnection();
        }
        return nProtState== oaProtocols[nMyProtNr()].getState();
    }


//............................      HANDLING EDITING    ............................











    //  ------------- Element scaling properties



    private static TextView mNewLabel(String sDesc) {
        TextView lbl = new TextView(mContext);
        lbl.setText(sDesc);
        mTextViewSetTextSize(lbl);
        return lbl;
    }
    //171219    Return a  drop down element
    private static Spinner mLayoutAddDropDown3(String sDesc, String[] sList, String sSelected) {
        int idx=mArrayFind(sList,sSelected);
        if (idx<0) idx=0;   //Limit imput range to be safe
        if (idx>=sList.length) idx=sList.length-1;
        return mLayoutAddDropDown3(sDesc,sList,idx);
    }
    private static Spinner mLayoutAddDropDown3(String sDesc, String[] sList, int idx) {
        Spinner inCB =new Spinner(mContext);
        TextView lblLbl1 = new TextView(mContext);
        lblLbl1.setText(sDesc);
        ArrayAdapter<String> adapter1=mMakeSpinnerAdapter(mContext,sList);
        inCB.setAdapter(adapter1);
        inCB.setSelection(idx);
        inCB.setEnabled(false);
        inCB.setOnItemSelectedListener(new OnItemSelectedListener() {    //These will fire automatically
            @Override
            public void onItemSelected(AdapterView<?> combobox, View view, int i, long l) {
                if (combobox.isEnabled()) {      //NOw see if user has changed it
                    if (mStr2Int(combobox.getTag().toString())!=i) {
                        isDirty = true;
                        if (combobox == cbProtNr) { //Change the protocol index
                            mSetProtocol(mGetText(cbProtNr));
                        }
                        if (combobox == cbElemNr) { //Change the element
                            mSetElement(mGetText(cbElemNr));
                        }
                        if (combobox == cbElemIndex) { //Change the index
                            mSetElemIdx(cbElemIndex.getSelectedItemPosition());

                        }
                    }
                    mRedrawInput();  //Accept and Reopen the editor

                }
                else {
                    combobox.setEnabled(true);
                }
                combobox.setTag(i);      //Set the last index
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mAddLine2Layout(lblLbl1,inCB);
        return inCB;
    }

    //170728    More recent drop down list accepting and index as selected item
    private static void mLayoutAddDropDown2(Spinner inCB, String sDesc, String[] sList, String sSelected) {
        int idx=mArrayFind(sList,sSelected);
        if (idx<0) idx=0;   //Limit imput range to be safe
        if (idx>=sList.length) idx=sList.length-1;
        mLayoutAddDropDown2(inCB,sDesc,sList,idx);
    }
    private static void mLayoutAddDropDown2(Spinner inCB, String sDesc, String[] sList, int idx) {
        TextView lblLbl1 = new TextView(mContext);
        lblLbl1.setText(sDesc);
        ArrayAdapter<String> adapter1=mMakeSpinnerAdapter(mContext,sList);
        inCB.setAdapter(adapter1);
        inCB.setSelection(idx);
        inCB.setEnabled(false);
        inCB.setOnItemSelectedListener(new OnItemSelectedListener() {    //These will fire automatically
            @Override
            public void onItemSelected(AdapterView<?> combobox, View view, int i, long l) {
                if (combobox.isEnabled()) {      //NOw see if user has changed it
                    if (mStr2Int(combobox.getTag().toString())!=i) {
                        isDirty = true;
                        if (combobox == cbProtNr) { //Change the protocol index
                            mSetProtocol(mGetText(cbProtNr));
                        }
                        if (combobox == cbElemNr) { //Change the element
                            mSetElement(mGetText(cbElemNr));
                        }
                        if (combobox == cbElemIndex) { //Change the index
                            mSetElemIdx(cbElemIndex.getSelectedItemPosition());

                        }
                    }
                    mRedrawInput();  //Accept and Reopen the editor

                }
                else {
                    combobox.setEnabled(true);
                }
                combobox.setTag(i);      //Set the last index
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mAddLine2Layout(lblLbl1,inCB);
    }

    private static void mSetElemIdx(int i) {
        oElemViewProps.mSetElemIdx(i);
    }

    private static void mSetElement(String s) {
       oElemViewProps.mSetElement(        oElemViewProps.mGetProtName(),s);
    }

    private static void mSetProtocol(String s) {
        oElemViewProps.mSetElement(s,        oElemViewProps.mGetElemName());
    }

    private static String sProtName() {
        if (mElement()==null) return "Null";
        return mElement().sProtName();
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
                        mEditAccept1();     //accept and close
                }
            }
        });
        //Handle the users next click on keyboard
        txtEdit[i].setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mEditAccept1();

                    bReturnKeyPressed=true;
                    return true;
                }
                bReturnKeyPressed=false;
                return false;
            }
        });

    }
    private static EditText mLayoutAddTextEdit2(String sDesc, Object Value) {
        TextView lblLbl = mNewLabel(sDesc);
        txtEdit[0] = new EditText(mContext);
        txtEdit[0].setText(""+Value);
        txtEdit[0].setMaxLines(1);
        if (Value instanceof Integer) {
            txtEdit[0].setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        }
        else if (Value instanceof Float)
            txtEdit[0].setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        else {
            txtEdit[0].setRawInputType(InputType.TYPE_CLASS_TEXT);
        }
        mAddLine2Layout((View) lblLbl,(View) txtEdit[0]);
        txtEdit[0].setFocusableInTouchMode(true);
        txtEdit[0].setHighlightColor(Color.BLACK);
        mShowKeyboard(txtEdit[0]);
        return txtEdit[0];
    }

    private static void mLayoutAddTextEdit1(EditText[] txtEdit, String sDesc, Object Value) {
        TextView lblLbl = mNewLabel(sDesc);
        txtEdit[0] = new EditText(mContext);
        txtEdit[0].setText(""+Value);
        txtEdit[0].setMaxLines(1);
        if (Value instanceof Integer) {
            txtEdit[0].setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        }
        else if (Value instanceof Float)
            txtEdit[0].setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        else {
            txtEdit[0].setRawInputType(InputType.TYPE_CLASS_TEXT);
        }
        mAddLine2Layout((View) lblLbl,(View) txtEdit[0]);
        txtEdit[0].setFocusableInTouchMode(true);
        txtEdit[0].setHighlightColor(Color.BLACK);
        mShowKeyboard(txtEdit[0]);
    }

    private static void mShowKeyboard(TextView txtEdit) {
        if (!bHideKeyboard)
            txtEdit.setOnFocusChangeListener(new View.OnFocusChangeListener(){
                public void onFocusChange(View v, boolean hasFocus){
                    if (hasFocus)
                        ((EditText)v).selectAll();        //But it's not easy to read then
                    if (v.isEnabled()==false){
                        if (bReturnKeyPressed)
                            mEditAccept1();     //accept and close
                    }
                }
            });
        //Handle the users next click on keyboard
        txtEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mEditAccept1();
                    bReturnKeyPressed=true;
                    return true;
                }
                bReturnKeyPressed=false;
                return false;
            }
        });

    }

    private static CheckBox mLayoutAddCheckbox3(String sDesc, boolean b) {
        TextView lblLbl = new TextView(mContext);
        lblLbl.setText(sDesc);
        CheckBox cbCheck1 = new CheckBox(mContext);
        cbCheck1.setChecked(b);
//        cbCheck1.setOnClickListener(new View.OnClickListener() {             public void onClick(View v) //R170627C
//        {
//            mRedrawInput();        }});
        mAddLine2Layout(lblLbl,cbCheck1);
        return cbCheck1;
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
        mEditRedraw(false);
        oEditType =et;
        mEditRedraw(true);          //Reopen with new values when checked
    }

    private static Button mLayoutAddCmd2(String sDesc,String sCmdTxt) {
        /* add a command button to the layout see: R170627  for using        */
        Button cmdCmd1 = new Button(mContext);
        cmdCmd1.setText(sCmdTxt);
        mAddLine2Layout(mNewLabel(sDesc),cmdCmd1);
        return cmdCmd1;
    }
    private static Button mLayoutAddCmd1(String sDesc) {
        /* add a command button to the layout see: R170627  for using        */
        Button cmdCmd1 = new Button(mContext);
        cmdCmd1.setText(sDesc);
        mAddLine2Layout(null,cmdCmd1);
        return cmdCmd1;
    }
    private static Button mLayoutAddCmd(int i, String sDesc) {
        /* add a command button to the layout see: R170627  for using        */
        cmdCmd = new Button(mContext);
        cmdCmd.setText(sDesc);
        mAddLine2Layout(null,cmdCmd);
        return cmdCmd;
    }

    private static String nTextEditGet(int i) {
        if (txtEdit[i]==null) return "";
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
            try {
                horizontalLayout.addView(view1);
            } catch (Error e){}
        }
        if (view2!=null) {
            lp.gravity = Gravity.RIGHT;
            lp.weight = 0.25f;
            if (view2.getParent()!=null)
                ((ViewGroup)view2.getParent()).removeView(view2);
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
            mDialogSetup2(mElement().sElemName(),eEditType.kEditDescription);
        }
        else if ((oEditType == eEditType.kEditDescription)){
            //Get the fields from the editbox
            oElemViewProps.mDescr(nTextEditGet(0));                 //Input description
            oElemViewProps.mAlias(nTextEditGet(1));
            isDirty=true;
            oEditType = eEditType.kNull;
            mRedrawParent();
            return;
        }
    }

    //Dispatcher for the response
    private static void mEditAccept1() {  //R170522 open/close and accept a dialog b
        if ((cbSettingsFile!=null)&(oFocusdActivity instanceof cSetupFile)){
            String sFileName =mGetText(cbSettingsFile);
            mSettingsFile_Set(sFileName);
            cbSettingsFile=null;
        }
        boolean bOpen = false;
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

        }  else if (eEditType.kBitNames== oEditType){      //Accept mEditBitDesc
            mEditBitDesc(bOpen,0);
        }

     if (bOpen==false) mClose(mAlertDialog);
        bDoRedraw=true;             //Redraw all windows
    }



    private static void mEditRedraw(boolean bOpen) {  //R170522 open/close and accept a dialog b
        if (cbSettingsFile!=null) return;
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

        }  else if (eEditType.kBitNames== oEditType){      //Accept mEditBitDesc
            mEditBitDesc(bOpen,0);
        }

        if (bOpen==false) mClose(mAlertDialog);
        bDoRedraw=true;             //Redraw all windows
    }



//  HELPER FUNCTIONS

    private static cUInput myInstance=new cUInput();
    private cUInput(){};
    public static cUInput mInit(Context mThis) {
        mContext=mThis;
        cbProtNr =new Spinner(mContext);
        cbElemNr =new Spinner(mContext);
        cbElemIndex =new Spinner(mContext);
        inCB[0]=new Spinner(mContext);
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
                    bAccepted = true;           //Input was accepted
                    mEditAccept1();
                    return true;
                }
                return false;
            }
        });
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bAccepted = true;           //Input was accepted
                    mEditAccept1();
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
            mClose(mAlertDialog);
        }
        isDirty=false;      //set to true if some control is changed
    }
    private static void mDialogSetup1(String sTitle) {
//          SETUP the dialog for userinput
        mDialogSetup2(sTitle, oEditType);
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
        mInputRefresh(false);
        oEditType = eEditType.kNull;
        bDoRedraw=true;
        if (dialog!=null)        dialog.dismiss();
        alert=null;
        mAlertDialog=null;

    }

    private static void mLinLayPrep2(){  //Prepare a layout of the UserInput
        mClose(mAlertDialog);           //Close any existing alert dialogs
        mContext=(Context) oFocusdActivity;     //So it openes in all activities
        if (myLayout !=null) myLayout.removeAllViews();
        myLayout = new LinearLayout(mContext);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        myLayout.setOrientation(LinearLayout.VERTICAL);
        myLayout.setLayoutParams(parms);

    }
    public static void mSettingsDialog() {       //171222
        mLinLayPrepare("Settings");
        cOptionBox ob = mOptionBox("Permissions ", "User;Advanced;Admin", cProgram3.mPrivileges(), true);
        ob.setListener(new cOptionBox.ChangeListener() {
            @Override
            public void onChange(int nIndex) {
                cProgram3.mPrivileges(nIndex);
            }
        });
        Button btn = mLayoutAddCmd2("Protocol", "Load");
        btn.setOnClickListener(v -> {
            BaseActivity.mDispSettings();
        });
        if (mPrivileges()>0) {
            CheckBox cb1 = mLayoutAddCheckbox3("Design mode", cProgram3.bDesignMode());
            cb1.setOnClickListener(v -> {
                cProgram3.bDesignMode(!cProgram3.bDesignMode());
                cProgram3.bDoRedraw = true;         //Clicked action
            });

            mLayoutAddCmd2("Listen for client", "Activate").setOnClickListener(v -> {
                mStartServerService();
                mClose(mAlertDialog);
            });
            mLayoutAddCmd2("Refresh rate", "" + nRefreshRate()).setOnClickListener(v -> {
                mInputRefresh(true);
            });
        }
        mDialogShow();             //Prepare dialog layout
    }

    private static cOptionBox mOptionBox(String s, String s1, int i, boolean b) {
        cOptionBox ob = new cOptionBox(mContext);
        ob.createRadioButton(s,s1,i,b);
        mAddLine2Layout(ob, null);
        return ob;
    }

    public static void mUserLevel() {       //171222
        mLinLayPrepare("Select user level");
        cOptionBox oUserLvl = new cOptionBox(mContext);
        oUserLvl.createRadioButton("Permissions ", "User;Advanced;Admin",cProgram3.mPrivileges(),true);
        oUserLvl.setListener(new cOptionBox.ChangeListener() {
            @Override
            public void onChange(int nIndex) {
                cProgram3.mPrivileges(nIndex);
            }
        });
        mAddLine2Layout(oUserLvl, null);
        mDialogShow();             //Prepare dialog layout
    }

    private static void mLinLayPrepare(String s) {
        sDialogTitle=s;
        mLinLayPrep2();
    }

    private static void mDialogShow() {
        mDialogSetup1(sDialogTitle);
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