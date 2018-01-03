//170822
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1CbbC7rmMiF9elvdlxlXcmLHVmI7TeRtVYwY4vdcV9Ow/edit
//General generic nonplatform specific functions

package it.fdg.lm;

import java.util.Arrays;

/**
 * General crossplatform functions and methods
 */

public final class cFunk {


    /****************************** NUMBERS and Strings*************************************/

    public static Integer Str2Int(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }//.................CONVERT A STRING TO INTEGER
	//-------------Covert a string to array of integers
	public static int[] str2IntArr(String str) {
        str = str.replace("[", "").replace("]", "");
        String[] sa = str.split(",");
        int L = sa.length;
        int[] ia = new int[L];
        for (int i = 0; i < L; i++) {
            try {
                ia[i] = Str2Int(sa[i].trim());
            } catch (NumberFormatException e) {
                ia[i] = 0;
            }
        }
        return ia;
    }

    public static String[] mIndexStringArry(cProtElem myElement) {
        int nVecLen=myElement.nDataLength();
        String sIndex[]=new String[nVecLen];
        for(int i = 0; i<nVecLen; i++) {
                sIndex[i] ="Idx: "+ Integer.toString(i);
            }
        return sIndex;
    }
    //      Boolean int conversion
    public static int mBool2int(boolean set) {
        if (set) return 1;
        else return 0;
    }
    public static boolean mInt2Bool(int value1) {
        return (value1!=0);
    }
    //      Properties
    static int bitstate(int nBitPattern, int nBitNumber){
        return (int) (nBitPattern & (1L << nBitNumber));
    }//Get bit status
    static int bitclear(int bits,int n){
        return (int) (bits & ~(1L << n));
    }       //Return bits with m bit cleared
    static int bitset(int bits, int n){
        return (int) (bits |(1L << n));
    }//Return bits with n'th bit set
    static int bittoggle(int bits,int n){
        return (int) (bits ^(1L << n));
    }//Return bits with n'th bit toggled

    //-------------Covert a string to array of integers
    public static int[] mStr2IntArr(String str) {
        str = str.replace("[", "").replace("]", "");
        String[] sa = str.split(",");
        int L = sa.length;
        int[] ia = new int[L];
        for (int i = 0; i < L; i++) {
            try {
                ia[i] = mStr2Int(sa[i].trim());
            } catch (NumberFormatException e) {
                ia[i] = 0;
            }
        }
        return ia;
    }
    public static String mStrArr2Str(String [] sArr){
        if (sArr==null)
            return "";
        else if (sArr.length<1)
            return "";//Return an empty string
        String s = Arrays.toString(sArr);
        return s;
    }
    public static String[] mStr2StrArr(String str){
        str = str.replace("[", "").replace("]", "");    //The brackest are from the native Array.str method
        String[] sa = str.split(",");
        for (int i=0;i<sa.length;i++) sa[i]=sa[i].trim();
        return  sa;
    }
    public static String mIntArr2Str(int[] intArr) {
        String s;
        s = Arrays.toString(intArr);
//        s = s.replace("[", "").replace("]", "");
        return s;
    }
    public static String mIntArr2HexStr(int[] intArr) {
        String s="[";
        for (int i=0;i<intArr.length;i++) {
            if (i > 0) s=s+",";
            s = s + "0x" + Integer.toHexString(intArr[i]).toUpperCase();
        }
        return s+"]";
    }
    public static float[] mStr2FloatArr(String str) {
        str = str.replace("[", "").replace("]", "");    //The brackest are from the native Array.str method
        String[] sa = str.split(",");
        int L = sa.length;
        float[] ia = new float[L];
        for (int i = 0; i < L; i++) {
            try {
                ia[i] = mStr2Float(sa[i].trim(), 0);
            } catch (NumberFormatException e) {
                ia[i] = 0;
            }
        }
        return ia;
    }
    public static int mStr2Int(String text) {
        text=text.replaceAll("\\D+-", "");
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {     //Try to decode hexadecimal hex2int
            if (text.contains("[0][xX]")) text=text.replaceFirst("[0][xX]","");
            if (text.contains("#")) text=text.replaceFirst("#","");
            try {   //Try hexadecimal
                return (int) Long.parseLong(text, 16);
            }catch (NumberFormatException e1){
            }
            if (text.length()>1)
                return mStr2Int(text.substring(1));     //Remove first letter could be # or 0x
            return 0;
        }
    }
    public static byte[] mInt2ByteArray(int i)    {       //Convert a 32 bit integer to 4 bytes
        byte[] result = new byte[4];      //[] Check negative integer
        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);
        return result;
    }
    public static String mInt2str(int  Value) {
        String s;
        s = Integer.toString(Value);
        return s;
    }
    public static String mFloatArr2Str(float[] myDisplayRange) {
        String s;
        s=Arrays.toString(myDisplayRange);
        return s;
    }
    public static String byteArrayToString(byte[] bytes)     {   //Casting a byte array to string
        return new String(bytes);
    }
    //  FLOAT STRING CONVERSION (rev 170818 to be more robust for user input errors, returns defValue on error)
    public static float mStr2Float(String str, float defValue){
        float nVal=0;
        str = str.replace("[", "").replace("]", "");
        try {
            nVal= Float.valueOf(str);
        } catch (NumberFormatException e) {     //Strip from end until you have a valid number
            if (str.length()<1) return defValue;
            str=str.substring(0, str.length() - 1);
            nVal=mStr2Float(mTrim(str), defValue);
        }
        return nVal;
    }
    public static String mFloat2Str(float value) {
        return Float.toString(value);
        //return String.format("%.1f",value);
    }
    public static String mTrim(String s) {     //Remove strange characters like 160 break and trailing whitespace
        return s.replaceAll("[\u00A0\n]"," ").trim();
    }

    public static int mArrayFind(String[] sArray, String sLookFor) {
        if (sLookFor==null) return -1;
        if (sLookFor=="") return -1;
        for (int i = 0; i < sArray.length; i++) {
            if (cFunk.mTextLike(sArray[i],sLookFor)) {
                return i;
            }
        }
        return -1;
    }
    //Redimensioning arrays
    public static String[] mArrayRedim(String[] sArr, int nArrayIdx) {
        int i;
        for (i=0;(nArrayIdx>=sArr.length); i++)
            sArr = mStr2StrArr(mStrArr2Str(sArr) + ",Nil]");
        return sArr;
    }
    public static int[] mArrayRedim(int[] nArr, int nArrayIdx) {
        int i;
        for (i=0;(nArrayIdx>=nArr.length); i++)
            nArr = mStr2IntArr(mIntArr2Str(nArr) + ",0]");
        return nArr;
    }

    //          Math
    public static int mLimit( int nmin, int value, int nmax) {
        if (value<nmin) return nmin;
        if (value>nmax) return nmax;
        return value;
    }
    public static float mLimit(float nmin, float v1, float nmax) {
        if (v1 < nmin) return nmin;
        if (v1 > nmax) return nmax;
        return v1;
        ////        !!! methods to move to base utils
    }


    public static boolean mTextLike(String s1, String s2) {
        if (s1==s2) return true;
        if (s1==null) return false;
      return mTrim( s1).equals(mTrim(s2));
    }

    public static int mAverage(int y0, long x) { //Averaging a value
        int alfa=500;
        long y1 = (y0 * alfa + x * (1000 - alfa)) / 1000;
        return (int) y1;
    }
}
