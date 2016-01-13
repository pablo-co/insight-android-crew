package mx.itesm.logistics.crew_tracking.model;

import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.String;
import java.util.HashMap;

import edu.mit.lastmite.insight_library.model.Location;

public class CrewLocation extends Location {
    public static final String JSON_WRAPPER = "crewtrace";

    public CrewLocation(android.location.Location location) {
        super(location);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject wrapper = new JSONObject();
        wrapper.put(JSON_WRAPPER, toJSONWithoutWrapper());
        return wrapper;
    }

    public RequestParams buildParams() {
        HashMap<String, Object> object = new HashMap<>();
        object.put(JSON_LATITUDE, mLatitude);
        object.put(JSON_LONGITUDE, mLongitude);
        object.put(JSON_SPEED, mSpeed);
        object.put(JSON_TIME, mTime);
        object.put(JSON_CSTOP_ID, mCStopId);

        RequestParams params = new RequestParams();
        params.put(JSON_WRAPPER, object);
        return params;
    }
}
