//Rev 170919
//Doc: https://docs.google.com/document/d/16kXpTxGW7bW9uiBzxPqRSLVksmpT2Yp3Qs5-66ZoiFA/edit
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8

//  Multihandle sliders
// use mGetAllValues,mSetAllValues to exchange values (with pixel resolution)
/*  event handler
    slider = (SliderView)findViewById(R.id.idslider);
	slider.setHandleValueChangeListener(new iHandleValueChangeListener() {		//Return an array of the handle values
			@Override
			public void onValuesChanged(int[] Vals) {
				if (Vals.nDataLength==2)
					text_interval.setText(Vals[0] + " - " + Vals[1]);
				else if (Vals.nDataLength==3)
					text_interval.setText(Vals[0] + " - " + Vals[1]+ " - " + Vals[2]);
				else if (Vals.nDataLength==4)
					text_interval.setText(Vals[0] + " - " + Vals[1]+ " - " + Vals[2]+ " - " + Vals[3]);
			}
		});

 */

package it.fdg.lm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.WHITE;
import static it.fdg.lm.cProgram3.nSliderSize;
import static it.fdg.lm.cProgram3.oGraphText;

public class cSliderView extends View {
    static cSliderView oFocusedSlider;          //Currently focused slider in the collection
    static cSliderHandle oFocusedHandle;        //Currently focused handle
    public float nScaleMax =100;

    static int margin=2;
    protected iHandleValueChangeListener iHandleValueChangeListener;
    private int kControls=4;        //Maximum controls
    public cSliderHandle[] oHandle = new cSliderHandle[kControls]; // array that holds the oHandle
    private boolean bInitialized =false;
    private Paint paintBorder=new Paint();

    private Paint mTickMarks =new Paint();
    private Paint oPaintInterval=new Paint();
    private Rect oRect[]=new Rect[kControls+1];       //rectangle for the intervals
    private Rect oArea=new Rect();                      //Area of the slider body, Text outside
    private int oBar[]=new int[4];                   //180416  Area of the bar
    public int nActiveIdx =0;            //Currently selected vertex/dragpoint
    private int[] mColorHandle= {Color.YELLOW, Color.MAGENTA, Color.LTGRAY, Color.GREEN};
    private int[] mColorInterval= {Color.YELLOW, Color.MAGENTA, Color.LTGRAY, Color.GREEN};
    private boolean bCanDrag=false;
    protected boolean bRotate=false;
    private boolean bEnabled[];                                     //true in designmode
    private int nType[];                                     //180327 type of handle Diamond,rect,up,down
    private boolean bVisible[];                                     //170914  true in designmode


//      Descriptive texts
    private String sValueText[] = {"sValueText ","2","3","4"};             //String of current value
    public String[] sDescription ={"Description1","","",""};                    //A text shown next to slider
    private int canvasHeight;
    public int canvasWidth;
    public boolean bChangedByUser =false;
    public cSlider oParent;             //The owner of this widget

    /*_____________________________Public properties____________________________*/
    public boolean bDesignMode=true;        //170914 Will show and enable all handles, to allow interactive design.
    public boolean bLimit2Range=true;      //170914 Limit datainput to the sliders range
    private boolean bLimit2Siblings[];          //Confine values between siblings
    public int nHandleSize=0;                   //Handlesize Set in mSetSizes
    private int nTextHeight=0;                  //Label height Set in mSetSizes
    private int nTickTextHeight=0;              //Height of the tick texts set in mSetSizes
    private int nTickMarkCount = 5;             //Number of tickmarks on the slider


    public cSliderView(Context context) {
        super(context);
        setFocusable(true);
        mSetHandles();
    }

