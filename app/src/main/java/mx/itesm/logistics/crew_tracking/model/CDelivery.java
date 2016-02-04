package mx.itesm.logistics.crew_tracking.model;

import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import edu.mit.lastmite.insight_library.model.Delivery;

public class CDelivery extends Delivery {
    public static final String JSON_WRAPPER = "cdelivery";
    public static final String JSON_SERVED = "served";
    public static final String JSON_TYPE = "type";
    public static final String JSON_START_TIME = "start_time";
    public static final String JSON_END_TIME = "end_time";

    protected Boolean mServed;
    protected Long mStartTime;
    protected Long mEndTime;

    public CDelivery() {
        measureTime();
    }

    public Boolean getServed() {
        return mServed;
    }

    public void setServed(Boolean served) {
        mServed = served;
    }

    public Long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(Long startTime) {
        mStartTime = startTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }

    public void setEndTime(Long endTime) {
        mEndTime = endTime;
    }

    public void measureTime() {
        if (mStartTime == null) {
            mStartTime = System.currentTimeMillis();
        }

        if (mEndTime == null) {
            mEndTime = System.currentTimeMillis();
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject wrapper = new JSONObject();
        wrapper.put(JSON_WRAPPER, toJSONWithoutWrapper());
        return wrapper;
    }

    public RequestParams buildParams() {
        HashMap<String, Object> object = new HashMap<>();
        object.put(JSON_ID, mId);
        object.put(JSON_TIME, mTime);
        object.put(JSON_LATITUDE, mLatitude);
        object.put(JSON_LONGITUDE, mLongitude);
        object.put(JSON_TYPE, mType);
        object.put(JSON_SERVED, mServed);
        object.put(JSON_START_TIME, mStartTime);
        object.put(JSON_END_TIME, mEndTime);
        object.put(JSON_VISIT_ID, mVisitId);
        object.put(JSON_ROUTE_ID, mRouteId);

        RequestParams params = new RequestParams();
        params.put(JSON_WRAPPER, object);
        return params;
    }
}
