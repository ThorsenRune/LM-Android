//Doc:  https://docs.google.com/document/d/1hba4rFO9x6oHt7Qxqj9Skk9aJn7n1qS5JlBLx-Y7SA8/edit

package it.fdg.lm;
/*  Class of constants used in the application
 * Created by rthorsen on 05/10/2017.
 */

public class cKonst {
     public enum eSerial{
        //  cSerial5 nState_Serial constants
        kBT_Connected,             //Good connection
        kBT_Undefined,            //Unconnected
         // following cKonst.eProtState.kConnectionError
        kBT_TimeOut,
        kDevicePickerActive, kDevicePickerClosed,       //State of discovery
         kConnectionError,
         kTryToConnect,
         kBrokenConnection,             //Device switched off !+ ideally we should listen for when it switches on again
         kDisconnected,  kBT_ConnectRequest, kListening, kListenAccepted,
         kOverflow;
      }

    public enum eNum {     //Application properties enumerator
        kPrivileges,
        kShowHidden,        //Will reveal all controls so they can be edited. ToDo set this flag false when not in editing mode
        kBlockScrolling,
        kAutoConnect,
        kRefreshRate      //program is running

    }
    public enum bitmask {
        nil1;
        static int kDebug=1<<3;    //  Debug level of user level
    }

    public static int nTextSize=15;
    public enum eTexts {     //
        a;
        public static String txtDevice_DoConnect    ="Switch on device and press here";
        public static String txtDevice_Connecting   ="Waiting for ";
        public static String txtDevice_TimeOut      ="Timeout in connection, Retry ";
        public static String txtDevice_Initializing ="Initializing protocol";
    };

    public enum eProtState{                         //States of the protocol        (171012 refactored to here)
        kProtResetReq,                              //Do a reset of the protocol
        //We are waing for device to send packs exposing the protocol
        kProtInitInProgress,                        //Request for reset of the protocol, kCommInit will be sent to device
        kProtInitDone,                              //Protocol is loaded and ready to mStartListening
        kBT_ConnectReq,                                //Request to mConnect to device
        //The protocol has just been filled with exposed variables and is ready to use
        kProtInitFailure,
        //Error in protocol initialization
        kProtUndef,                                 //The protocol is undefined and cannot be used, device disconnected
        kProtReady,                                 //Protocol is readyto use
        kProtTimeOut,
        kBTUnavailable,
        kProtError,
        kBT_DiscoverReq,                            //Request discovering a BT device (pairing)
        kBTDiscoverInProgress,                        //BT pairing in progress
        kBTDiscoverDone,
        kDoConnect_step2,
        kRelay,
        kConnected1,
        kConnectionError;

    }
}
