//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1n5cxcvRK8UCgizSq2F5HOb8E9mJj1_OphfQWwdkFaOc/edit
//          FILESYSTEM related functions, Android specific
package it.fdg.lm;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static it.fdg.lm.cFunk.byteArrayToString;
import static it.fdg.lm.cFunk.mFloatArr2Str;
import static it.fdg.lm.cFunk.mIntArr2Str;
import static it.fdg.lm.cFunk.mStr2FloatArr;
import static it.fdg.lm.cFunk.*;
import static it.fdg.lm.cFunk.mTrim;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.mMsgDebug;


/**
 * Created by rthorsen on 04/10/2017.
 * Managing files and persistent storage
 */

public class cFileSystem {
    private static Context mContext;
    //      CONSTANTS
    private static String kKeySepStr ="=>";
    private static String kKeyEndLine=";";  //was "\n"

    //  Private stuff

    private static File myFile;
    private static SharedPreferences oPreferences;             //Standard stuff for the preferences
    private static  SharedPreferences.Editor prefsEditor;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;


    //**************************************        IMPLEMENTATION          ******************
    public static void mInit(Context main2) {
        mContext=main2;
    }
    //****170721            Read/Write file         *****
    private static void verifyStoragePermissions(Context mContext) {
        // Check if we have write permission on api>23
        int permission = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    (Activity) mContext,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    public static String mFileRead(Context mContext, String sFileName) {
        String content = "";
        FileInputStream fileIn;
        verifyStoragePermissions(mContext);
        File mDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        myFile = new File(mDir, sFileName);
        if (myFile.exists()) {
            try {
                fileIn = new FileInputStream(myFile);     //2ver
                byte[] buffer =   new byte[(int) fileIn.getChannel().size()];
                fileIn.read(buffer);   //2ver
                fileIn.close();
                content=byteArrayToString(buffer);

            }
            catch (Exception e){
                mErrMsg("Error: " + e.getMessage());}
            catch                  (Throwable t) {
                mErrMsg( "Exception: " + t.toString());
            }
        }
        return content.trim();
    }

    public static void mFileSave(String sFileName,String sSaveText) {
        verifyStoragePermissions(mContext);
        File mDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        myFile = new File(mDir, sFileName);
        try {
            FileOutputStream fileOut = new FileOutputStream(myFile);     //2ver
            fileOut.write(sSaveText.getBytes());   //2ver
            fileOut.close();
            mMsgDebug( "Settings Saved!");
        } catch (Throwable t) {
            mMsgDebug("Exception: " + t.toString());
            //Check: Did you pass the right context?
        }
    }


    //Read text from RAW file with sFileName
    public static String mFileRead_Raw(String sFileName)
    {
        Context ctx = mContext;
        int resId = ctx.getResources().getIdentifier(sFileName, "raw", ctx.getApplicationInfo().packageName);
        if (resId<1) mErrMsg("Cant find resource file:"+sFileName);
        InputStream inputStream = ctx.getResources().openRawResource(resId);
        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }

//PREFERENCES           _______________________________/*_________________________________________________________________*/
    //The preferences is a way of storing application data on the persistent system memopry
//Work function for mPrefs5 is loading/saving in persistent memory
    public static String[] mGetPrefFileNames(String StripExt){
        File prefsdir = new File(mContext.getApplicationInfo().dataDir,"shared_prefs");
        String[] list = prefsdir.list();
        String fl="";
        for (int i = 0; i < list.length; i++) {
            if (list[i].contains(StripExt)){
                fl = fl + list[i].replaceAll(StripExt, ";");
            }
        }
        return fl.split(";");
    }
    public static String[] mGetDownloadFileNames(String StripExt){
        verifyStoragePermissions(mContext);
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File directory = new File(path);
        String[] list = directory.list();
        String fl="";
        for (int i = 0; i < list.length; i++) {
            if (mTextLike(list[i],StripExt)){
                fl = fl + list[i].replaceAll(StripExt, ";");
            }
        }
        return fl.split(";");
    }
    public static String[] mGetRawFileNames(){          //Extension less list of files in raw directory
        Field[] fields = R.raw.class.getFields();
        String[] list=new String[fields.length];
        for(int i=0; i < fields.length; i++){
            list[i]=fields[i].getName();
        }
        return list;
    }
    public static int mPrefFileNameSet(String sPrefFileName){
        //Set the current preferences file, (load factory settings from raw directory
        sPrefFileName=sPrefFileName.replace(".xml",""); //Remove extension
         mMessage("File:"+ sPrefFileName);
        oPreferences = mContext.getSharedPreferences(sPrefFileName, Context.MODE_PRIVATE);
        prefsEditor = oPreferences.edit();
        return oPreferences.getAll().size();
}

    public static Boolean mPref5HasKey(String sKey){
        if (oPreferences==null) return false;
        return oPreferences.contains(sKey);
    }
    public static String mPrefs5(boolean doGet, String sKey, String strValue ){          //Get set default values the Android way
        /*Context mContext = Program.mContext;//.get();
         see them all by watch :oPreferences.mMap  */
    String s="";
    sKey=sKey.trim();
    if (oPreferences ==null)
        return "";      //file has not yet been selected, this is an error that should be removed by reordinging program execution
    else if(doGet){
        s= oPreferences.getString(sKey, strValue).replace("[", "").replace("]", "").trim();
        return mTrim(s);
    }
    else {
        prefsEditor.putString(sKey, strValue.trim());
        prefsEditor.commit();    //Don't forget to commit the changes
        return strValue;
    }
}

    public static void  mPref_Clear(){
        prefsEditor.clear();            //Remove old preferences
        prefsEditor.commit();
    };                      //Clear existing settings

    public static String mPref2Str(String sFilter) {
        /*  exchanging preferences with a string      mPref2Str,mStr2Pref      *******
        *   file layout   <key>  :  [   <VALUE> ]*/
        String sStr1 = "";
        Map<String, ?> keys = oPreferences.getAll();
        SortedSet<String> keys1 = new TreeSet<String>(keys.keySet());
        for (String key : keys1) {
            String value = mTrim(keys.get(key).toString()).replace("[","").replace("]","");
            if (sFilter=="")
                sStr1 = sStr1 + key + "\t"+ kKeySepStr +"\t[" + value + "]"+kKeyEndLine+"\n";
            else if (key.toLowerCase().contains(sFilter.toLowerCase()))
                sStr1 = sStr1 + key + "\t"+ kKeySepStr +"\t[" + value + "]"+kKeyEndLine+"\n";
            else if (key.toLowerCase().contains(sFilter.toLowerCase()))
                sStr1 = sStr1 + key + "\t"+ kKeySepStr +"\t[" + value + "]"+kKeyEndLine+"\n";
        }
        return sStr1;
    }       //Make a string with the preferences (selected by Filter)
    public static String mStr2Pref(String sFile_protCfg, String s1) {
        cFileSystem.mPrefFileNameSet(sFile_protCfg);
        return cFileSystem.mStr2Pref(s1);
    }
    public static String mStr2Pref(String sStr1){
        sStr1=sStr1.replaceAll("/\\*.*\\*/", ""); //Remove "/* comments */"
        sStr1=sStr1.replaceAll("//.*?\n","\n") ;    //Remove comments
//        String[] separated = sStr1.split("\\r?\\n");           //Split by newlines (There's only really two newlines (UNIX and Windows) that you need to worry about.)
        String[] separated = sStr1.split(kKeyEndLine);           //Split by semicolon
        String sStrRes="";
        String key="";
        for (int i = 0; i < separated.length; i++) {
            String s[] = mTrim(separated[i]).split(kKeySepStr);
            if (s.length>0) key=_GetKey(s[0]);
            if (s.length==2) {
                //Remove remarks
                s[1]=s[1].replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");
                //Remove double backslash end of line
                String value = mTrim((s[1]));
                value= mPrefs5(false,key,value);    //Write the value
                 sStrRes = sStrRes + key + "\t"+ kKeySepStr +"\t" + value + kKeyEndLine+"\n";
            }
            else if  ((key.length()>1)&(s.length==1)) {
                try {
                    prefsEditor.remove(key);       //Remove empty values
                } catch (Exception e) {
                }
            }
        }
        prefsEditor.commit();               //Write the data to the file
        return sStrRes;
    }           //Set settings by a string

    //Overloading the mPrefs5 for different datatypes

    public static float[] mPrefs5(boolean bGetIt, String sKey, float[] naFloat) {
        naFloat=mStr2FloatArr(mPrefs5(bGetIt, sKey, mFloatArr2Str(naFloat)));
        return naFloat;
    }
    public static int[] mPrefs5(boolean bGetIt, String sKey, int[] nInts) {
        nInts=mStr2IntArr(mPrefs5(bGetIt, sKey, mIntArr2Str(nInts)));
        return nInts;
    }
    public static int[] mPersistHex(boolean bGetIt, String sKey, int[] nInts) {
        nInts=mStr2IntArr(mPrefs5(bGetIt, sKey, mIntArr2HexStr(nInts)));
        return nInts;
    }
    public static String[] mPrefs5(boolean bGetIt, String sKey, String[] aStr) {
        aStr=mStr2StrArr(mPrefs5(bGetIt, sKey, mStrArr2Str(aStr)));
        return aStr;
    }
    public static boolean mPrefs5(boolean getIt, String privileges, boolean bShowHidden) {
        int v=0;            //Convert true=1
        if (bShowHidden) v=1;
        v= mPrefs5(getIt,privileges,v);
        return v != 0;
    }
    public static int mPrefs5(boolean doGet, String sKey, int Value){      //Remember int
        return mStr2Int(mPrefs5(doGet,sKey,"["+mInt2str(Value)+"]").replace("[", "").replace("]", ""));
    }
    public static float mPrefs5(boolean doGet, String sKey, float Value){  //Remember Float
        //https://docs.google.com/document/d/1XGr-o9CHRaiVuQK9ZJQnmfMKFJby2Zq3zY4slL3I9Vs/edit#heading=h.yuhic13krhbx
        return mStr2Float(mPrefs5(doGet,sKey,"["+mFloat2Str(Value)+"]"), 0);
    }
    private static String _GetKey(String s) {
        //String key =mTrim() s[0].replaceAll("[^A-Za-z0-9_>-].", ""));           //Remove spaces and non characters in keys
        return         mTrim( s.replaceAll(" ", ""));           //Remove spaces  in keys

    }


    public static String mRawFile2PrefFile(String sPrefFileName) {
        String s = mFileRead_Raw(sPrefFileName);
        mPrefFileNameSet(sPrefFileName);
        mStr2Pref(s);
        return s;
    }
}
