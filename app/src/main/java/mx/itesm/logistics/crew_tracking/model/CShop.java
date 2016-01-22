package mx.itesm.logistics.crew_tracking.model;

import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import edu.mit.lastmite.insight_library.model.Shop;

public class CShop extends Shop {
    public static final String JSON_WRAPPER = "cshop";

    public JSONObject toJSON() throws JSONException {
        JSONObject wrapper = new JSONObject();
        wrapper.put(JSON_WRAPPER, toJSONWithoutWrapper());
        return wrapper;
    }

    public RequestParams buildParams() {
        HashMap<String, Object> object = new HashMap<>();
        object.put(JSON_LATITUDE, mLatitude);
        object.put(JSON_LONGITUDE, mLongitude);

        RequestParams params = new RequestParams();
        params.put(JSON_WRAPPER, object);
        return params;
    }
}
