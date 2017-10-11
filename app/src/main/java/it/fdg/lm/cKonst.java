//Doc:  https://docs.google.com/document/d/1hba4rFO9x6oHt7Qxqj9Skk9aJn7n1qS5JlBLx-Y7SA8/edit

package it.fdg.lm;
/*  Class of constants used in the application
 * Created by rthorsen on 05/10/2017.
 */

public class cKonst {
    public enum eNum {     //Application properties enumerator
        kPrivileges,
        kShowHidden,        //Will reveal all controls so they can be edited. ToDo set this flag false when not in editing mode
        kBlockScrolling,
        kAutoConnect,
        kRefreshRate      //program is running
    };

    public static float kTextSize=15;
}
