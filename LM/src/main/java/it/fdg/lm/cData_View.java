//170824
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:https://docs.google.com/document/d/1V5uIFXGwyrThFKnORvRhic2cdrnKbHNrNbsVu16Mg6w/edit
package it.fdg.lm;
//
//Object to View and edit data in element
//

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import static it.fdg.lm.cFunk.mInt2str;
import static it.fdg.lm.cFunk.mStr2Int;


public class cData_View extends LinearLayout {
    private cElemViewProps _ElemViewProps =null;
    //******************    PRIVATE
    private SeekBar editSlider;
    private Button lblElementName;
    public Context mContext;
    private TextView editValue;
    //Not used , reserved for future use
    private String myId1="D";
    private int myIndex1=0;


    public cData_View(Context context) {
        super(context);
        init(null, 0);
    }

    public cData_View(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public cData_View(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        mContext =getContext();
        inflate(getContext(), R.layout.data_view, this);
        editSlider =(SeekBar)this.findViewById(R.id.idseekBar);
        editValue= (TextView) findViewById(R.id.ideditValue1);
        editValue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditValue();
            }});
        lblElementName=(Button) findViewById(R.id.lblElementName);
        lblElementName.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mSelectElement();
            }});
        if (isInEditMode()) return;
    }

    private void mEditValue() {
        cUInput.mSetFocus(_ElemViewProps);
        cProgram3.oUInput.mInputValue1();
    }

    private void mSelectElement() {
        cUInput.mSetFocus(this);       //180103 but it should be handled by the central handler
        cUInput.mInputViewSettings1(true);
    }

    private void mInputHandler(final EditText editValue) {
        editValue.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                //If the keyevent is a key-down event on the "enter" button
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    _ElemViewProps.mRawValue(mStr2Int(editValue.getText().toString()));
                    mParentRedraw();
                    return true;
                }
                return false;
            }
        });

    }

    private void mParentRedraw() {      //Should take care of also refreshing this

        //editValue.clearFocus();
    }
    public void mRefresh(boolean bRedraw) {
        if (bRedraw) mRedraw();
        if (_ElemViewProps ==null) {
            cProgram3.mErrMsg("Empty element failure 170621");
            return;
        }
        if (editValue==null) return;
        String s = mInt2str(_ElemViewProps.mRawValue());
        editValue.setText(s);
        editValue.invalidate();
    }       //Refresh the control (or full redraw)

    private void mRedraw() {
        _ElemViewProps.mUpdate(this);
        if (_ElemViewProps.bVisible()) {
            lblElementName.setText(_ElemViewProps.mGetName());
        }
    }           //Draw whats not refreshed

    public void mInit(Context oOwner, String sId) {
        mContext =oOwner;

    }

    public int mRawValue() {
        return _ElemViewProps.mRawValue();
    }

    public void mRawValue(int myValue) {
        _ElemViewProps.mRawValue(myValue);
    }

//  Getter setters
    public cElemViewProps mElemViewProps() {
        return _ElemViewProps;
    }

    public void mElemViewProps(cElemViewProps oNewElemViewProps) {
        _ElemViewProps =oNewElemViewProps;
    }
    //          SYSTEM METHODS
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    protected void onFinishInflate() {
        editSlider.setVisibility(GONE);      //Hide the layout
        super.onFinishInflate();
    }   //!? Sorry,I still dont understand when this is called

    public cElemViewProps oElemViewProps() {
            return _ElemViewProps;
    }
}

