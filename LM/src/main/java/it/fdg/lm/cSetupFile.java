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
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

import static it.fdg.lm.cFileSystem.mFileRead;
import static it.fdg.lm.cFileSystem.mPref2Str;
import static it.fdg.lm.cFileSystem.mPref_Clear;
import static it.fdg.lm.cFileSystem.mStr2Pref;
import static it.fdg.lm.cProgram3.mAppSettings;
import static it.fdg.lm.cProgram3.mMessage;
import static it.fdg.lm.cProgram3.mPersistAllData;
import static it.fdg.lm.cProgram3.sFile_ProtCfg;
import static it.fdg.lm.cProgram3.sMsgLog;
import static it.fdg.lm.cUInput.mSettingsFile_Select;


public class cSetupFile extends BaseActivity {
    private cElemViewProps myElemView;
    private TextView oEditor;
    private File file;
    private int nTest=0;
    private TextView oFilter;
    private Button cmdFilter;
    private TextView oTextMsg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_display);
        cmdFilter = (Button)findViewById(R.id.idbutton31);
        cmdFilter.setText("Filter");
        cmdFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cmdCommand();
            }
        });
        //setupActionBar();
        oEditor = (TextView) findViewById(R.id.idSettingsText);
        oFilter = (TextView) findViewById(R.id.idFilterText);
        oTextMsg=(TextView)findViewById(R.id.idMsg);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);   //Hide the soft keyboard when activity gets focus
        sFilterText( cUInput.sElemName());
    }



    private void cmdCommand1() {
        cmdCommand();
    }

    @Override
    protected void onDestroy() {            //Last event before program is killed
        super.onDestroy();
    }
    //Must come after onCreate (at least it seems so)
    public void cmdCommand(){
        String s =mEditText();           //Get the settings from texteditor
        s=mFilterText(s,sFilterText());
        mEditText(s);                     //Readback of settings to texted
    }

    private String mFilterText(String s, String sFilter) {
        String sOut="";
        String kSp="\n";
        String[] sa = s.split(kSp);
        for (int i = 0; i < sa.length; i++) {
            if (sa[i].toLowerCase().contains(sFilter.toLowerCase()))
                   sOut=sOut+sa[i]+kSp;
        }
        return sOut;
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


    private void mAdd2Settings() {      //Save the settings
        String s1 = mEditText();
        s1=cFileSystem.mStr2Pref(sFile_ProtCfg,s1);     //Save in internal preferences file
        mEditText(s1);
        mAppSettings(false);
        mPersistAllData(true,sFile_ProtCfg);     //Load app

    }



    private void mShowSettings() {
        oTextMsg.setText(sFile_ProtCfg);
        mPersistAllData(false,sFile_ProtCfg);      //Vars to prefs
        String s = mPref2Str(sFilterText());
        mEditText(s);
    }

    private String sFilterText() {
        String s = oFilter.getText().toString();
        return  s;
    }
    private void sFilterText(String s) {
        oFilter.setText(s);
    }

    private String mEditText() {     //Get the text of editor
        String s =oEditor.getText().toString();           //Get the settings from texteditor
        return  s;
    }
    public   void mEditText(String s) {
        oEditor.setText(s);
    }

    private void mFile2Settings( String sFile_ProtCfg) {
        String s = mFileRead(this,sFile_ProtCfg);    //Read file
        mEditText(s);
        s=mStr2Pref(s);                      //Transfer to preferences
        mPersistAllData(true,sFile_ProtCfg);                 //Read app settings from preferences
    }
//  R170725     New menu pattern
    public boolean mMenuAction1(Menu menu, int nId) {    //Create/Check menu action in one
        if (mnuClick(nId   ,"Clear text",true)){
            mEditText("");
            return true;
        } else if (mnuClick(nId,"Load protocol settings",true)) {
            mSettingsFile_Select();
            return true;
        } else if (mnuClick(nId,"Show current Settings",true)) {
            mShowSettings();
            return true;

        } else if (mnuClick(nId,"Clear settings")) {
            mPref_Clear();            //Remove old preferences
            return true;
        } else if (mnuClick(nId,"Load from file")){
            mFile2Settings(sFile_ProtCfg);            //Clicked action
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
            sMsgLog="";
            return true;
        }



        return true;
    }



    private  void mSettings_Load() {
        //Load external file of settings to editor, to make them active use save
//        String s = mFileRead(this, sCurrPrefFileName);    //Read file
        mMessage("to implement");
        //mEditText(s);
    }
    private  void mSettings_Save() {
        String s =mPref2Str("");           //Get the settings
   //     mFileSave(sCurrPrefFileName,s);                    //Store texteditor to external file
    //    mMessage("Prefences saved to " +sCurrPrefFileName);
    }
}
