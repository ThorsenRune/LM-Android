package it.fdg.lm;

import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;

import static it.fdg.lm.cProgram3.mContext;


/**
 * Created by rthorsen on 03/10/2017.
 */

public class cGraphText extends Application {       //This should handle text graphics
    private static float margin=10;
    public static Paint oTextPaint=new TextPaint();
    private static int canvasHeight;
    private static int canvasWidth;
    public static int nBackColor;
    private static boolean bRotated;
    private static int nTextSize=30;
    private static cGraphText ob;

    public cGraphText(Context context) {
        mContext=context;
    }

    public void getContext() {
        mContext=getApplicationContext();
    }
    public cGraphText() {
        if (mContext==null)        mContext=getApplicationContext();
        mInit(nTextSize);
    }

    public static void mInit(int newTextSize) {
        if (mContext!=null)
            nBackColor = ContextCompat.getColor(mContext,R.color.colorBackground);
        nTextSize= newTextSize;
        oTextPaint.setTextSize(nTextSize);
    }


    private static int mY2C(int y){  //Transform y rotated views coordinates
        return canvasHeight -y;
    }
    public static void mTextDraw(String sString, int pos, Canvas canvas) {  //170914  revised with bottom alignmnet and right
        canvasHeight=canvas.getHeight();
        canvasWidth =canvas.getWidth();
        bRotated=(canvasHeight>canvasWidth);
        if (canvasHeight>canvasWidth){
            canvasHeight=canvas.getWidth();
            canvasWidth =canvas.getHeight();
        }
        //  placement   1,2,3 top left,mid,right. 4,5,6  mid left mid right 7,8,9   bottom-left,mid,right
        int kLabelColor= Color.WHITE;        //Color of the label on the slider
        Rect textBounds=new Rect();
        oTextPaint.getTextBounds(sString, 0, sString.length(), textBounds);  // Get size of sValueText.
        float ypos =0;
        float xpos = 0;
        if ((pos==1)|(pos==4)|(pos==7))     //Left positions
            xpos=margin;
        if ((pos==3)|(pos==6)|(pos==9))     //Right positions
            xpos = canvasWidth - textBounds.width() - margin;
        if ((pos==1)|(pos==2)|(pos==3))     //Top positions
            ypos =textBounds.height();//Correct ypos for the height of the text
        if ((pos==7)|(pos==8)|(pos==9))     //Bottom positions
            ypos =canvasHeight;
        oTextPaint.setColor(nBackColor);    //Draw a background for the text
        canvas.drawRect(xpos,ypos - textBounds.height(), xpos + textBounds.width()+2, ypos+1, oTextPaint);
        oTextPaint.setColor(kLabelColor);
        canvas.drawText(sString, xpos, ypos, oTextPaint);
    }
}