    public cSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        mSetHandles();
    }

    public cSliderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);
        mSetHandles();
    }

    //Get/set values of the handles


    public float[] mGetAllValues() {
        float[] a= new float[kControls];
        for (int i=0;i<kControls;i++){
            a[i]= oHandle[i].getValue(nScaleMax);
        }
        return a;
     }

    public void mSetAllValues(float[] Vals) {
        int[] a=new int[kControls];
        for (int i=0;i<kControls;i++){
            oHandle[i].setValue(nScaleMax,Vals[i]);
        }
        invalidate();       //170912    Redraw the slider
    }
    //To rotate the slider set the rotation
    // number of handles can be set by using the android:tag="#"
    private void mSetHandles() {          //Emulator initialization
        if (oGraphText==null) oGraphText=new cGraphText(this.getContext());
        if (isInEditMode()) cGraphText.mInit(20);
        if (this.getTag()!=null) {      //Set number of handles
            String s = this.getTag().toString();
            try {
                kControls= Integer.parseInt(s);       //Number of controls

            } catch(NumberFormatException nfe) {
                System.out.println("Could not parse " + nfe);
            }
        }
         mSetHandles(kControls);
        paintBorder.setStyle(Paint.Style.STROKE);
    }

    public void mSetHandles(int nNewNoOfHandles) {      //Initialize and set number of handles
        kControls=nNewNoOfHandles;
		bEnabled=new boolean[nNewNoOfHandles];		//170913 booleans determining user input capability
        nType=new int[nNewNoOfHandles];             //180327    Type of handle
		bLimit2Siblings=new boolean[nNewNoOfHandles];//170915 Libit to be within siblings
        bVisible=new boolean[nNewNoOfHandles];		//170913 booleans determining user input capability
        bInitialized = false;      //Will redimension the controls asap
        for (int i = 0; i< kControls; i++) {
            if (oHandle[i] == null) oHandle[i] = new cSliderHandle(this );
        }
 //       mSetAllValues(nValues);
    }

    private void mGetCanvasProportions() {
        if (getMeasuredHeight()>getMeasuredWidth()) bRotate=true;
        if (bRotate){                   //Swap height and width
            canvasWidth =getMeasuredHeight();
            canvasHeight =getMeasuredWidth();
        } else {
            canvasHeight =getMeasuredHeight();
            canvasWidth =getMeasuredWidth();
        }
        mSetSizes();
   }

    private void mSetSizes() {
        //Scaling the slider according to nTextSize
        nTextHeight= (int) (cKonst.nTextSize* nSliderSize);
        margin= (int) (nTextHeight*0.75);
        nTickTextHeight= (int) (nTextHeight*0.75);
        ViewGroup.LayoutParams p = getLayoutParams();
        nHandleSize = (int) (canvasHeight/2);//size of the handle
//        nHandleSize=abs(oBar[3]-oBar[1]);                    //Width of the handle=bar height

        if (p!=null) {
            if (canvasHeight < 5 * nTextHeight) {
                canvasHeight = 5 * nTextHeight;
                if (bRotate)
                    p.width = canvasHeight;
                else
                    p.height = canvasHeight;
                this.setLayoutParams(p);
            }
        }
    }

    private void mSetMargin(View v, int ml) {
        //Set margin runtime , thanks to SO https://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (bRotate)    //Vertical left margin
                p.setMargins(ml, p.topMargin, p.rightMargin, 0);
            else        //Horizontal, bottom space
                p.setMargins(0, p.topMargin, p.rightMargin,ml);
            v.requestLayout();
        }
    }
    //***************           EVENTS    *******************
    @Override   //This will be called when the view is layed out |?
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(!bInitialized)    mRedraw();        //initialise data for oHandle , slider
        bInitialized =false;        //Request a redraw of the slider
        //mSetMargin(this,100);       //170915 experimented with where to put, found onMeasure is ok
    }
    @Override
    protected void onDraw(Canvas canvas) {//      Draw update the slider
        mRefresh(canvas);
    }       //END OF DRAW ----------------------------------------------------


    private void mDrawTexts(Canvas canvas) {       //Rewritten 170824 to place text by position parameter
        //  if (oTextPaint == null) return;          //In case this code is called before instancing
        cGraphText.oTextPaint.setTextSize(nTextHeight);
        if (sValueText[nActiveIdx] == "") return;       //If no text return
        if (2* oHandle[nActiveIdx].getX() > canvasWidth)      //Place the value according to handle
            cGraphText.mTextDraw(sValueText[nActiveIdx], 7, canvas);
        else
            cGraphText.mTextDraw(sValueText[nActiveIdx], 9, canvas);
        if (sDescription[nActiveIdx]!="") {
            cGraphText.oTextPaint.setTextSize((float) (nTextHeight));
            cGraphText.mTextDraw(sDescription[nActiveIdx],3,canvas);
        }
    }

    public float nActiveValue() {
        float val = oHandle[nActiveIdx].getValue(nScaleMax);
        return val;
    }


    private int mY2C(int y){  //Transform y rotated views coordinates
        return canvasHeight -y;
    }
    void mDrawMidline(Canvas canvas){
        //Midline
//        paintBorder.setColor(RED);
//        canvas.drawRect(margin,mY2C(canvasHeight/2+1) ,oArea.right,mY2C(canvasHeight/2-1),paintBorder);
        int y = (int) (0.5*(oBar[1]+oBar[3]));  //Centerline of bar
        canvas.drawRect(oBar[0],y-1,oBar[2],y+1,paintBorder);

        canvas.drawRect(1,0 ,canvasWidth-1,canvasHeight-1,paintBorder); //Frame the control

    }
    private void mBorderTheArea(Canvas canvas) {    //Make a border around slider area
        paintBorder.setStrokeWidth(1);
        paintBorder.setColor(GREEN);
        paintBorder.setStyle(Paint.Style.STROKE);
        canvas.drawRect(margin,oBar[3] ,oArea.right,oBar[1],paintBorder);
        paintBorder.setColor(WHITE);
        canvas.drawRect(1,mY2C(canvasHeight) ,canvasWidth,mY2C(1),paintBorder);
        canvas.drawLine(0,0 ,canvasWidth,canvasHeight,paintBorder);
    }

    public void mRedraw() { //initialise data for oHandle , slider
        if (!isInEditMode()) {
            if ((isLaidOut() == false) | (getMeasuredHeight() < 1)) return;
        }
        if (isInEditMode()) cGraphText.mInit(20);
         bInitialized = true;
        mGetCanvasProportions();
               //Area of the slider x1,y2,x2,y1   (Yes y2 is top,y1 bottom Rect is a mess)
        oArea.set(3*margin, 2 * margin, canvasWidth - margin, canvasHeight - 2 * margin); //Left, top margin,right,bottom
        //Get number of thumbs Allocate objects  Initialize the intervals
        mTickMarks = mNewPaint(Color.WHITE);//the paint for the slider data(the values)
        mTickMarks.setAntiAlias(true);
        mTickMarks.setTextAlign(Paint.Align.CENTER);
        mTickMarks.setTextSize(nTickTextHeight );
        oBar[0]=margin;                             //Left start of bar area
        oBar[1]=mY2C(nTextHeight);                  //Value text below
        oBar[2]=canvasWidth-  margin;                 //Right endpoint of bar area
        oBar[3]=mY2C((int) (canvasHeight-2*nTextHeight-nTickTextHeight*0.75));       //Tick text and Description text above
        //rectangles that define the line between and outside of knob
        for (int i=0;i<mColorInterval.length;i++) { //From base to first
            oRect[i]= new Rect();
            oRect[i].left=canvasWidth-margin;
            oRect[i].left=margin;
            oRect[i].bottom=oBar[1];
            oRect[i].top=oBar[3];
            sValueText[0]="CH "+canvasHeight;
        }
        oArea.set(oBar[0],oBar[3],oBar[2],oBar[1]);
        int i;
        for (i = 0; i< kControls; i++) {
            //!-    oHandle[i] = new cSliderHandle(getContext(), R.drawable.knob);
            oHandle[i].setID(i);
            oHandle[i].mSetArea(oArea);
            //oHandle[i].setY(oArea.centerY());
            oHandle[i].setY((int) (0.5*(oBar[1]+oBar[3])));

            oHandle[i].setColor(mColorHandle[i]);
            oHandle[i].nShape=nType[i];
        }
        eventValueChanged(mGetAllValues());    //Notify owner that the values are ready
        invalidate();       //Needed for preview
    }

    private void mDrawTickMarks(Canvas canvas) {
        for(int i=0;i<=nTickMarkCount;i++) {
            float nX = nScaleMax * i / nTickMarkCount;
            //int xmark =  (margin+(i*(oArea.width() )/divs));
            int xmark =  oBar[0]+(i*(oBar[2]-oBar[0] )/nTickMarkCount);
            //Tickmarks
            canvas.drawLine((float)(xmark), oBar[3] -nTickTextHeight,(float)(xmark), oBar[3] + nTickTextHeight, mTickMarks);
            mTickMarks.setTextSize(nTickTextHeight );
            canvas.drawText(nX+"", (float)xmark,
                    oBar[3]-nTickTextHeight, mTickMarks);
            //Tickmarks
            //canvas.drawLine((float)(xmark), py,                    (float)(xmark), 2*py, mTickMarks);
        }

    }


    private Paint mNewPaint(int color) {
        Paint oNewPaint = new Paint();//the paint between oHandle
        oNewPaint.setColor(color);
        return oNewPaint;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();
        boolean ret=false;          //Return true  if event was captured
        int X = (int)event.getX();
        int Y = (int)event.getY();
        if (bRotate){
            Y = (int)event.getX();
            X = (int) (getHeight()-event.getY());
        }

        switch (eventaction) {
            //Touch down to check if the finger is on a knob
            case MotionEvent.ACTION_DOWN:
                // get next available draggable  check if inside the bounds of the knob(circle)
                ret = mActivateNext(X, Y);
                // get the centre of the knob
                ret= true;
                break;
            //Touch drag with the knob
            case MotionEvent.ACTION_MOVE:
                if ((bCanDrag)&(0<= nActiveIdx)){
                    X= mSiblingLimited2(nActiveIdx,X);
               //     if ( mUIAction_CanMoveTo(X)){
                        oHandle[nActiveIdx].setX(X);
                        eventValueChanged(mGetAllValues());
                        bChangedByUser =true;
                //    }
                }
                ret=true;
                break;
            // Touch drop - actions after knob is released are performed
            case MotionEvent.ACTION_UP:
                bCanDrag=false;
                ret=true;
                break;
            default:
                ret=false;
        }
        // Redraw the canvas
        if (ret)            invalidate();
        return ret;
    }

    private int mSiblingLimited2(int nActiveIdx, int nValue) {
        if (bLimit2Siblings[nActiveIdx]) {   //Limit between siblibgs
            if (0 < nActiveIdx){
                if (bLimit2Siblings[nActiveIdx - 1])
                    if (oHandle[nActiveIdx - 1].getX() > nValue){
                        return oHandle[nActiveIdx - 1].getX();  //Cant go past previous
                    }
            }
            if (nActiveIdx < kControls - 1)
                if (bLimit2Siblings[nActiveIdx + 1])
                    if (oHandle[nActiveIdx + 1].getX() < nValue){
                        return oHandle[nActiveIdx + 1].getX();   //Cant go past next
                }
        }
        return nValue;
    }

    private boolean mUIAction_CanMoveTo(int nValue) {     //Move the value from touch event
        if (bLimit2Siblings[nActiveIdx]) {   //Limit between siblibgs
            if (0 < nActiveIdx){
                if (bLimit2Siblings[nActiveIdx - 1])
                if (oHandle[nActiveIdx - 1].getX() > nValue){
   //                nActiveIdx=nActiveIdx - 1;
                    return false;  //Cant go past previous
                }
            }
            if (nActiveIdx < kControls - 1)
                if (bLimit2Siblings[nActiveIdx + 1])
                if (oHandle[nActiveIdx + 1].getX() < nValue){
 //                   nActiveIdx=                    nActiveIdx + 1; // Make focus on offender
                    return false;  //Cant go past next
                }
        }
        return true ;
    }
