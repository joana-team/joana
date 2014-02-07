
package dummy;

import android.content.SyncStatusObserver;
//import android.app.ActivityOptions.OnAnimationStartedListener;
import android.app.AppOpsManager.Callback;

public class Misc implements SyncStatusObserver, /* OnAnimationStartedListener, */ Callback {

    public void onStatusChanged(int which) { }
    public void onAnimationStarted() { }
    public void opChanged(int op, String packageName) { }
}
