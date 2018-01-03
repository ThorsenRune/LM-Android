//      FIFO BUFFER used between Bluetooth stream and package handlign in cProtocol
package it.fdg.lm;


import static it.fdg.lm.cProgram3.mErrMsg;

public class cFIFO {		                    //
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
        mErrMsg ("FIFO Underrun");
        return -1;
    }

    public int mPeek(int nOffset){   //Peek at offset without popping
        if (nOffset>nBytesAvail) return -1;     //Past end of buffer
        int idx = nSTARTpointer + nOffset;
        if (idx>=nFIFOSize) idx=idx-nFIFOSize;
        return (aFIFO[idx] & 0xFF);
    }
    public int[] mFIFOShow(){   //Show the buffer for debug purpo
        int[] aData=new int[nBytesAvail];
        for (int i=0;i<nBytesAvail;i++){
            int idx = nSTARTpointer + i;
            if (idx>=nFIFOSize) idx=idx-nFIFOSize;
            aData[i]=aFIFO[idx];
        }
        return aData;
    }
}
//mFIFOpop
