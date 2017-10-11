    //Rev 170919/170825
//Doc:  https://docs.google.com/document/d/1NoJTIE1K5UjaGTTAaelJdhM35_ZJsCVOAGpWONIcjV4/edit
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
package it.fdg.lm;

    import android.app.Activity;
    import android.content.Context;
    import android.util.Log;
    import android.view.GestureDetector;
    import android.view.MotionEvent;
    import android.view.ScaleGestureDetector;
    import android.view.View;

    import static it.fdg.lm.cProgram3.bAdmin;
    import static it.fdg.lm.cProgram3.mAppProps;
    import static java.lang.Math.abs;

    public class cGestureListener extends Activity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {
        static View myParent;

        public static float nScaleFactor;
        public static float nScaleCenterX;
        public static float nScaleCenterY;
        public static int[] nFlingDir ={0,0};
        public static float nX,nY;

        public static boolean isScrollingBlocked = cFunk.mInt2Bool(mAppProps(cKonst.eNum.kBlockScrolling))  ;        //Blocks scrollveiws
        static boolean bLongPress=false;
        static boolean bDoubleTap=false;
        static boolean bFling;
        static boolean bSingleTap=false;
        static boolean bScrolling=false;
        static int nSignalPaneHeight;
        static boolean bSingleTapConfirmed;
        private final ScaleGestureDetector oScaleDetector;      //
        private final GestureDetector oGestureDetector;
        private boolean bFlingLR;
        private boolean bFlingUD;
        private boolean bZoomEnable=bAdmin();
        private static boolean bScaling;
        private ScaleGestureDetector oScale;
        private boolean bScaleBegin=false;
        private boolean retval;


        public cGestureListener(Context mContext){
            oScaleDetector = new ScaleGestureDetector(mContext, this);
            oGestureDetector = new GestureDetector(mContext,this);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d("myLog", "onDoubleTapEvent");
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            bDoubleTap=true;
            return true;            //True when event is consumed
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            bSingleTapConfirmed=true;
            return false;
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            nY=motionEvent.getY();
            nX=motionEvent.getX();
            bSingleTap=true;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            bScrolling=true;
            return isScrollingBlocked;       //No scroll event implemented
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
                bLongPress=true;
        }
        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float velocityX, float velocityY) {
            /*A fling is one of several "gestures" recognised in Android; it is a quick swipe action that has no specific target. In Android, a fling generates a "velocity" value, which can be used to calculate how widgets should react after the user's finger has left the screen. A common example is seen when scrolling a list.*/
            bFling=true;
            nFlingDir[0]= (int) -velocityX;
            nFlingDir[1]= (int) -velocityY;
            if (abs(velocityY)>2*abs(velocityX)) {
                bFlingUD = true;      //Fling updown
            } else if (2*abs(velocityY)<abs(velocityX)) {
                bFlingLR=true;      //Fling LR
            }
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if(bZoomEnable==true){
                bScaling=true;
                nScaleFactor = detector.getScaleFactor();
                nScaleCenterX =  detector.getFocusX();
                nScaleCenterY= detector.getFocusY();
                //            oElemViewProps().mZoom(scalePointX,scalePointY,nFactor);
                return true;
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            bScaleBegin=true;
            oScale=scaleGestureDetector;
            return true ;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            bScaleBegin=false;
        }

        //      END OF EVENTS


        public boolean mListen(View myParent1, MotionEvent event) {     //170824 Supposed to replace mySetParent and gestureDetceoor on touch
            myParent = myParent1;
            if (myParent1 instanceof cSliderView)
                cUInput.setFocus(myParent);
            else if (myParent1 instanceof  cSignalView2) {
                cUInput.setFocus(myParent);
            } else if (myParent1 instanceof  cData_View)
                cUInput.setFocus(myParent);
            //else             mErrMsg("Call not implemented");

            int a = event.getAction();
            //oScaleDetector.onTouchEvent(event); //Always returns true
            retval=false;
            boolean handled = oScaleDetector.onTouchEvent(event);
            retval = oGestureDetector.onTouchEvent(event)|| handled;
            if (a==MotionEvent.ACTION_UP)
                if (!bScaleBegin){

            } else
                retval= true;
            return retval;
        }

        public static void mySetParent(Object newParent) {     //Replace by listen above !!todo
         //   if (myParent == newParent) return;      //MInimize recalls
            if (newParent instanceof View)
                myParent =(View) newParent;
            if (newParent instanceof  cSignalView2)
                cUInput.setFocus(myParent);
            else if (newParent instanceof  cData_View)
                cUInput.setFocus(myParent);
            else if (newParent instanceof cSlider)
                cUInput.setFocus(newParent);
            //else             mErrMsg("Call not implemented");
        }

        public boolean bLongPress() {           //Will clear the flag once tested
            boolean b=bLongPress;bLongPress=false;
            return b;
        }
        public boolean bDoubleTap() {           //Will clear the flag once tested
            boolean b=bDoubleTap;bDoubleTap=false;
            return b;
        }
        public boolean bSingleTap() {
            boolean b=bSingleTap;bSingleTap=false;
            return b;
        }
        public boolean bFlingLR() {
            boolean b=bFlingLR;bFlingLR=false;
            return b;
        }

        public boolean bScaling() {
            boolean b=bScaling;bScaling=false;
            return b;
        }

        public boolean bInputGesture() {
            boolean b=bLongPress|bDoubleTap;
            bDoubleTap=false;bLongPress=false;      //Clear
            return b;
        }
    }