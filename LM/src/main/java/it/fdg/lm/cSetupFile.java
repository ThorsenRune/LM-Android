//171005    Refactoring
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//Doc:  https://docs.google.com/document/d/1HVjmwPpq_Rx3uYDMZUzUErTfTFebEBtCJkuK7Jjjipw/edit

package it.fdg.lm;
/*  Editor of application settings
* */

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;

import static it.fdg.lm.cFileSystem.mFileRead;
import static it.fdg.lm.cFileSystem.mFileSave;
import static it.fdg.lm.cFileSystem.mPref2Str;
import static it.fdg.lm.cFileSystem.mPref_Clear;
import static it.fdg.lm.cFileSystem.mStr2Pref;
import static it.fdg.lm.cFileSystem.readRawTextFile;
import static it.fdg.lm.cFileSystem.sCurrPrefFileName;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.mPersistAllData;
import static it.fdg.lm.cProgram3.sMsgLog;


public class cSetupFile extends BaseActivity {
    private cElemViewProps myElemView;
    private TextView oEditor;
    private File file;
    private int nTest=0;
    private TextView oFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_display);
        //setupActionBar();
        mPersistAllData(false);     //Save app values to preferences to sync with current data so thats what you see
        oEditor = (TextView) findViewById(R.id.idSettingsText);
        oFilter = (TextView) findViewById(R.id.idFilterText);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);   //Hide the soft keyboard when activity gets focus
    }
    @Override
    protected void onDestroy() {            //Last event before program is killed
        super.onDestroy();
    }
    //Must come after onCreate (at least it seems so)
    public void cmdCommand(View v){
        String s =mEditText();           //Get the settings from texteditor
        mStr2Pref(s);                                 //Store them in local preferences
        mPersistAllData(true);                    //Load volatile data from local preferences
        mPersistAllData(false);                    //save volatile RAM to local preferences
        mEditText(mPref2Str(sFilterText()));                     //Readback of settings to texted
        /*Testing how to make links clickable
        oEditor.setLinksClickable(true);
        oEditor.setAutoLinkMask(Linkify.WEB_URLS);
        Linkify.addLinks(oEditor, Linkify.WEB_URLS);
        */
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {// R170822C
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();   //Close this activity and return to previous  ( R170822C)
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onResume() {
        super.onResume();
        cProgram3.oFocusdActivity=this;
    }



    //*********************TEXTEDITOR ***************************


    private void mAdd2Settings() {
        String s1 = mEditText();
        s1=mStr2Pref(s1);
        mEditText(s1);
        mPersistAllData(true);     //Load app
    }

    private void mShowSettings() {
        mPersistAllData(false);      //Vars to prefs
        String s = mPref2Str(sFilterText());
        mEditText(s);
    }

    private String sFilterText() {
        String s = oFilter.getText().toString();
        return  s;
    }

    private String mEditText() {     //Get the text of editor
        String s =oEditor.getText().toString();           //Get the settings from texteditor
        return  s;
    }
    private void mEditText(String s) {
        oEditor.setText(s);
    }

    private void mFile2Settings() {
        String s = mFileRead(this,sCurrPrefFileName);    //Read file
        mEditText(s);
        s=mStr2Pref(s);                      //Transfer to preferences
        mPersistAllData(true);                 //Read app settings from preferences
    }
//  R170725     New menu pattern
    public boolean mMenuAction1(Menu menu, int nId) {    //Create/Check menu action in one
        if (mnuClick(nId   ,"Clear text",true)){
            mEditText("");
            return true;
        } else if (mnuClick(nId,"Load factory settings",true)) {
            mLoadRawFile();
            return true;

        } else if (mnuClick(nId,"Show current Settings",true)) {
            mShowSettings();
            return true;

        } else if (mnuClick(nId,"Clear settings")) {
            mPref_Clear();            //Remove old preferences
            return true;
        } else if (mnuClick(nId,"Load from file")){
            mFile2Settings();            //Clicked action
            return true;
        } else if (mnuClick(nId,"Add to settings",true)){
            mAdd2Settings();
            return true;        }
        else if (mnuClick(nId,"Save to file")) {
            mSettings_Save();
            return true;
        }
        else if (mnuClick(nId,"See error log")) {
            mEditText(sMsgLog);
            return true;
        }
        if (BaseActivity.menuFindBTDevice(nId)) finish();

        return true;
    }

    private void mLoadRawFile() {
        mPref_Clear();                  //Clear settings and load the LM_5.txt in raw
        String s = readRawTextFile(R.raw.lm_5);
        mEditText(s);
        mStr2Pref(s);
    }

    private  void mSettings_Load() {
        //Load external file of settings to editor, to make them active use save
        String s = mFileRead(this, sCurrPrefFileName);    //Read file
        mMessage("Settings loaded from "+sCurrPrefFileName);
        mEditText(s);
    }
    private  void mSettings_Save() {
        String s =mPref2Str("");           //Get the settings
        mFileSave(sCurrPrefFileName,s);                    //Store texteditor to external file
        mMessage("Prefences saved to " +sCurrPrefFileName);
    }
}
