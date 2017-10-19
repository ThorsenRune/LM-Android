//170913   is last Revision  Android V4
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//doc:  https://docs.google.com/document/d/1a7LDR804A8fhAB9RbTbRRpTVu9y1ZQHBLW6qoogVvn8/edit
/* Widget for showing an element as a signal
//path:C:\work\test\1Prototyping\app\src\main\java\it\dongnocchi\myapplication\

*/

package it.fdg.lm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import static it.fdg.lm.cAndMeth.mIsHidden;
import static it.fdg.lm.cFunk.mLimit;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.mPalIdx2Col;
import static it.fdg.lm.cProgram3.mySignal;
import static it.fdg.lm.cProgram3.nSignalPage;
import static it.fdg.lm.cProgram3.oSlider;
import static java.lang.Math.sin;

public class cSignalView2 extends View {
    private String myId1="S1";                          //Identifier of this panel
    private cElemViewProps oElemViewProps =null;       //Visual properties of this control
    //***       LOCAL USE
    private int nCanvasWidth=1;                 //Values can't be zero, avoid null division
    private int nCanvasHeight=1;
    private Path mPath;
    private Paint mPaint;
    public float nScaleFactor = 1.0f;//2.1
    private float scalePointX, scalePointY;
    private float mLastTouchX, mLastTouchY;

    private float mX;
    private float mY;
    private static final float TOLERANCE = 5;
    float pY, vY,cX, cY,mPosX, mPosY;
    Bitmap mBitmap;
    Canvas mCanvas;
    private cProtElem _oElement;
    private int myIndex=1;              //Identity index id=S+myIndex

    public cSignalView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public void mInit(int myIndex1) {
         myIndex=myIndex1;
    }

    private void init(final Context context) {
        //gestureDetector = new GestureDetector(context, new cGestureListener());

        // we set a new Path
        mPath = new Path();
        // and we set a new Paint with the desired attributes
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(4f);
       // mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // your Canvas will draw onto the defined Bitmap
        if ((h>0 )&(w>0)) {     //To avoid crash on rotation
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            if ((mCanvas.getHeight()>0)&(mCanvas.getWidth()>0 )) {
                nCanvasWidth = mCanvas.getWidth();
                nCanvasHeight = mCanvas.getHeight();
            }
        }
        if (isInEditMode())            mSimulateSignal(mCanvas);    //170913 Placeholder signal
    }

    // override onDraw
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // draw the mPath with the mPaint on the canvas when onDraw
//        canvas.translate(0,canvas.getHeight());   // reset where 0,0 is located
//        canvas.scale( oGetElem.aData.nDataLength,-1);
//        canvas.scale(1,-1,nCanvasWidth/2,nCanvasHeight/2);
        //Draw the signal
        canvas.drawPath(mPath, mPaint);
        //R170822       Draw the alias on the signal pane
        if (oElemViewProps==null)   //is element valid
            return;
        //Draw alias on top left corner
        cSliderView oCanvas = oSlider[0].oSliderView;
        cGraphText.mTextDraw(oElemViewProps.mAlias(),1,canvas);
        cGraphText.mTextDraw("Max:" +oElemViewProps.mScaleMaxStr(),3,canvas);
        //cGraphText.nBackColor=cAndMeth.mGetBackgroundColor(this);
        cGraphText.mTextDraw("Min:" +oElemViewProps.mScaleMinStr(),9,canvas);
    }

    private void mSimulateSignal(Canvas canvas) {       //Draw a sine placeholder for design view
        mPaint.setColor(Color.WHITE);
        mPath.reset();          //Clear existing drawing
        for (int i=0;i<nCanvasWidth;i++){
            float x=i;
            mPath.lineTo(x, (float) (nCanvasHeight/2+nCanvasHeight*sin(x/50)/2));
        }
    }
    private int mDisplayX2SignalX(float x){
        return (int) (_oElement.nDataLength()*x/nCanvasWidth);
    }
    //......................implementation
    public int mRefreshSignal(boolean doRedraw){
        float nX;
        if (doRedraw) mRedraw();
        if (mIsHidden(this)) return 0;
        _oElement     = oElemViewProps.oGetElem();
        if (_oElement==null) return 0;            //No signal selected
        //Make the object drawing the signal
        int nLen = _oElement.nDataLength();
        for (int i=0;i<nLen;i++){
            vY = _oElement.getVal(i);    //value in units
            nX=(float)nCanvasWidth*i/nLen;
            pY= nCanvasHeight-oElemViewProps.unit2dispVal(vY,nCanvasHeight);
            if (i==0) {
                mPath.reset();          //Clear existing drawing
                mPath.moveTo(nX, pY);
            }
            else {
                mPath.lineTo(nX, pY);
            }
        }
        // mCanvas.drawPath(mPath, mPaint);
        invalidate();
        return 3;
    }

    private void mRedraw() {
        myId1="WS"+myIndex+"_"+nSignalPage;
        oElemViewProps= cProgram3.getElementViewById(myId1);        //R170727
        if ((oElemViewProps !=null)&(mPaint!=null)) {
            oElemViewProps.mUpdate(this);
            //Set the visibility of the container to the visibility of this
            ((LinearLayout)this.getParent()).setVisibility(this.getVisibility());
            if (mIsHidden(this)) {
                nSignalPage = 0;
                return;
            }
            mPaint.setColor(mPalIdx2Col(oElemViewProps.nColorGet1(0)));
            this.setBackgroundColor(mPalIdx2Col(oElemViewProps.nColorGet1((byte) 1)));
            this.setBackgroundColor(Color.BLUE);
        }
    }



    public void mShowCoord(float x, float y) //Show the coordinate on the signal panel
        {    mMessage("("+mDisplayX2SignalX(x)+":"+oElemViewProps.mVal2Str(y));
    }

    public cElemViewProps oElemViewProps() {
        return oElemViewProps;
    }

    public void mShiftPane(int nPageChange) {      //Change pane by changing the ID back or forth
        nSignalPage=mLimit(0,nSignalPage+nPageChange,1);
        cUInput.setFocus(this);
        cProgram3.bDoRedraw=true;
        mMessage("Panel "+nSignalPage);
    }

    public void mShow(boolean b) {
        if (_oElement==null) mShiftPane(-1);    //Back up
        if (b)
           mySignal.oElemViewProps.bVisible(true);
        else
            mySignal.oElemViewProps.bVisible(false);
        cProgram3.bDoRedraw=true;
    }

}

