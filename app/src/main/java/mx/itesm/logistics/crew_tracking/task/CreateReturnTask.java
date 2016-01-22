package mx.itesm.logistics.crew_tracking.task;


import org.apache.http.Header;
import org.json.JSONObject;

import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import mx.itesm.logistics.crew_tracking.model.Return;
import mx.itesm.logistics.crew_tracking.util.Preferences;

public class CreateReturnTask extends NetworkTask {
    protected Return mReturn;

    public CreateReturnTask(Return aReturn) {
        mReturn = aReturn;
    }

    @Override
    public void execute(Callback callback) {
        mCallback = callback;
        updateReturn();
        mAPIFetch.post("returnings/postInitialreturning", mReturn.buildParams(), new APIResponseHandler(mApplication, null, false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Return aReturn = new Return(response);
                    saveReturnId(aReturn.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }
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
        mReturn.setCStopId(getCStopId());
    }

    protected long getCStopId() {
        return getLocalLong(Preferences.PREFERENCES_CSTOP_ID);
    }

    protected void saveReturnId(long returnId) {
        putLocalLong(Preferences.PREFERENCES_RETURN_ID, returnId);
    }
}
