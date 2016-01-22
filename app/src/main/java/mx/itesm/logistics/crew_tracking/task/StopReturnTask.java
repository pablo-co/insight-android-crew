package mx.itesm.logistics.crew_tracking.task;

import android.util.Log;

import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import mx.itesm.logistics.crew_tracking.model.Return;
import mx.itesm.logistics.crew_tracking.util.Preferences;

public class StopReturnTask extends NetworkTask {
    protected Return mReturn;

    public StopReturnTask(Return aReturn) {
        mReturn = aReturn;
    }

    @Override
    public void execute(Callback callback) {
        mCallback = callback;
        updateReturn();
        Log.d("TAG", mReturn.buildParams().toString());
        mAPIFetch.post("returnings/postEnd", mReturn.buildParams(), new APIResponseHandler(mApplication, null, false) {
            @Override
            public void onFinish(boolean success) {
                activateCallback(success);
            }
        });
    }

    @Override
    public Object getModel() {
        return mReturn;
    }

    protected void updateReturn() {
        mReturn.setId(getReturnId());
    }

    protected long getReturnId() {
        return getLocalLong(Preferences.PREFERENCES_RETURN_ID);
    }
}
