package it.fdg.lm;
//Widget for making an optionbox
/*
Use example:
        LinearLayout ll = (LinearLayout) txtTest.getParent();
        cOptionBox v = new cOptionBox(this, null);
        ll.addView(v);
        v.createRadioButton("Title", "1;2;3").setListener(new cOptionBox.ChangeListener() {
            @Override
            public void onChange(int nIndex) {
                mShowResult(nIndex);
            }
        });
                v.mSetHorizontal(true   );
 */
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


public class cOptionBox extends LinearLayout {
    private ChangeListener listener;
    public int nSelectIdx = -1;
    private Button btnDisplay;
    private cOptionBox oCV;
    private LinearLayout myLayout;
    private RadioGroup myGroup;
    private TextView lblTitle;

    public cOptionBox(Context context) {
        super(context);
        init();
    }

    // Setup views
    private void init() {
        myLayout =(LinearLayout) getRootView();
        if (btnDisplay!=null) {
            btnDisplay.setText("Text set in init");
        //    oCV = (cOptionBox) findViewById(R.id.idOptionBox);
            if (oCV == null){
                oCV = (cOptionBox) this.getRootView();
            }
            oCV.createRadioButton("Options", "O0;O1;O2;O3");       //Demotext
        }
    }

    public cOptionBox createRadioButton(String sTitle, String sList) {
        myLayout.removeAllViews();
        String[] sLbl = sList.split(";");
        RadioButton[] rb = new RadioButton[sLbl.length];
        Context ctx=getContext();
        if (myLayout==null) return null;
        lblTitle=new TextView(ctx);
        lblTitle.setText(sTitle);
        myLayout.addView(lblTitle);//you add the whole RadioGroup to the layout
        myGroup = new RadioGroup(ctx); //create the RadioGroup
        for(int i=0; i<sLbl.length; i++){
            rb[i]  = new RadioButton(ctx);
            rb[i].setText(sLbl[i]);
            rb[i].setId(i + 100);
            if (i==nSelectIdx) rb[i].setChecked(true);
            myGroup.addView(rb[i]);
        }        ;
        myLayout.addView(myGroup);//you add the whole RadioGroup to the layout
        myGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup oGroup, int i) {
                int idx = -1;   //Nothin selected
                int selectedId = oGroup.getCheckedRadioButtonId();
                if(selectedId>0) {
                    RadioButton rb= (RadioButton) oGroup.findViewById(selectedId);
                    idx = oGroup.indexOfChild(rb);
                }
                cOptionBox.this.mIndex(idx);
            }
        });
        return this;
    }

    public void mSetHorizontal(boolean b) {
        if (b)
            myGroup.setOrientation(RadioGroup.HORIZONTAL);
        else {
            myGroup.setOrientation(RadioGroup.VERTICAL);
        }
    }

    public void createRadioButton(String sTitle, String sOptionsList, int iSelectedIdx,boolean bHorizontal) {   //Ver 171222
        mIndex(iSelectedIdx);
        createRadioButton(sTitle,sOptionsList);
        mSetHorizontal(bHorizontal);
    }


    public interface ChangeListener {
        void onChange(int nIndex);
    }
    public int mGetCheckedIndex() {
        return nSelectIdx;
    }

    public void mIndex(int nSelectIdx1) {
        nSelectIdx = nSelectIdx1;
        if (listener != null)
            listener.onChange(nSelectIdx);
    }

    public ChangeListener getListener() {
        return listener;
    }

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }
}
