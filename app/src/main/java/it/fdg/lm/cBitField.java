package it.fdg.lm;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import static it.fdg.lm.cAndMeth.mSetVisibility;
import static it.fdg.lm.cFunk.mInt2Bool;
import static it.fdg.lm.cFunk.mInt2str;
import static it.fdg.lm.cFunk.mStr2Int;
import static it.fdg.lm.cProgram3.bAdmin;
import static it.fdg.lm.cProgram3.getElementViewById;
import static it.fdg.lm.cProgram3.mAppProps;
import static it.fdg.lm.cProgram3.mAppPropsToggle;

public class cBitField extends BaseActivity {
    private CheckBox[] cb=new CheckBox[32];
    private TextView[] lbl=new TextView[32];
    private static cData_View myDataView;       //Holds the elementprops
    private boolean isBusy=false;
    LinearLayout bitlayout;
    private static final int MENU3 = 1;
    private String myId1="B0";
    private int myIndex=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitfield);
        bitlayout = (LinearLayout)findViewById(R.id.linlay4);
        myDataView= (cData_View) findViewById(R.id.idDataValue2);
        myDataView.mInit(this,myId1);
        mMakeBitFields();               //make the layout
        cProgram3.bDoRedraw=true;
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);//hIDE THE KEYBOARD ON STARTUP OF ACTIVITY
    }
    public  void mInit(int myIndex1) {    //Pepare the controls myDataView,170818 This was needed to make
        //  oaElementViews aware of this control
        myIndex=myIndex1;
        myDataView=new cData_View(this);
        myDataView.mInit(this,myId1);
    }

    @Override
    public void onResume() {
        cGestureListener.mySetParent(myDataView);
        super.onResume();
        cProgram3.oFocusdActivity=this;
     }
    public void onPause() {
        super.onPause();
        //mSettings(false);
    }
    private void mMakeBitFields() {
        Context mContext = this;//getContext();
        if (bitlayout ==null)return;
        LinearLayout layout;
        for(int i = 0 ; i<32 ; i++)
        {

//            if (sBitNames[i]=="")                nBitVisible[i]= 2;     //2:Gone          170628 removed to not be default hidden
            lbl[i] = new TextView(mContext);
            cb[i]=new CheckBox(mContext);
            lbl[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            cb[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            lbl[i].setTag(Integer.valueOf(i));
            cb[i].setTag(Integer.valueOf(i));
            layout = new LinearLayout(mContext);
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setLayoutParams(parms);
            bitlayout.addView(layout);
            lbl[i].setOnClickListener(handleOnClick(lbl[i]));
            cb[i].setOnClickListener(handleOnClick(cb[i]));     //Action on user click
            layout.addView(cb[i]);
            layout.addView(lbl[i]);
        }
    }

    private View.OnClickListener handleOnClick(final View button) {//Helper for setup mMakeBitFields
        return new View.OnClickListener() {
            public void onClick(View v) {

                if (v instanceof CheckBox)
                    mBitSetClick(button);
                else if (v instanceof TextView)
                    if (cProgram3.bDesignMode())
                        mEditBitDesc(mStr2Int(v.getTag().toString()));
            }
        };
    }

    private void mEditBitDesc(int bitnr) {
        cUInput.setFocus(mElemViewProps());
        cUInput.mEditBitDesc(true,bitnr );
    }

    private cElemViewProps mElemViewProps() {
        return myDataView.mElemViewProps();
    }

    void mBitSetClick(View button){
        String msg = button.getTag().toString();
        int myValue=mRawValue();
        if (button instanceof CheckBox){
            Integer i = Integer.valueOf( button.getTag().toString());
            myValue=(myValue|(1<<i));
            if (((CheckBox)button).isChecked()==false)
                myValue=(myValue^(1<<i));   //Clear the bit
        }
        mRawValue(myValue);     //Set the controls value
        mRefresh(false);
    }


    //*******************************  END CLICK MANAGEMENT***********************
    public void mBitListSet(int myValue) {
        if(myValue<0) return;
        for(int i = 0 ; i<32 ; i++)
        {
            cb[i].setChecked(((myValue & (1<<i))!=0));
        }
    }

    private String mBitName(int i) {        //R170727B
        if (mElemViewProps() ==null)
            return "Undef View";
        if (myProtElem()==null)
            return  "Undef prot elem";
        return myProtElem().mBitName(i);
    }

    private int mBitVisible(int i) {        //R170727A
        if (myProtElem()==null)
            return 0;
        else
            return myProtElem().mBitVisible(i);

    }

    public void mRedraw(){
        myId1="B"+(myIndex);
        myDataView.mElemViewProps(getElementViewById(myId1));
        for(int i = 0 ; i<32 ; i++)
        {
            mSetVisibility(cb[i],mBitVisible(i));
            mSetVisibility(lbl[i],mBitVisible(i));
            lbl[i].setText(mInt2str(i)+"_"+mBitName(i));
        }
    }
    public void mRefresh(boolean doRedraw) {
        if (doRedraw)mRedraw();
        if (myProtElem()==null) return;
        if (isBusy) return;
        isBusy=true;
        if (mElemViewProps()!=null)
            mBitListSet(mRawValue());
        if (myDataView!=null) {
            myDataView.mRefresh(doRedraw);
        }
        isBusy=false;
    }

    private int mRawValue() {
        return mElemViewProps().mRawValue();
    }
    private void mRawValue(int myValue) {
        mElemViewProps().mRawValue(myValue);
    }


    private cProtElem myProtElem() {
        if (mElemViewProps()==null) return null;
        return mElemViewProps().oGetElem();
    }


    public boolean mMenuAction_bf(Menu myMenu, int nId) {
        if (mnuClick(nId, "Set Value", cUInput.mSelected())) {
            cUInput.mInputValue(true);
        }
        if (mnuClick(nId,"Mode settings",false)){

        }
        if (bAdmin()) {            ;       //170728    Advanced permissions
            if (mnuClick(nId, "Control:"+ cUInput.oCtrlID(), cUInput.mSelected())) {
                cUInput.mInputViewSettings1(true); //Associate element with widget
            }
            if (mnuCheck(nId, "Design mode",mInt2Bool(mAppProps(cKonst.eSettings.kShowHidden)))) { //Will show all hidden controls
                mAppPropsToggle(cKonst.eSettings.kShowHidden);   cProgram3.bDoRedraw=true;         //Clicked action
            }
            if (mnuClick(nId, "Settings file")) {
                mDispSettings();
            }
        }
        myMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "______________________________________").setEnabled(false);
        if (mnuClick(nId,"User permissions"))          //R170725
            cUInput.mUserLevel(true);
        return false;
    }



}
