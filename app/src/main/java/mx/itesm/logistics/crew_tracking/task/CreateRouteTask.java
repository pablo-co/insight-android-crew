package mx.itesm.logistics.crew_tracking.task;


import android.util.Log;

import org.apache.http.Header;
import org.json.JSONObject;

import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import mx.itesm.logistics.crew_tracking.util.Preferences;

public class CreateRouteTask extends NetworkTask {
    protected Route mRoute;

    public CreateRouteTask(Route route) {
        mRoute = route;
    }

    @Override
    public void execute(Callback callback) {
        mCallback = callback;
        updateRoute();
        mAPIFetch.post("routes/postRoute", mRoute.buildParams(), new APIResponseHandler(mApplication, null, false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Route route = new Route(response);
                    saveRouteId(route.getId());
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
        return mRoute;
    }

    protected void updateRoute() {
        long vehicleId = getVehicleId();
        if (mRoute.getVehicleId() == null && vehicleId != -1) {
            mRoute.setVehicleId(vehicleId);
        }
    }

    protected long getVehicleId() {
        return getLocalLong(Preferences.PREFERENCES_VEHICLE_ID);
    }

    protected void saveRouteId(long routeId) {
        putLocalLong(Preferences.PREFERENCES_ROUTE_ID, routeId);
    }
}
