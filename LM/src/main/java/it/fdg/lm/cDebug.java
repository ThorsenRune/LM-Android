//      Testing and debugging
//https://docs.google.com/document/d/1UDs1rimRQGZ1xY1e6ugaWuFbjg8a3SzdPyEUF0WwOBg/edit
package it.fdg.lm;

/**
 * Created by rthorsen on 05/10/2017.
 */

public class cDebug {
    public static int[] nTestCount ={0,0,0,0,0,0,0,0};    //Temporay debugging counter
    public static long[] nTickTock={0,0,0,0,0,0,0,0};
    public static long[] nTick={0,0,0,0,0,0,0,0};
    public static long[] nTock={0,0,0,0,0,0,0,0};
    public static boolean bTest=false;

    public static void mTock(int i) {
        //nTickTock[i]=System.nanoTime()-nTick[0];
        nTickTock[i]=System.currentTimeMillis()-nTick[0];
    }

    public static void mTick(int i) {
        //nTick[i]=System.nanoTime();
        nTick[i]=System.currentTimeMillis();
    }


}
