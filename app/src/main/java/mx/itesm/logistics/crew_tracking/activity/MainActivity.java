/*
 * [2015] - [2015] Grupo Raido SAPI de CV.
 * All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Created by Pablo Cárdenas on 25/10/15.
 */

package mx.itesm.logistics.crew_tracking.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.activity.SingleFragmentActivity;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.fragment.CrewTrackFragment;
import mx.itesm.logistics.crew_tracking.fragment.LoadingFragment;
import mx.itesm.logistics.crew_tracking.model.VehicleType;
import mx.itesm.logistics.crew_tracking.service.LocationManagerService;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;

public class MainActivity extends SingleFragmentActivity {

    @Inject
    protected Helper mHelper;

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Lab mLab;

    protected boolean mIsLoading = false;

    @Override
    protected Fragment createFragment() {
        if (mLab.getVehicleTypes().isEmpty()) {
            mIsLoading = true;
            return new LoadingFragment();
        } else {
            return new CrewTrackFragment();
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    public void injectActivity(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overrideTransitions = false;
        super.onCreate(savedInstanceState);
        getVehicleTypes();
    }

    protected void getVehicleTypes() {
        mAPIFetch.get("typevehicles/getTypes", null, new APIResponseHandler(this, getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray types = response.getJSONArray(VehicleType.JSON_WRAPPER + "s");
                    addVehicleTypesFromJSON(types);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }
        });
    }

    protected void addVehicleTypesFromJSON(JSONArray json) {
        ArrayList<VehicleType> vehicleTypes = new ArrayList<>();
        try {
            for (int i = 0; i < json.length(); ++i) {
                VehicleType vehicleType = new VehicleType(json.getJSONObject(i));
                vehicleTypes.add(vehicleType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mLab.setVehicleTypes(vehicleTypes).saveVehicleTypes();
        inflateTrackFragment();
    }

    protected void inflateTrackFragment() {
        if (mIsLoading) {
            inflateFragment(R.id.fragmentContainer, new Helper.FragmentCreator() {
                @Override
                public Fragment createFragment() {
                    return new CrewTrackFragment();
                }
            }, R.animator.no_animation, R.animator.no_animation);
        }
    }
}
