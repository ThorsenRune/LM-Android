//Rev 170911
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc: https://docs.google.com/document/d/1bh_dFb6dHkwnqtpGCbxZGD-_I13O3U4J3LiUjvM5ARw/edit
package it.fdg.lm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

public class cSliderHandle {
	private Paint oPaintBorder =new Paint();
	private Paint oPaintHandle= new Paint();//the paint of the handle
	private Bitmap img; // the image
	private int coordX = 0; // the x coordinate at the canvas
	private int coordY = 0; // the y coordinate at the canvas
	private int id; // gives every knob his own id

	private Rect oCanvasArea;
	private cSliderView oParent;
	private int nHandleSize;		//Size of the control handle


	//**************************			INITIALIZATIONS   ***********************
	public cSliderHandle(cSliderView oSliderView) {
//		BitmapFactory.Options opts = new BitmapFactory.Options();
//		opts.inJustDecodeBounds = true;
//		img = BitmapFactory.decodeResource(context.getResources(), drawable);
		mInit(oSliderView);
	}

	public cSliderHandle(Context context, int drawable, Point point) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		img = BitmapFactory.decodeResource(context.getResources(), drawable);
		coordX= point.x;
		coordY = point.y;

	}

	private void mInit(cSliderView oSliderView) {
		//Draw a border
		this.oParent =oSliderView;
		oPaintBorder.setColor(Color.WHITE);
		oPaintBorder.setStyle(Paint.Style.STROKE);
		oPaintBorder.setStrokeWidth(1);
		setColor(Color.BLUE);
	}

	void setX(int newValue) {
		coordX = newValue;
		if (coordX<=oCanvasArea.left)     //Stay within bounds
			coordX=oCanvasArea.left;
		else if (coordX>oCanvasArea.right)
			coordX=oCanvasArea.right;
	}

	public int getX() {
		return coordX;
	}

	void setY(int newValue) {
		coordY = newValue;
	}

	public int getY() {
		return coordY;
	}

	public void setID(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	public Bitmap getBitmap() {
		return img;
	}

	public void mDraw(Canvas canvas, boolean bEnabled) {
		//canvas.drawBitmap(this.getBitmap(), this.getX()-radiusKnob, this.getY()-radiusKnob, null);
		int coordY = getY();
		int coordX= getX();
		if (oParent.bLimit2Range)			//Limit within visible area
			if (coordX> oParent.canvasWidth- oParent.margin)
				setX(oParent.canvasWidth- oParent.margin);           //Limit upper bound
			else if (coordX<0)
				setX(0);                            //Limit lower bound

        //canvas.drawBitmap(this.getBitmap(), this.getX()-radiusKnob, this.getY()-radiusKnob, null);
        if (bEnabled) {
            canvas.drawCircle(coordX,coordY,nHandleSize, oPaintHandle);
            canvas.drawCircle(coordX,coordY,nHandleSize, oPaintBorder);
        }
        else {			//Disabled is a rectangle
            float size = 5;//oPaintHandle.getStrokeWidth();
            canvas.drawRect(coordX-size,coordY-nHandleSize,coordX+size,coordY+nHandleSize,oPaintHandle);
			canvas.drawRect(coordX-size,coordY-nHandleSize,coordX+size,coordY+nHandleSize,oPaintBorder);
        }
    }


    public void mSetArea(Rect oArea) {
		oCanvasArea =oArea;
	}

	public boolean bContains(int x, int y) {
		if (x<(coordX- nHandleSize))    //Out of left
			return false;
		else if ((coordX+ nHandleSize)<x)    //Out of right
			return false;
		else   //// TODO: 07/09/2017  implement y check
			return true;
	}

	public float getValue(float nRes) {             //Return the scaled value
		float value=getX()-oCanvasArea.left;
		 value=nRes*value/oCanvasArea.width();
		return value;
	}

	public void setValue(float nRes, float value) {	//170915 changed to float
		setX((int) ((value*oCanvasArea.width()/nRes)+oCanvasArea.left));

	}

	public void setColor(int color) {
		oPaintHandle.setColor( color);
		oPaintHandle.setStyle(Paint.Style.FILL);
		nHandleSize= oParent.nHandleSize;
	}
}