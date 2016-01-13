package mx.itesm.logistics.crew_tracking.task;

import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import mx.itesm.logistics.crew_tracking.model.CStop;
import mx.itesm.logistics.crew_tracking.util.Preferences;

public class StopCStopTask extends NetworkTask {
    protected CStop mCStop;

    public StopCStopTask(CStop cStop) {
        mCStop = cStop;
    }

    @Override
    public void execute(Callback callback) {
        mCallback = callback;
        updateCStop();
        mAPIFetch.post("cstops/postEndcstop", mCStop.buildParams(), new APIResponseHandler(mApplication, null, false) {
            @Override
            public void onFinish(boolean success) {
                activateCallback(success);
            }
        });
    }

    @Override
    public Object getModel() {
        return mCStop;
    }

    protected void updateCStop() {
        mCStop.setId(getCStopId());
    }

    protected long getCStopId() {
        return getLocalLong(Preferences.PREFERENCES_CSTOP_ID);
    }
}