//  Select the next control in the clicked area
    private boolean mActivateNext(int x, int y) {  //Go to next available control (kw: select next)
        int idx=nActiveIdx;
        for (int i=0;i<kControls;i++) 		{
            idx = (idx + 1) % (kControls);
            if (oHandle[idx].bContains(x, y)) {
                bCanDrag = true;
                if (canFocus(idx))            //180420 unsure if we want this
                {        //170914  Only enabled controls can be activated
                    nActiveIdx = idx;
                    oFocusedSlider=this;
                    oFocusedHandle= oHandle[idx];
                    return true;
                }
            }
        }
        bCanDrag=false;
        return false;
    }

    private boolean canFocus(int idx) {     //Returns true if the control can get focus
        return bEnabled[idx]|bDesignMode;
    }

    public void   mSetHandleColor(int nHandleIdx, int nColor,int nShape) {
        mColorHandle[nHandleIdx]=nColor;
        nType[nHandleIdx]=nShape;
    }

    public void mSetDescription(int i, String s) {
        sDescription[i]=s;
    }

    public void mValueInPx(int i, float v) {
        if (oHandle[i]==null) return;       //170912 Avoiding null exceptions
        oHandle[i].setValue(nScaleMax,v);
        invalidate();
    }

    void mValueText(int i, String s) {      //Todo 180417 there may be a problem with unassigned value idx 4 being assign to all_flags
        sValueText[i]=s;
    }

    void mRefresh(Canvas oCanvas) {
        if (oCanvas==null) return;
        if(getWidth()<1) return;
        //background for slider
        if(!bInitialized)            mRedraw();        //initialise data for oHandle , slider
        if (bRotate) {
            oCanvas.save(Canvas.MATRIX_SAVE_FLAG); //Saving the canvas and later restoring it so only this image will be rotated.
            oCanvas.rotate(-90);
            oCanvas.translate(-getHeight(), 0);
        }
        mDrawMidline(oCanvas);
//Draw  intervals as rectangle
        int startX = oArea.left;      //First interval
        for ( int i = 0; i< kControls; i++) {
            oRect[i].left = startX;
            oRect[i].right = oHandle[i].getX();
            startX = oHandle[i].getX();
            oPaintInterval.setColor(mColorInterval[i]);
            if (bVisible[i]|bDesignMode)
                oCanvas.drawRect(oRect[i], oPaintInterval);
        }
//Place thumbs
        for (int i = 0; i< kControls; i++) {
            if (bVisible[i]|bDesignMode)
                oHandle[i].mDraw(oCanvas);
        }
        mDrawTickMarks(oCanvas);
        mDrawTexts(oCanvas);
         //mBorderTheArea(oCanvas);     //For debugging
        if (bRotate)oCanvas.restore();
    }
	public void mEnabled(int idx,boolean ena){		//Set user input enabled
		bEnabled[idx]=ena;

	}
    public void mLimit2Siblings(int idx,boolean ena){		//Set user input enabled
         bLimit2Siblings[idx]=ena;           //Limit enabled values to sibling values
    }
    public void mSetIntervalColor(int nHandleIdx, int nColor) {
        mColorInterval[nHandleIdx]=nColor;
    }

    public void mVisible(int i, boolean b) {
        bVisible[i]=b;
    }

    /**
     * Interface which defines the knob values changed listener method
     */
    interface iHandleValueChangeListener {
        void onValuesChanged(
                float[] Vals);
    }

    /**
     * Method applied to the instance of SliderView
     */
    public void setHandleValueChangeListener(iHandleValueChangeListener l) {
        iHandleValueChangeListener = l;
    }

    /**
     * Method used by handle values changed listener
     * param Vals
     */
    private void eventValueChanged(float[] Vals) {

        if(iHandleValueChangeListener != null)
            iHandleValueChangeListener.onValuesChanged(Vals);
    }
}
