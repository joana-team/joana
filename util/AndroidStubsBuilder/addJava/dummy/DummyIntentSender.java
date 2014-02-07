package dummy;

import android.content.IntentSender;
import android.os.Bundle;
import android.content.Intent;

class DummyIntentSender implements IntentSender.OnFinished {
    public DummyIntentSender() {}
    public void onSendFinished(IntentSender IntentSender, Intent intent, int resultCode, String resultData, Bundle resultExtras) {}
}
