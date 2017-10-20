package it.fdg.lm;
//folder: https://drive.google.com/drive/u/0/folders/0B5pCUAt6BabuU1I1Ri1zNzA4SG8
//doc:  https://docs.google.com/document/d/1DZHtpABi8_ojjYB80mZkYM1BGeR8dZl6DTg3ZUgmo8A/edit

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import static it.fdg.lm.cAndMeth.mIsHidden;
import static it.fdg.lm.cAndMeth.mToggleVisibility;
import static it.fdg.lm.cFileSystem.readRawTextFile;
import static it.fdg.lm.cFunk.mInt2Bool;
import static it.fdg.lm.cGestureListener.nSignalPaneHeight;
import static it.fdg.lm.cProgram3.mErrMsg;
import static it.fdg.lm.cProgram3.mProtocol;
import static it.fdg.lm.cProgram3.mRefreshRate;
import static it.fdg.lm.cProgram3.nCurrentProtocol;
import static it.fdg.lm.cProgram3.nUserLevel;
import static it.fdg.lm.cProgram3.oFocusdActivity;
import static it.fdg.lm.cProgram3.oProtocol;
import static it.fdg.lm.cUInput.mFocusedPanel;
import static it.fdg.lm.fMain.fPanelSignals1;
import static it.fdg.lm.fMain.fPanelVertSliders;


public class BaseActivity extends AppCompatActivity {
    private static Menu myMenu;
    private static BaseActivity mThis;
    private static MenuItem _mItem;
    private static int nClickCnt1;
    private static int nMnuCount=0;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        myMenu = menu;
        myMenu.clear();
        mDoMenu(myMenu, -1);    //Just redraw menus
        return super.onPrepareOptionsMenu(menu);
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
        if (myMenu==null) return false;
        if (mnuClick(nId, "Set Value", cUInput.mSelected())) {
            cUInput.mInputValue(true);
        }
        if (mnuCheck(nId,"Show signal",!mIsHidden(fPanelSignals1))){      //Revision 170921 increase panel size
            mToggleVisibility(fPanelSignals1);
            cAndMeth.mLayoutWeightSet(fPanelSignals1,nSignalPaneHeight);
        }
        if (mnuCheck(nId,cKonst.eTexts.txtMenu_showVertical,!mIsHidden(fPanelVertSliders))){
            mToggleVisibility(fPanelVertSliders);
            cProgram3.bDoRedraw=true;
        }
        if (mnuClick(nId,"Next page ")){
            cProgram3.nWatchPage=((cProgram3.nWatchPage+1)%2);
            cProgram3.bDoRedraw=true;
        }
        if ((0<nUserLevel())&(mFocusedPanel()!=null)){
            if (mnuClick(nId,"Zoom panel")){
                ViewGroup fa = (ViewGroup) mFocusedPanel();
                float w = cAndMeth.mLayoutHeightGet(fa);
                w=(w+1)%5;
                cAndMeth.mLayoutWeightSet(fa,w);
            }

        }

        if (mnuClick(nId,"Mode settings")){
            mBitFields_Show();
        }
        if (cProgram3.nUserLevel()>0) {            //170728    Advanced permissions
            if (mnuClick(nId, "Control:"+ cUInput.oCtrlID(), cUInput.mSelected())) {
                cUInput.mInputViewSettings1(true); //Associate element with widget
            }
            if (nUserLevel()>1) //Admin only
            if (mnuCheck(nId, "Design mode", mInt2Bool(cProgram3.mAppProps(cKonst.eAppProps.kShowHidden)))) {
                cProgram3.mAppPropsToggle(cKonst.eAppProps.kShowHidden);   cProgram3.bDoRedraw=true;         //Clicked action
            }
            if (mnuClick(nId, "Settings")) {
                mDispSettings();
            }
            BaseActivity.menuFindBTDevice(nId);
            if (nUserLevel()>1) //Admin only    !- testing only
            if (mnuClick(nId,"Listen for client"))
            {
                cProgram3.oProtocol[0].oSerial.mStartServerService();
            }
        }
        myMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "______________________________________").setEnabled(false);
        if (mnuClick(nId,"User permissions"))          //R170725
            cUInput.mUserLevel(true);
        if (mnuClick(nId,"About & Help"))          //R170725
        {
            mShowAbout();
        }
        return false;
    }

    private void mShowAbout() {      //Open a browser with the google help document
        String s = readRawTextFile(R.raw.lm2_help_html);
        cUInput.mShowHtml("Libermano App",s);
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
    static void mMenuCommon(Menu myMenu, int nId) {        //Negative index will just update, index of which menu has been clicked
        nMnuCount=0; //   Reset menucounter
        //NOw the Connection menu
        if (mnuMain(nId, mGetConnectMenuText())) {    //Make a connection, 3 clicks to
            cProgram3.mCommunicate(true);
            mProtocol().mProtResetReq();
        }
        mRefreshRate(500);
    }

    private static String mGetConnectMenuText() {
            String title = oProtocol[nCurrentProtocol].mDeviceNameGet();
            return title;
        }       //Returns a text to display on the menu



    boolean mnuCheck(int nId, String s, boolean bChecked) {
        mnuClick(nId,s);
        if (nId<0) {
            myMenu.findItem(nMnuCount).setCheckable(true);
            myMenu.findItem(nMnuCount).setChecked(bChecked);
        }

        return (nId==nMnuCount);
    }
    private static boolean mnuMain(int nId, String s) {
        mnuClick(nId,s);
        if (nId<0) {
            myMenu.findItem(nMnuCount).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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



    public static boolean menuFindBTDevice(int nId) {
        if (mnuClick(nId,"Find BT device"))          //R170725
        {
            cProgram3.mProtocol().mBT_PickDevice1();
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
