//              USER INPUT using the build in Dialog
//180308        Added method of getting focused control
//Doc:  https://docs.google.com/document/d/1iuD2xAL0aTZgCWTZS4Z6E5y8QVqJ1_82keiaz6wcHO0/edit
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
package it.fdg.lm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import static it.fdg.lm.cAndMeth.mMakeSpinnerAdapter;
import static it.fdg.lm.cFileSystem.mFileRead;
import static it.fdg.lm.cFileSystem.mFileSave;
import static it.fdg.lm.cFileSystem.mGetDownloadFileNames;
import static it.fdg.lm.cFileSystem.mGetPrefFileNames;
import static it.fdg.lm.cFileSystem.mGetRawFileNames;
import static it.fdg.lm.cFileSystem.mPref2Str;
import static it.fdg.lm.cFileSystem.mPref_Clear;
import static it.fdg.lm.cFileSystem.mStr2Pref;
import static it.fdg.lm.cFunk.mArrayFind;
import static it.fdg.lm.cFunk.mIndexStringArry;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cFunk.mStr2Float;
import static it.fdg.lm.cFunk.mStr2Int;
import static it.fdg.lm.cFunk.mStr2StrArr;
import static it.fdg.lm.cKonst.eProtState.kDoConnect1;
import static it.fdg.lm.cKonst.eProtState.kProtInitDone;
import static it.fdg.lm.cKonst.eProtState.kProtInitFailure;
import static it.fdg.lm.cKonst.eProtState.kProtInitInProgress;
import static it.fdg.lm.cKonst.eProtState.kProtReady;
import static it.fdg.lm.cKonst.eProtState.kProtResetReq1;
import static it.fdg.lm.cKonst.eProtState.kProtUndef1;
import static it.fdg.lm.cKonst.eSerial.kBT_BrokenConnection;
import static it.fdg.lm.cKonst.eSerial.kBT_Connected1;
import static it.fdg.lm.cKonst.eSerial.kBT_Undefined;
import static it.fdg.lm.cProgram3.bDoRedraw;
import static it.fdg.lm.cProgram3.mAlert2;
import static it.fdg.lm.cProgram3.mAppSettings;
import static it.fdg.lm.cProgram3.mCommandTxt;
import static it.fdg.lm.cProgram3.mCommunicate;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mPersistAllData;
import static it.fdg.lm.cProgram3.nRefreshRate;
import static it.fdg.lm.cProgram3.oFocusdActivity;
import static it.fdg.lm.cProgram3.oUInput;
import static it.fdg.lm.cProgram3.oaProtocols;
import static it.fdg.lm.cProgram3.sDevices2;
import static it.fdg.lm.cProgram3.sFile_AppCfg;
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
    public static Context _mContext=cProgram3.mContext;
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
    private static cOptionBox oListMemory;       //Location for loading files in mSettingsFile_Select
    private static int nSettingsDestIdx;

    private static String sDialogTitle="";
    private static GridLayout gridLayout;
    private static cOptionBox obPrivileges;
    private EditText oRefreshRate;
    private EditText oReturnValue;
    private Spinner oDD1;
    private Spinner oDD2;
    private Spinner oDD3;
    private Spinner oDD4;
    private EditText oVL1;
    private EditText oVL2;
    private CheckBox oCB1;
    private CheckBox oCB2;

    private Spinner cbFactorySettingsFile;
    private static String sMessages="";
    private static long nClosingTime=0;

    //private static cBitField oBitField;         //When editing cBitField

    public static boolean mSelected() {         //Returns true if an element is selected
        return (oUInput.oElemViewProps!=null);
    }

    public static String oCtrlID() {
        // id of focused control
        if (oUInput.oElemViewProps==null) return "";
        String s = oUInput.oElemViewProps.myId1;
        if (mElement()!=null)
            s=s+" - " +oElemViewProps.myProtElem1().sElemName();
        return  s;
    }
    static String mGetCtrlDescr(){      //171213
        String s = "";
        if (oElemViewProps==null) return s;
        s=oElemViewProps.myId1;
        if (mElement()!=null)
            s=s+" - " +oElemViewProps.myProtElem1().sElemName();    //180403
            s=oElemViewProps.myProtElem1().sElemName() +" "+ oElemViewProps.mGetValueText();
        return s;
    }
    public static String sValue() {
        return (""+oElemViewProps.mGetValue());
    }

    public static cElemViewProps mGetViewProps() {
        return  oElemViewProps;
    }

    private static void mStartServerService() {
        if (mProtocol()!=null) {
            mProtocol().oSerial.mStartServerService();
        }
    }


    private static void mSettingsFile_Load(Spinner cbSettingsFile, cOptionBox oListMemory) { //Load a file from factory,download or preferences
        sFile_ProtCfg =mGetText(cbSettingsFile);
        String s="//"+sFile_ProtCfg+"\n";
        mAppSettings(false);        //Remember what you selected
        if (oListMemory.mGetCheckedIndex()==0) {
            s =s+ cFileSystem.mFileRead_Raw(sFile_ProtCfg);
            mPref_Clear();            //Remove old preferences
            s=mStr2Pref(sFile_ProtCfg,s);                      //Transfer to preferences
            mPersistAllData(true,sFile_ProtCfg);  //Load setup
          }
        else if (oListMemory.mGetCheckedIndex()==1){
            s = s+mFileRead(cProgram3.mContext,sFile_ProtCfg+".txt" );
            mPref_Clear();            //Remove old preferences
            s=mStr2Pref(sFile_ProtCfg,s);                      //Transfer to preferences
            mPersistAllData(true,sFile_ProtCfg);  //Load setup
        } else {
            cFileSystem.mPrefFileNameSet(sFile_ProtCfg);
            mPersistAllData(true,sFile_ProtCfg);  //Load setup
        }
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

    public void mZero() {
        cProgram3.mMsgLog(2,"Zeroed");
        oElemViewProps.mSetValue( 0);
    }

    public   Boolean mCommandLongPress() {
        mMessage(cProgram3.mMsgLog(),0);
        return true;
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
        if (inCB==null) {
            //mErrMsg("null ptr");
            return "";
        }
        if (inCB.getSelectedItem()==null) {mErrMsg("null pointer 170623");return "";}
        return inCB.getSelectedItem().toString();
    }
    private static String mGetText(EditText txtEdit) {         //Get the return value for an input
        if(txtEdit==null) return "";
        return txtEdit.getText().toString();
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
        Intent intent = new Intent(mContext(), colorpick.class);
        mContext().startActivity(intent);
    }

    private static Context mContext() {
        return (Context)oFocusdActivity;
    }

//              EDIT THE CONTROL ELEMENT


    public static void mInputRefresh(boolean doOpen) {        //Input a single value,   R170727
        if (doOpen) {
            mCommunicate( false);   //Changing the refresh rate requires to stop communication
            mLinLayPrep2();
//            mLayoutAddTextEdit2("Refresh time (max: "+cProgram3.mLoopTime+")" , cProgram3.mLoopTime);
            txtRefresh=mLayoutAddTextEdit2("Refresh rate", nRefreshRate());
            mDialogSetup1("Refresh rate (max: "+cProgram3.mLoopTime+")");          //Set dialog boxes
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
            if (oElemViewProps!=null) oFocusedView = oElemViewProps.myView;
            if (oFocusedView==null) oFocusedView= (View) obj;
            _mContext = ((View) obj).getContext();
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
        } else if (oView instanceof cData_View){
            return ((cData_View)oView).oElemViewProps();
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
        //  Serial states if not connected
    switch (mSerialState()) {       //Connection issues
        case kBT_Connecting:
            mCommandTxt("Calling "+ mDeviceName());
            return;
        case kBT_TimeOut:
        case kBT_Undefined:
        case kBT_BrokenConnection:
        case kBT_Disconnected:
            mCommandTxt(mDeviceName()+" disconnected - Connect");        //n180328a
            if (bExecute) mTryToConnect();
            return;
        case kBT_ConnectReq1:
            mCommandTxt("Connecting to " + mDeviceName());        //n180328a
            if (bExecute) mTryToConnect();
            return;

        case kDeviceWasSelected:
            mCommandTxt("Selected device " + mDeviceName() );        //n180328a
            if (bExecute) mTryToConnect();
            return;
        }
        //      Protocol states if connected
         if ( mIsStateProtocol(kProtInitDone)) {
            if (bExecute) mTryToConnect();
        }else if ( mIsStateProtocol(kProtUndef1)) {
            if (bExecute) mTryToConnect();
        }else if (mIsStateProtocol(kProtInitInProgress)) {
            mCommandTxt("Initializing protocol - wait");        //n180328a
            if (bExecute) mTryToConnect();

    } else if (mIsStateProtocol(kProtInitFailure)){
        mCommandTxt("Failure in init - Push to retry");        //n180328a
        if (bExecute) oaProtocols[nMyProtNr()].mStateSet(kProtResetReq1);
    } else if (mElement()!=null) {
            mCommandTxt(mGetCtrlDescr());
            if (bExecute) oUInput.mInputValue1();
     }else if (mAreProtocolsReady()) {  //Moved to 180420b
             mCommandTxt("Ready, Drag a control");

        }else if (mConnectionFault()) {                          //Error was encountered tell the user
             mCommandTxt("Disconnected - try to Connect?");        //n180328a
            if (bExecute) mProtStateSet(kDoConnect1);
        }
    }

    private static boolean mAreProtocolsReady() {
        for (int i = 0; i< oaProtocols.length; i++)
            if (oaProtocols[i].getState()!=kProtReady) return false;
        return true;
    }

    private static String mDeviceName() {
        return oaProtocols[myProtIdx].mDeviceNameGet();
    }

    private static void mMsgStatusErr(String msg) { //Setting both command button and message popup
        fMain.cmdText(msg);
        mErrMsg(msg);
    }

    private static cKonst.eSerial mSerialState() {
        if (oaProtocols==null) return kBT_Undefined;
        if (oaProtocols[nMyProtNr()]==null) return kBT_Undefined;
        if (oaProtocols[nMyProtNr()].oSerial==null) return kBT_Undefined;
        return oaProtocols[nMyProtNr()].oSerial.mStateGet();
    }

    private static boolean mConnectionFault() {
        if(mElement()==null) return false;
        if(mElement().myProtocol()==null) return false;
        return (mElement().myProtocol().getState()!=kProtReady);
    }


    private static void mTryToConnect() {       //Connect if not connected
        for (int i = 0; i< oaProtocols.length; i++)
            if (oaProtocols[i].oSerial.isConnected()==false)
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
        if (mSerialState()==kBT_Connected1)
            return nProtState== oaProtocols[nMyProtNr()].getState();
        else {        //DO a check of states
            if (mSerialState()==kBT_BrokenConnection)
                oProtocol().mRequestConnection();
        }
        return nProtState== oaProtocols[nMyProtNr()].getState();
    }


//............................      HANDLING EDITING    ............................











    //  ------------- Element scaling properties



    private static TextView mNewLabel(String sDesc) {
        TextView lbl = new TextView(mContext());
        lbl.setText(sDesc);
        //mTextViewSetTextSize(lbl);
        return lbl;
    }
    //171219    Return a  drop down element
    private static Spinner mLayoutAddDropDown(String sDesc, String[] sList, String sSelected) {
        int idx=mArrayFind(sList,sSelected);
        if (idx<0) idx=0;   //Limit imput range to be safe
        if (idx>=sList.length) idx=sList.length-1;
        return mLayoutAddDropDown3(sDesc,sList,idx);
    }

    private   Spinner mAddCombo4(String sDesc, String[] sList, Object sSelected) {
        int idx;
        if (sSelected instanceof Integer){
            idx=(Integer) sSelected;
        }else
        {
            idx=mLimit(0, mArrayFind(sList,(String) sSelected) ,sList.length-1);
        }
        Spinner inCB =new Spinner(mContext());
        TextView lblLbl1 = new TextView(mContext());
        lblLbl1.setText(sDesc);
        ArrayAdapter<String> adapter1=mMakeSpinnerAdapter(mContext(),sList);
        inCB.setAdapter(adapter1);
        inCB.setSelection(idx);
        mAddView(mNewLabel(sDesc),inCB);
      //  inCB.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);  //180404

        return inCB;
    }
    private   Spinner mAddCombo(String sDesc, String[] sList, Object sSelected) {
        int idx;
        if (sSelected instanceof Integer){
            idx=(Integer) sSelected;
        }else
        {
             idx=mLimit(0, mArrayFind(sList,(String) sSelected) ,sList.length-1);
        }
        Spinner inCB =new Spinner(mContext());
        TextView lblLbl1 = new TextView(mContext());
        lblLbl1.setText(sDesc);
        ArrayAdapter<String> adapter1=mMakeSpinnerAdapter(mContext(),sList);
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
                            int j=cbProtNr.getSelectedItemPosition();
                            mSetProtocol(oaProtocols[j].sProtName());
                        }
                        if (combobox == cbElemNr) { //Change the element
                            mSetElement(mGetText(cbElemNr));
                        }
                        if (combobox == cbElemIndex) { //Change the index
                            mSetElemIdx(cbElemIndex.getSelectedItemPosition());

                        }
                    }
//                    mRedrawInput();  //Accept and Reopen the editor conflicting after 180412

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
        mAddView(mNewLabel(sDesc),inCB);
        return inCB;
    }
    private static Spinner mLayoutAddDropDown3(String sDesc, String[] sList, int idx) {
        Spinner inCB =new Spinner(mContext());
        TextView lblLbl1 = new TextView(mContext());
        lblLbl1.setText(sDesc);
        ArrayAdapter<String> adapter1=mMakeSpinnerAdapter(mContext(),sList);
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
                            int j=cbProtNr.getSelectedItemPosition();
                            mSetProtocol(oaProtocols[j].sProtName());
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
        TextView lblLbl1 = new TextView(mContext());
        lblLbl1.setText(sDesc);
        ArrayAdapter<String> adapter1=mMakeSpinnerAdapter(mContext(),sList);
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
                            int j=cbProtNr.getSelectedItemPosition();
                            mSetProtocol(oaProtocols[j].sProtName());
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
        lblLbl[i] = new TextView(mContext());
        lblLbl[i].setText(sDesc);
        inCB[i] =new Spinner(mContext());
        ArrayAdapter<String> adapter1=mMakeSpinnerAdapter(mContext(),sList);
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
        txtEdit[i] = new EditText(mContext());
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
        cAndMeth.mTextEdit_SetFocusable(txtEdit[i]);
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
        txtEdit[0] = new EditText(mContext());
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

        cAndMeth.mTextEdit_SetFocusable(txtEdit[0]);

        mShowKeyboard(txtEdit[0]);
        return txtEdit[0];
    }

    private static void mLayoutAddTextEdit1(EditText[] txtEdit, String sDesc, Object Value) {
        TextView lblLbl = mNewLabel(sDesc);
        txtEdit[0] = new EditText(mContext());
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
        cAndMeth.mTextEdit_SetFocusable(txtEdit[0]);
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
    private  CheckBox mAddCheckBox4(String sDesc, boolean b) {
        CheckBox cbCheck1 = new CheckBox(mContext());
        cbCheck1.setChecked(b);
//        cbCheck1.setOnClickListener(new View.OnClickListener() {             public void onClick(View v) //R170627C
//        {
//            mRedrawInput();        }});
        mAddView(mNewLabel(sDesc),cbCheck1);
        return cbCheck1;
    }


    private static CheckBox mLayoutAddCheckbox(int i, String sDesc, boolean b) {
        lblLbl[i] = new TextView(mContext());
        lblLbl[i].setText(sDesc);
        cbCheck[i] =new CheckBox(mContext());
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
    private int mViewValue(EditText editValue) {
        int v = mStr2Int(editValue.getText().toString());
        return v;
    }
    private String mViewStr(EditText editValue) {
        String v = editValue.getText().toString();
        return v;
    }

    private EditText mAddEdit(String sDesc, Object Value) {
        EditText txtEdit = new EditText(mContext());
        txtEdit.setText(""+  Value);
        txtEdit.setMaxLines(1);
        cAndMeth.mTextEdit_SetFocusable(txtEdit);

        if (Value instanceof Integer) {
            txtEdit.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        }
        else if (Value instanceof Float)
            txtEdit.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        else {
            txtEdit.setRawInputType(InputType.TYPE_CLASS_TEXT);
        }
        mAddView(mNewLabel(sDesc), txtEdit);            //Add two views to the grid
        /*
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
           //     InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //     imm.showSoftInput(txtEdit, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        */
        return txtEdit;
    }
    private   Button mCmdButton(String sCmdTxt) {
        Button cmdCmd1 = new Button(mContext());
        cmdCmd1.setText(sCmdTxt);
        return cmdCmd1;
    }
    private   Button mAddCmd3(String sDesc, String sCmdTxt) {
        /* add a command button to the layout see: R170627  for using        */
        Button cmdCmd1 = new Button(mContext());
        cmdCmd1.setText(sCmdTxt);
        mAddView(mNewLabel(sDesc),cmdCmd1);
        return cmdCmd1;
    }
    private Button mLayoutAddCmd2(String sDesc,String sCmdTxt) {
        /* add a command button to the layout see: R170627  for using        */
        Button cmdCmd1 = new Button(mContext());
        cmdCmd1.setText(sCmdTxt);
        mAddView(mNewLabel(sDesc),cmdCmd1);
        return cmdCmd1;
    }
    private static Button mLayoutAddCmd1(String sDesc) {
        /* add a command button to the layout see: R170627  for using        */
        Button cmdCmd1 = new Button(mContext());
        cmdCmd1.setText(sDesc);
        mAddLine2Layout(null,cmdCmd1);
        return cmdCmd1;
    }
    private static Button mLayoutAddCmd(int i, String sDesc) {
        /* add a command button to the layout see: R170627  for using        */
        cmdCmd = new Button(mContext());
        cmdCmd.setText(sDesc);
        mAddLine2Layout(null,cmdCmd);
        return cmdCmd;
    }

    private static String nTextEditGet(int i) {
        if (txtEdit[i]==null) return "";
        return txtEdit[i].getText().toString();
    }


    private static void mAddLine2Layout(View view1, View view2) {   //170612    Make a horizontal row of two elements to left-right
        LinearLayout horizontalLayout = new LinearLayout(mContext());
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
    public   void mInputProtData2() { //Revised Scaling data editing 180418
        mDialogGridInit1("Calibration of "+mElement().sElemName());
            EditText oOffset = mAddEdit("Integer offset", mElement().nOffset);
            EditText oFactor = mAddEdit("Conversion factor", mElement().nFactor);
            EditText oUnits = mAddEdit("Units of conversion ", mElement().sUnit);             //input units
            TextView oLbl = mNewLabel(mElement().mGetValueText(oElemViewProps.mGetElemIdx()) + " = Int:" + oElemViewProps.mRawValue());
            mAddView(null,oLbl);
        //Action on confirm
        alert.setPositiveButton( "Confirm", new DialogInterface.OnClickListener() {     //ToDo 180413: Improve this layout
            public void onClick(DialogInterface dialog, int id) {
                //Get the fields from the editbox
                mElement().nOffset= mStr2Int(mGetText(oOffset));
                mElement().nFactor=mStr2Float(mGetText(oFactor), mElement().nFactor);
                mElement().sUnit=(mGetText(oUnits));
                mAccept2();
                mClose1();
            }});
            mAlertShow();
    }
    //  ---------           EDIT DESCRIPTIONS OF THE ELEMENT        ---------
    public   void mEditDescription1( ) {     //180417 To rewrite
        mDialogGridInit1("Description of "+oElemViewProps.mGetProtName());
        EditText oDescr = mAddEdit("Description", oElemViewProps.mDescr());
        EditText oAlias = mAddEdit("Alias", oElemViewProps.mAlias());
        alert.setPositiveButton( "OK", new DialogInterface.OnClickListener() {     //ToDo 180413: Improve this layout
            public void onClick(DialogInterface dialog, int id) {
                oElemViewProps.mDescr(mGetText(oDescr));                 //Input description
                oElemViewProps.mAlias(mGetText(oAlias));
                mAccept2();
            }});
        mAlertShow();
    }
    public  void mButton_Scaling1( ) {     //Add a scaling command button
        mDialogGridInit1("Scaling of "+oElemViewProps.mGetProtName());
        Button c = mAddCmd3(sElemName(), "Do autoscaling");
        c.setOnClickListener(v -> {
            oElemViewProps.mAutoRange(true);
        });
        Button c1 = mAddCmd3(sElemName(), "Same Scale");
        c1.setOnClickListener(v -> {
            mApplySameScale();
        });
    }


    //Dispatcher for the response
    private static void mEditAccept1() {  //R170522 open/close and accept a dialog b

        boolean bOpen = false;
        if (eEditType.kValue== oEditType){
            //mInputValue(bOpen );
        }   else if (eEditType.kProtDataCalib== oEditType){

        }   else if (eEditType.kViewSettingsEdit== oEditType){

        }  else if (eEditType.kInputRange== oEditType) {
            mInputRange(bOpen);
        }  else if (eEditType.kInputSelectDevice== oEditType) {
            mErrMsg("180410 Not implemented");//mInputSelectDevice(bOpen);
        }  else if (eEditType.kEditDescription== oEditType) {

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
            //mInputValue(bOpen );
        }   else if (eEditType.kProtDataCalib== oEditType){

        }   else if (eEditType.kViewSettingsEdit== oEditType){

        }  else if (eEditType.kInputRange== oEditType) {
            mInputRange(bOpen);
        }  else if (eEditType.kInputSelectDevice== oEditType) {
            mErrMsg("180410 Not implemented");//mInputSelectDevice(bOpen);
        }  else if (eEditType.kEditDescription== oEditType) {

        }  else if (eEditType.kSelProtDataElem== oEditType){
        }  else if (eEditType.kUserLevel== oEditType){

        }  else if (eEditType.kBitNames== oEditType){      //Accept mEditBitDesc
            mEditBitDesc(bOpen,0);
        }

        if (bOpen==false) mClose(mAlertDialog);
        bDoRedraw=true;             //Redraw all windows
    }



//  HELPER FUNCTIONS
     public cUInput(Context ctx){_mContext=ctx;};
    public void   mInit(Context mThis) {
        _mContext=mThis;
        cbProtNr =new Spinner(mContext());
        cbElemNr =new Spinner(mContext());
        cbElemIndex =new Spinner(mContext());
        inCB[0]=new Spinner(mContext());

    }



///////////////////////////////////////////////////////////////////////////////////////////////////////

    //-----------------implementations----------------------------
    public static void mShowHtml(String sTitle,String sDesc) {     //Alertbox with information
        /* supported html http://stacktips.com/tutorials/android/display-html-in-android-textview*/
        oEditType = null;
        mLinLayPrep2();             //protocol data setup
        WebView lblLbl1=new WebView(mContext());
        lblLbl1.loadData(sDesc, "text/html; charset=utf-8", "UTF-8");
        //lblLbl1.setMovementMethod(LinkMovementMethod.getInstance());    //Somehow makes links work as clickable
        //lblLbl1.setMovementMethod(new ScrollingMovementMethod());
        mAddLine2Layout((View) lblLbl1,null);
        mDialogSetup2(sTitle, oEditType);
    }

    private  void mDialogGridInit1(String sTitle) {            //Version 180413  show with mAlertShow();
        if (mAlertDialog!=null) mClose(mAlertDialog);
        alert = new AlertDialog.Builder(mContext());
        // Set an EditText view to get user inText
        gridLayout = new GridLayout(mContext());
        gridLayout.setColumnCount(2);
        alert.setView(gridLayout);
        alert.setCancelable(true);
        alert.setTitle(sTitle);
        alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {//171018  only caught if NOT setImeOptions(EditorInfo.IME_ACTION_DONE);
                    mAccept2();          //Input was accepted
                    return true;
                }
                return false;
            }
        });
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
//                mAccept2();         //Input was accepted
                mClose1( );
            }
        });
         alert.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mClose1();
            }
        });
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mClose1( );
            }
        });
    }

    private void mAlertShow() {
        try {
            // if(mAlertDialog==null)
            mAlertDialog = alert.create();
            // if ((false==mAlertDialog.isShowing())) {
            mAlertDialog.show();
            //We don't know why but following seems to enable the softkeyboard see:https://stackoverflow.com/questions/9102074/android-edittext-in-dialog-doesnt-pull-up-soft-keyboard
            mAlertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);    //Rev 180327
            //}
        } catch (Exception e) {
            Log.e("log_tag", "Error " + e.toString());
            mClose(mAlertDialog);
        }
    }

    private  void mDialogGridInit(String sTitle) {            //Version 180328
        if (mAlertDialog!=null) mClose(mAlertDialog);
        alert = new AlertDialog.Builder(mContext());
        // Set an EditText view to get user inText
        gridLayout = new GridLayout(mContext());
        gridLayout.setColumnCount(2);
        alert.setView(gridLayout);
        alert.setCancelable(true);
        alert.setTitle(sTitle);
        alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {//171018  only caught if NOT setImeOptions(EditorInfo.IME_ACTION_DONE);
                    mAccept2();          //Input was accepted
                    return true;
                }
                return false;
            }
        });
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mAccept2();         //Input was accepted
                mClose1( );
            }
        });

        alert.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mClose1();
            }
        });
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mClose1( );
            }
        });
        try {
            // if(mAlertDialog==null)
            mAlertDialog = alert.create();
            // if ((false==mAlertDialog.isShowing())) {
            mAlertDialog.show();
            //We don't know why but following seems to enable the softkeyboard see:https://stackoverflow.com/questions/9102074/android-edittext-in-dialog-doesnt-pull-up-soft-keyboard
            mAlertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);    //Rev 180327
            //}
        } catch (Exception e) {
            Log.e("log_tag", "Error " + e.toString());
            mClose(mAlertDialog);
        }
    }
    private View mAddView(View View1, View ValView){
        if (View1!=null)
            gridLayout.addView(View1);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
//        params.columnSpec = GridLayout.spec(1);
        params.setGravity(Gravity.FILL_HORIZONTAL);  //180404
        //params.width=GridLayout.LayoutParams.MATCH_PARENT;//180404
        if (ValView!=null) {
            ValView.setLayoutParams(params);
            gridLayout.addView(ValView);
        }
        return ValView;
    }

    private static void mDialogSetup2(String sTitle,  eEditType myEditType) {
//          SETUP the dialog
        if (alert==null)
            alert = new AlertDialog.Builder(mContext());
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
        oUInput.mClose1();
     }



    private static void mLinLayPrep2(){  //Prepare a layout of the UserInput
        mClose(mAlertDialog);           //Close any existing alert dialogs
        if (myLayout !=null) myLayout.removeAllViews();
        myLayout = new LinearLayout(mContext());
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        myLayout.setOrientation(LinearLayout.VERTICAL);
        myLayout.setLayoutParams(parms);

    }

    public void mSaveFile(){
        String sFileName =mGetText(cbSettingsFile)+".txt";
        if (sFileName=="") sFileName=sFile_ProtCfg;
        mPersistAllData(false,sFile_ProtCfg);
        mMessage("Saving "+sFileName,2000);
        mFileSave(sFileName,mPref2Str(""));
        mAlert2(sFileName+"-Saved in Downloads");
    }
    public void mSelectFile(){
        mDialogGridInit1("Settings File");          //Set dialog boxes
        // 180403 todo some command with         BaseActivity.mDispSettings();
        oListMemory = new cOptionBox(mContext(), "Factory;Downloads;Internal",nSettingsDestIdx,false);
        mAddView(mNewLabel( "Location"), oListMemory);
        oListMemory.setListener(new cOptionBox.ChangeListener() {
            @Override
            public void onChange(int nIndex) {
                nSettingsDestIdx = nIndex;
                mSelectFile();
            }
        });
        String[] sNot = new String[]{sFile_AppCfg, cProgram3.sFileName_Help};
        if (nSettingsDestIdx==0)
            cbSettingsFile= mAddCombo("Factory file ", cFunk.mStripFromList(mGetRawFileNames(),sNot), sFile_ProtCfg.replace(".xml",""));
        else if (nSettingsDestIdx==1)
            cbSettingsFile= mAddCombo("Downloads file ", mGetDownloadFileNames(".txt"), sFile_ProtCfg.replace(".txt",""));
        else
            cbSettingsFile= mAddCombo("Settings file ", cFunk.mStripFromList(mGetPrefFileNames(".xml"),sNot), sFile_ProtCfg.replace(".xml",""));
        mLayoutAddCmd2(sFile_ProtCfg,"Save").setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {       //R170627
                mSaveFile();
            }
        });
        if (nSettingsDestIdx==1)
        alert.setNeutralButton( "Save", new DialogInterface.OnClickListener() {     //ToDo 180413: Improve this layout
            public void onClick(DialogInterface dialog, int id) {
                mSaveFile();
            }});;
        alert.setNegativeButton( "Load", new DialogInterface.OnClickListener() {     //ToDo 180413: Improve this layout
            public void onClick(DialogInterface dialog, int id) {
                mSettingsFile_Load(cbSettingsFile,oListMemory);
                cbSettingsFile=null;
                mClose1();
            }});;
        alert.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mClose1( );
            }
        });
        mAlertShow();
    }
    public void mMessage(String msg,int nTimeOut) {      //Popup message
        if (msg!="")        cUInput.sMessages=sMessages+msg+"\n";
        if (nTimeOut>0) {
            mTimeOut(nTimeOut);
        }
        if (Looper.myLooper() != Looper.getMainLooper())
            return;
        if (oFocusdActivity!=mContext())
            return;
        if (alert == null) {
            alert = new AlertDialog.Builder(mContext());
            alert.setPositiveButton("OK", (dialog, which) -> mClose1( ));
            alert.setNegativeButton("Clear", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cProgram3.nMsgVerboseLevel--;
                    cUInput.sMessages="";
                    mClose1();
                }
            });

        }
      //  alert.setTitle(sTitle);

            alert.setMessage(sMessages);
        //Make the dialog
        try {
            if(mAlertDialog==null)
                mAlertDialog = alert.create();
            if (mAlertDialog.isShowing()==false)
                mAlertDialog.show();
        //    mAlertDialog.setTitle(sTitle);
            mAlertDialog.setMessage(sMessages);
        } catch (Exception e) {
            Log.e("log_tag", "Error " + e.toString());
            mClose1();
        }

    }

    private  void mTimeOut(int nTimeOut) {
        cUInput.nClosingTime=cProgram3.mNowMs()+nTimeOut;
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (mAlertDialog!=null)
                    if (nClosingTime<cProgram3.mNowMs())    //Close only on latest timeout
                    mClose1();
            }
        }, nTimeOut);
    }

    public void mSelectFactoryFile(){
        mDialogGridInit("Select a Setup File");          //Set dialog boxes
        String[] s = mGetRawFileNames();
        cbFactorySettingsFile= mAddCombo("File", s,sFile_ProtCfg);
    }
    public void mInputSelectDevice1() {
        mDialogGridInit1("Device status");          //180328
        String sList="";
        for (int i = 0; i < oaProtocols.length; i++) {
            final int i1=i;
            String sDsc = oaProtocols[i].mDeviceNameGet();
            if (oaProtocols[i].getState()==kProtReady)sDsc=oaProtocols[i].mDeviceNameGet()+" - Ready";
            if (oaProtocols[i].getState()!=kProtReady) {sDsc=oaProtocols[i].mDeviceNameGet()+" - Error"; myProtIdx=i;}
            sList = sList + sDsc + ";";
        }
        cOptionBox oOptions = new cOptionBox(mContext(), sList , myProtIdx, false);
        mAddView(mNewLabel("Select"),oOptions);
        mLayoutAddCmd2("Device status & data","Save").setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {       //R170627
                mPersistAllData(false,sFile_ProtCfg);mClose1();
            }
        });
        alert.setNegativeButton( "Connect", new DialogInterface.OnClickListener() {     //ToDo 180413: Improve this layout
            public void onClick(DialogInterface dialog, int id) {
                myProtIdx=oOptions.nSelectIdx;
                mDoConnect(myProtIdx);
                mClose1();
            }});;
        alert.setNeutralButton( "Pair", new DialogInterface.OnClickListener() {     //ToDo 180413: Improve this layout
            public void onClick(DialogInterface dialog, int id) {
                myProtIdx=oOptions.nSelectIdx;
                oaProtocols[myProtIdx].mBT_PickDevice2();
                mClose1();
            }});;
        mAlertShow();
    }
    void mEditControl2_Accept(){
        if (oDD1 != null)  oElemViewProps.mSetElement(oaProtocols[mGetSelIdx(oDD1)].sProtName(),mGetText(oDD2));        oDD1=null;oDD2 = null;
        if (oDD3 != null) mSetElemIdx(mGetSelIdx(oDD3));        oDD3 = null;
        if (oDD4 != null) oElemViewProps.nTypeId =mGetSelIdx(oDD4);oDD4=null;
        if (oVL1 != null)  mElement().nDisplayRange[0] = mStr2Float(mGetText(oVL1), mElement().nDisplayRange[0]);        oVL1=null;
        if (oVL2 != null) mElement().nDisplayRange[1] = mStr2Float(mGetText(oVL2), mElement().nDisplayRange[1]);        oVL2=null;
        if (oCB1 != null) mFocusedViewProps().bEnabled( oCB1.isChecked());        oCB1=null;
        if (oCB2 != null) mFocusedViewProps().bVisible( oCB2.isChecked());        oCB2=null;
        bDoRedraw=true;
    };
    public void mEditControl2() {       //180329 using grid for mInputViewSettings1
        if (oElemViewProps==null) return;
        mDialogGridInit1("Edit "+oCtrlID());          //Set dialog boxes
        oDD1= mAddCombo4("Device", sDevices2, cProtElem.mIndex2ProtName(sProtName()));
        oDD2= mAddCombo4("Element",mElemNameList(), sElemName());
        oDD3= mAddCombo4("Index", mIndexStringArry(mElement()), oElemViewProps.mGetElemIdx());
        if (oElemViewProps.sTypeList()!="")
        oDD4= mAddCombo4("Type", mStr2StrArr(oElemViewProps.sTypeList()), oElemViewProps.nTypeId);
        if (mElement()!=null) {
            oVL1=mAddEdit( "Minimum value", mElement().nDisplayRange[0]);
            oVL2 = mAddEdit("Maximum value", mElement().nDisplayRange[1]);
            oCB1 = mAddCheckBox4("Visible ", mFocusedViewProps().bVisible());
            oCB2 = mAddCheckBox4("Editable", mFocusedViewProps().bEnabled());
            mAddCheckBox4("Set data \n on start", mElement().bWriteOnReset).   //180328e
                setOnClickListener(v -> {
                mElement().bWriteOnReset  =!mElement().bWriteOnReset;
            });
            CheckBox oCB4 = mAddCheckBox4("Limit to sibling", mFocusedViewProps().bLimit2Siblings());   //180328e
            oCB4.setOnClickListener(v -> {
                mFocusedViewProps().bLimit2Siblings(oCB4.isChecked());
            });
        }
        Button cmd1=mCmdButton("color");
        cmd1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {       //R170627
                mStartColorPicker(oElemViewProps);  //Close dialog and open another
            }
        });
        Button cmd2 = mCmdButton("Description");
        cmd2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {       //R170627
                mClose1();
                mEditDescription1();
            }
        });
        this.mAddView(cmd1,cmd2);
        Button cmd3 = mCmdButton("Unit");
        cmd3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {       //R170627
                mClose1();
                mInputProtData2();
            }
        });
        Button cmd4 = mCmdButton("Scale");
        cmd4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {       //R170627
                mClose1();
                mButton_Scaling1 ();
            }
        });
        this.mAddView(cmd3,cmd4);
        alert.setPositiveButton( "Confirm", new DialogInterface.OnClickListener() {     //ToDo 180413: Improve this layout
            public void onClick(DialogInterface dialog, int id) {
                //Get the fields from the editbox
                mEditControl2_Accept();
                mClose1();
            }});
        mAlertShow();
    }

    private String mViewValue(Spinner oDD2) {
        return mGetText(cbElemNr);
    }

    public void mSettingsDialog() {      //
        mDialogGridInit("Settings");          //Set dialog boxes
        obPrivileges= new cOptionBox(mContext(), "User;Advanced;Admin",cProgram3.mPrivileges(),false);
       // obPrivileges.createRadioButton1(mContext(), "User;Advanced;Admin",cProgram3.mPrivileges(),false);
        mAddView(mNewLabel( "Permissions"), obPrivileges);
        if (cProgram3.mPrivileges()>0) {           //Extended settings
            oRefreshRate= mAddEdit("Refresh rate", "" + nRefreshRate());
            mAddCmd3("Protocol:"+sFile_ProtCfg,"Change").setOnClickListener(v -> {
                mClose(mAlertDialog);
                mSelectFile();
            });
            mAddCheckBox4("Design mode", cProgram3.bDesignMode()).setOnClickListener(v -> {
                cProgram3.bDesignMode(!cProgram3.bDesignMode());
                cProgram3.bDoRedraw = true;         //Clicked action
            });
            //if (mPrivileges()>=1)
            mAddCheckBox4("Write Device on reset",cProgram3.bWriteOnReset).setOnClickListener(v -> {
                cProgram3.bWriteOnReset=!cProgram3.bWriteOnReset;
            });
        }
    }
    public   void mInputRange( ) {        //Input a single value,   R170727
        if (oElemViewProps==null)            return;
        if (mElement()==null) return;
        mDialogGridInit(oElemViewProps.mAlias());          //Setup the dialog
        oVL2 = mAddEdit("Maximum value", mElement().nDisplayRange[1]);
        oVL1=mAddEdit( "Minimum value", mElement().nDisplayRange[0]);
        mShowKeyboard1();
    }

    public   void mInputValue1( ) {        //Input a single value,   R170727
        if (oElemViewProps==null)            return;
        if (mElement()==null) return;
        cProtElem myProtData= oElemViewProps.myProtElem1();
        mDialogGridInit1(oElemViewProps.mAlias());          //Setup the dialog
        EditText oTxt;
        if (mElement().mIsInt())
           oTxt= mAddEdit("Value", (int)oElemViewProps.mGetValue());
        else
            oTxt = mAddEdit("Value",  oElemViewProps.mGetValue());
        mShowKeyboard1();
        alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String s=mViewStr(oTxt);
                if (mElement().mIsInt()) {
                    oElemViewProps.mRawValue(mStr2Int(s));
                } else {
                    oElemViewProps.mSetValue(
                            mStr2Float(s, oElemViewProps.mGetValue()));
                }
                mClose1( );
            }
        });
        mAlertShow();
    }

    private void mShowKeyboard1() {
        if (mAlertDialog!=null)
        mAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public void mAccept2(){
        if (obPrivileges !=null) cProgram3.mPrivileges(obPrivileges.nSelectIdx);
        if (oRefreshRate !=null) nRefreshRate( mViewValue(oRefreshRate));
        if (oReturnValue !=null)   {
            float mRetVal = mStr2Float(mViewStr(oReturnValue), oElemViewProps.mGetValue());
            oElemViewProps.mSetValue( mRetVal);

        }
        if (cbFactorySettingsFile!=null){//180412
            String sFileName = mGetText(cbFactorySettingsFile);
            cProgram3.mLoadFactorySettings(sFileName);
            cbFactorySettingsFile=null;
        }
        mEditControl2_Accept();
        cProgram3.bDoSavePersistent=true;
        mClose1();
    }

    private int mGetSelIdx(Spinner CB) {
        return CB.getSelectedItemPosition();
    }

    public void mClose1() {            //180328 clear all variables
        oReturnValue=null;
        oRefreshRate=null;
        obPrivileges=null;
        try {
            if (mAlertDialog!=null) mAlertDialog.dismiss();
            alert=null;
            mAlertDialog=null;
        } catch (Exception e) {
            Log.e("log_tag", "Error " + e.toString());
        }

        oEditType = eEditType.kNull;
        bDoRedraw=true;
        oUInput=new cUInput(mContext());      //180412    Leave clearing variables to GC (ugly, but im to lazy to null all variables)
    }


    private static cOptionBox mOptionBox(String s, String s1, int i, boolean b) {
        cOptionBox ob = new cOptionBox(mContext());
        ob.createRadioButton(s,s1,i,b);
        mAddLine2Layout(ob, null);
        return ob;
    }

    public static void mUserLevel() {       //171222
        mLinLayPrepare("Select user level");
        cOptionBox oUserLvl = new cOptionBox(mContext());
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

//*********RUBBISH*********
public boolean isAlertDialogShowing(AlertDialog thisAlertDialog){
    if(thisAlertDialog != null){
        return thisAlertDialog.isShowing();
    }
    return false;
}
}