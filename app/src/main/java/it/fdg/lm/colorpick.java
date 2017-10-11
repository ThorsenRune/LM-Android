//170914    Change in color indexing
//170825
// 170824
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1eO9ciGwQySJq00XIfCDw69b5EdvT8xRBdiAVtw8dbyI/edit

//  A Colour picker for the controls
package it.fdg.lm;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import static it.fdg.lm.cFunk.mStr2Int;

public class colorpick extends BaseActivity {
    private boolean bBackColor=false;
    TextView txtTest;
    private SeekBar seekBarFontCol;
    private SeekBar seekBarBackCol;
    private cElemViewProps vp;
    private ViewGroup parentlayout;
    private SeekBar seekBarFont;
    private boolean bIsdirty=false;
    private int currColor;

    private SeekBar sliderBackColor;
    private Button[] oButtons;
    private Button oOkButton;
    private int[] nSelCol={0,0};
    private int[] nViewDims ={50,20};
    private boolean isBlockedScrollView=false;
    private Button[] oBtnColor=new Button[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.colorpicker);
        txtTest=(TextView) findViewById(R.id.idtextview6);
        oOkButton=(Button)findViewById(R.id.idOkButton87);
        oBtnColor[0]=(Button)findViewById(R.id.idcol1);
        oBtnColor[1]=(Button)findViewById(R.id.idcol2);
        oOkButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mAccept();// click handling code
            }
        });

        sliderBackColor = (SeekBar)findViewById(R.id.seekbar_back);
        sliderBackColor.setMax(256*7-1);
        sliderBackColor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    bIsdirty=true;
                    currColor=mColor(progress);
                    oBtnColor[0].setBackgroundColor(currColor);
                }
                else {
                    mSetGradient();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {        }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {        }

        });
           mPopulate();
        vp= cUInput.mGetViewProps();
        ViewTreeObserver vtObserver = sliderBackColor.getViewTreeObserver();
        vtObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mSetGradient();
                // check if the view's height and/or width > 0
                // don't forget to remove the listener when done using removeOnGlobalLayoutListener
            }
        });

        mSetCols(vp.nColorGet1((byte) 0),     vp.nColorGet1((byte) 1));
           final Handler handler = new Handler();
           handler.postDelayed(new Runnable() {
               @Override
               public void run() {
                   mLayout_Reflow(parentlayout);
                   //Do something after 100ms
               }
           }, 1000);
           //cAndMeth.mScrollViewBlock((ScrollView) findViewById(R.id.idscrollview));      //170905
    }

    private void mAccept() {
        cUInput.mSetColorIndex(nSelCol[0],0);      //Foreground
        cUInput.mSetColorIndex(nSelCol[1],1);     //Background
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();   //Close this activity and return to previous
                return true;
            /*      //This had a flaw: when opening setupfile from cBitField it did not go back to bitfield but to main screen
                Intent intent = NavUtils.getParentActivityIntent(this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(this, intent);
                return true;
                */
        }
        return super.onOptionsItemSelected(item);
    }
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mSetGradient();
        // your code here
    }

    public void onResume() {
        super.onResume();

    }

    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight,
                               int oldBottom) {
        // its possible that the layout is not complete in which case
        // we will get all zero values for the positions, so ignore the event
        if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            return;
        }

        // Do what you need to do with the height/width since they are now set
    }
    private void mSetGradient() {
        float w = sliderBackColor.getWidth();
        if (w==0)
            w=600f;
        LinearGradient test = new LinearGradient(0.f, 0.f, w, 0.0f,
                new int[] { 0xFF000000, 0xFF0000FF, 0xFF00FF00, 0xFF00FFFF,
                        0xFFFF0000, 0xFFFF00FF, 0xFFFFFF00, 0xFFFFFFFF},
                null, Shader.TileMode.CLAMP);
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.getPaint().setShader(test);
        sliderBackColor.setProgressDrawable( (Drawable)shape );

    }

    private int mColor(int progress) {
        int r = 0;
        int g = 0;
        int b = 0;

        if(progress < 256){
            b = progress;
        } else if(progress < 256*2) {
            g = progress%256;
            b = 256 - progress%256;
        } else if(progress < 256*3) {
            g = 255;
            b = progress%256;
        } else if(progress < 256*4) {
            r = progress%256;
            g = 256 - progress%256;
            b = 256 - progress%256;
        } else if(progress < 256*5) {
            r = 255;
            g = 0;
            b = progress%256;
        } else if(progress < 256*6) {
            r = 255;
            g = progress%256;
            b = 256 - progress%256;
        } else if(progress < 256*7) {
            r = 255;
            g = 255;
            b = progress%256;
        }
        return Color.argb(255, r, g, b);
    }

    private int nSelected;
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int Col = mStr2Int(v.getTag().toString());

            if (bBackColor)
                nSelCol[1] = mStr2Int(v.getTag().toString());
            else
                nSelCol[0] = mStr2Int(v.getTag().toString());
            bBackColor=!bBackColor;
            mSetCols(nSelCol[0] ,    nSelCol[1] );

            /*
            nSelected = mStr2Int(v.getTag().toString());
            if (bIsdirty){
                nPalette[nSelected]=currColor;
                ((Button)(v)).setBackgroundColor(currColor);
                bIsdirty=false;
                return;
            }
            cUInput.mSetHandleColor(nSelected,bBackColor);
            if (bBackColor) {
                mSetCols(vp.nColorGet1((byte) 0),     vp.nColorGet1((byte) 1));
            }
            else
                finish();          //Close the activity on second selection
            bBackColor=!bBackColor;
            */
            }
        };

    private void mSetCols(int iForeCol, int iBackCol) {
        for (int i = 0; i < oButtons.length; i++) {
            if (i==iForeCol) {
                oButtons[i].setText("Front");
                oButtons[i].setTextColor(cProgram3.nPalette[i]^0xFF000000);
                oBtnColor[0].setBackgroundColor(cProgram3.nPalette[i]);
                nSelCol[0]=i;
            }
            else if (i==iBackCol) {
                oButtons[i].setText("Back");
                oButtons[i].setTextColor(cProgram3.nPalette[iForeCol]);
                oBtnColor[1].setBackgroundColor(cProgram3.nPalette[i]);
                nSelCol[1]=i;
            }
            else
                oButtons[i].setText("Col:"+i);

        }
    }

    private void mPopulate() {
        parentlayout = (RelativeLayout) findViewById(R.id.idparent3);
        oButtons=new Button[cProgram3.mGetPalette().length];
        for (int i = 0; i < oButtons.length; i++) {
            oButtons[i]=new Button (this);
            oButtons[i].setTag(i);
            oButtons[i].setBackgroundColor(cProgram3.nPalette[i]);
            oButtons[i].setOnClickListener(onClickListener);
            oButtons[i].setText("Col:"+i);
            parentlayout.addView(oButtons[i]);
        }
    }

    private void mAddToLayot(int i, Button myButton) {      //Adding with overflow to a relative layput
        RelativeLayout.LayoutParams rel_btn = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int myXPosition = (int) ((i % 5)* nViewDims[0] *1.2);
        int myYPosition = (int) ((i / 5)*nViewDims[1]*1.2);
        rel_btn.leftMargin = myXPosition;
        rel_btn.topMargin = myYPosition;
        rel_btn.width = nViewDims[0];
        rel_btn.height = nViewDims[1];

        myButton.setLayoutParams(rel_btn);
        parentlayout.addView(myButton);
    }
    private void mLayout_Reflow(ViewGroup oLayout){
        int pos[] ={0,0};
        for (int i=0;i<oLayout.getChildCount();i++){
            View myButton = oLayout.getChildAt(i);
            if (nViewDims[0] <myButton.getWidth()) nViewDims[0] =myButton.getWidth();
            if(nViewDims[1]<myButton.getHeight())nViewDims[1]=myButton.getHeight();

            RelativeLayout.LayoutParams rel_btn = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rel_btn.leftMargin = pos[0];    //X position
            rel_btn.topMargin = pos[1];     //Y position
            rel_btn.width = nViewDims[0];
            rel_btn.height = nViewDims[1];
            myButton.setLayoutParams(rel_btn);

            pos[0]=(int)  (pos[0]+(nViewDims[0] *1.2));       //Next position
            if (pos[0]+ nViewDims[0] >oLayout.getWidth()){       //Next line?
                pos[0]=0;
                pos[1]= (int) (pos[1]+(nViewDims[1]*1.2));
            }
        }
    }
    private void nMakeNewLine() {
        LinearLayout horizontalLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(  LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);;
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout p = (LinearLayout)parentlayout.getParent();
        LinearLayout.LayoutParams n = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.addView(horizontalLayout);
        parentlayout=horizontalLayout;
    }


    public void cmdSetColor(int n) {
        cProgram3.mMessage("Pressed "+n);
        /*
        cUInput.mSetColorIndex(n,bBackColor);
        if (bBackColor)        this.finish();          //Close the activity on second selection
        bBackColor=!bBackColor;
        */
    }
}
