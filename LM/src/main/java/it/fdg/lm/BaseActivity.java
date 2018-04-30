package it.fdg.lm;
//doc:  https://docs.google.com/document/d/1DZHtpABi8_ojjYB80mZkYM1BGeR8dZl6DTg3ZUgmo8A/edit
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//rev 171222

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import static it.fdg.lm.cFileSystem.mFileRead_Raw;
import static it.fdg.lm.cKonst.eProtState.kProtReady;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.nPanelSizes;
import static it.fdg.lm.cProgram3.oFocusdActivity;
import static it.fdg.lm.cProgram3.oUInput;
import static it.fdg.lm.cProgram3.oaProtocols;
import static it.fdg.lm.fMain.fPanelVertSliders;


public class BaseActivity extends AppCompatActivity {
    private static Menu myMenu;
    private static BaseActivity mThis;
    private static MenuItem _mItem;
    private static int nClickCnt1;
    private static int nMnuCount=0;
    private static MenuItem mnuStatus;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        if (fPanelVertSliders==null) return false ; //Guard null pointer
        myMenu = menu;
        myMenu.clear();
        mDoMenu(myMenu, -1);    //Just redraw menus
        mStatusRedraw();
        return super.onPrepareOptionsMenu(menu);
    }

    public static void mStatusRedraw() {
        String sDsc="";
        for (int i = 0; i < oaProtocols.length; i++) {
            if (oaProtocols[i].getState()==kProtReady)  sDsc=sDsc+","+oaProtocols[i].mDeviceNameGet()+"=OK";
            if (oaProtocols[i].getState()!=kProtReady) {sDsc=sDsc+","+oaProtocols[i].mDeviceNameGet()+"=Err";}
            if (oaProtocols[i].getState()==kProtReady)  sDsc= "Devices - OK";
            if (oaProtocols[i].getState()!=kProtReady) {sDsc= "Device - ERROR";break;}
        }
        if (myMenu==null) return;
        if (mnuStatus==null)
            mnuStatus=myMenu.findItem(1);
        if (mnuStatus!=null)
            mnuStatus.setTitle( sDsc);
    }
    private boolean mDoMenu(Menu myMenu, int id) {
        mMenuCommon(myMenu,id);
        if (oFocusdActivity instanceof cSetupFile)
            return ((cSetupFile)oFocusdActivity).mMenuAction1(myMenu,id);
        if (oFocusdActivity instanceof fMain)
            return mMenuAction_dm(myMenu,id);
        if (oFocusdActivity instanceof cBitField)
            return ((cBitField)oFocusdActivity).mMenuAction_bf(myMenu,id);
      return true;
    }

    public boolean mMenuAction_dm(Menu myMenu, int nId) {
        if (myMenu == null) return false;
        if (mnuClick(nId, "Set Value", oUInput.mSelected())) {
            oUInput.mInputValue1();
        }
        myMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "______________________________________").setEnabled(false);
        if (mnuCheck(nId, cKonst.eTexts.txtMenu_showVertical, nPanelSizes[0]>0)) {
            nPanelSizes[0]=-nPanelSizes[0];
            cProgram3.bDoRedraw = true;
        }
        if (mnuCheck(nId, "Blanking",nPanelSizes[1]>0)) {
            nPanelSizes[1]=-nPanelSizes[1];
            cProgram3.bDoRedraw = true;
        }
        if (mnuCheck(nId, "Show signal", nPanelSizes[2]>0)) {      //Revision 170921 increase panel size
            nPanelSizes[2]=-nPanelSizes[2];
            cProgram3.bDoRedraw = true;
        }

        if (mnuCheck(nId, "Show Data",  nPanelSizes[3]>0)) {      //Revision 170921 increase panel size
            nPanelSizes[3]=-nPanelSizes[3];
            cProgram3.bDoRedraw = true;
        }
        if (mnuClick(nId, "Set Device Mode")) {
            mBitFields_Show();
        }
        if (cProgram3.mPrivileges() > 0) {            //180329    Advanced permissions
            if (mnuClick(nId, "Edit:" + cUInput.oCtrlID(), cUInput.mSelected())) {
                oUInput.mEditControl2(); //Associate element with widget
            }
        }
        if (mnuClick(nId, "Settings")) {
            oUInput.mSettingsDialog();
            //mDispSettings();
        }
        myMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "______________________________________").setEnabled(false);
        if (mnuClick(nId,"About & Help"))          //R170725
        {
            mShowAbout();
        }
        return false;
    }

    private void mShowAbout() {      //Open a browser with the google help document
        String s = mFileRead_Raw(cProgram3.sFileName_Help);
        cUInput.mShowHtml("LibreMano App",s);
    }

    public static void mDispSettings(){
        Intent intent = new Intent(cProgram3.mContext, cSetupFile.class);
        cProgram3.mContext.startActivity(intent);
    }

    private static void mBitFields_Show() {
        Intent intent = new Intent(cProgram3.mContext, cBitField.class);
        cProgram3.mContext.startActivity(intent);
    }

    //170816  Revision of the way menus are handled
    void mMenuCommon(Menu myMenu, int nId) {        //Negative index will just update, index of which menu has been clicked
        nMnuCount=0; //   Reset menucounter
        //NOw the Connection menu
        if (mnuMain(nId, cProgram3.sConnectStatus)) {    //Make a connection, 3 clicks to
            oUInput.mInputSelectDevice1(); //180328Dev
        }
    }

    private static String mGetConnectMenuText() {

        return "Status";
        }       //Returns a text to display on the menu



    boolean mnuCheck(int nId, String s, boolean bChecked) {
        mnuClick(nId,s);
        if (nId<0) {
            myMenu.findItem(nMnuCount).setCheckable(true);
            myMenu.findItem(nMnuCount).setChecked(bChecked);
        }

        return (nId==nMnuCount);
    }
    public static boolean mnuMain(int nId, String s) {
        mnuClick(nId,s);
        if (nId<0) {
            mnuStatus = myMenu.findItem(nMnuCount); //r180328
            mnuStatus.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return (nId==nMnuCount);
            /*      Alternative information
    *.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    * see also:https://stackoverflow.com/questions/22046903/changing-the-android-overflow-menu-icon-programmatically
    * */
    }

    public static boolean mnuClick(int nId, String s) {
        return mnuClick( nId,  s,true);
    }
    public static boolean mnuClick(int nId, Object s,boolean ena) {   //add a clickmenu
        nMnuCount=        nMnuCount+1;  //Increase for each call to iterate through menus
        if (nId < 0) {          //Add the   item to the menu bar
            _mItem=myMenu.add(Menu.NONE, nMnuCount, Menu.NONE, (CharSequence) s);   //Make menu item
            _mItem.setEnabled(ena);
        }
        if (nId == nMnuCount) {      //Returns true if clicked
            if (myMenu==null)                mErrMsg("Fatal nulponter");
            else            _mItem=myMenu.findItem(nId);
            return true;
        }
        return false;
    }





    //  --------------------------  SYSTEM CALLS ---------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mThis = this;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        myMenu = menu;
 //           mDoMenu(myMenu, -1);    //Just redraw menus
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        mDoMenu(myMenu, id);
        return super.onOptionsItemSelected(item);
    }


}
