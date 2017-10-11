//      FIFO BUFFER used between Bluetooth stream and package handlign in cProtocol
package it.fdg.lm;



public class cFIFO {		//[] Put a c in front to indicate that it's a class
    public static final int kFIFOSize=10000;	//Maximal buffer nDataLength in bytes
    public int nBytesAvail;
    private int nSTARTpointer;
    private int nENDpointer;
    private int nFIFOSize;
    private byte[] aFIFO;

    cFIFO(int nSize){
        nFIFOSize=nSize;
        aFIFO=new byte[nSize];
        this.mFlush();
    }   //Construct the FIFO buffer
    public final void mFlush(){               //170217 flush the buffer
        nBytesAvail=0;
        nSTARTpointer=0;				//Startpointer==Endpointer means overflow or buffer empty
        nENDpointer=0;
    }

    //-----------------------------------------------------------------------------------------------------------------
    public boolean mCanPop(int number){       //!+
        return (number<=this.nBytesAvail);
    }
    public boolean mCanPush(){
        return (nBytesAvail<nFIFOSize);
    }
    public int  nFree(){
        return (nFIFOSize-nBytesAvail);
    }
    //--------------------------------------------------------------------------------------
    public void mFIFOpush(byte b){
        if(this.mCanPush()==false){
          //  GUIAnd.message1("FATAL error!! BUFFER is FULL!!!");
        }else{
            this.aFIFO[nENDpointer]=b;
            this.nENDpointer++;
            nBytesAvail=nBytesAvail+1;
            if(nENDpointer>=nFIFOSize){//rollover
                this.nENDpointer=0;
            }
            if(this.nENDpointer==this.nSTARTpointer){
              //  GUIAnd.message1("Fatal error nr 42 mFIFOpush full");
            }
        }
    }//mFIFOpush

    //--------------------------------------------------------------------------------------------------------
    public int mFIFOpop(){
        byte b=0;
        if(0<nBytesAvail){
            b=aFIFO[nSTARTpointer];
            nSTARTpointer++;
            nBytesAvail=nBytesAvail-1;
            if(nSTARTpointer>=nFIFOSize){//Rollover
                nSTARTpointer=0;
            }
            return ((int)b  & 0xFF);    //Typecasting to an unsigned byte in integer format
        }
      // GUIAnd.message1("Another fatal error !!!");
        return -1;
    }
}
//mFIFOpop
