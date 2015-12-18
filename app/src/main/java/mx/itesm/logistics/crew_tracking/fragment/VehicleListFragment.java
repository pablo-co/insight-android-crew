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

package mx.itesm.logistics.crew_tracking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.rey.material.widget.ProgressView;
import com.squareup.otto.Bus;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.User;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;

public class VehicleListFragment extends FragmentResponder implements ListView.OnItemClickListener {

    public static final String TAG = "VehicleListFragment";

    public static final String EXTRA_VEHICLE = "com.gruporaido.tasker.extra_vehicle";

    protected User mUser;

    protected VehicleAdapter mVehicleAdapter;

    protected ArrayList<Vehicle> mVehicles;

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Bus mBus;

    @Bind(R.id.vehicle_list_listView)
    protected ListView mListView;

    @Bind(R.id.loadingProgressView)
    protected ProgressView mLoadingProgressView;

    @Bind(android.R.id.empty)
    protected View mEmptyView;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = Lab.get(getActivity()).getUser();
        mVehicles = new ArrayList<>();
        mVehicleAdapter = new VehicleAdapter(mVehicles);
        loadVehicles();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_vehicle_list, parent, false);

        ButterKnife.bind(this, view);

        mListView.setEmptyView(mLoadingProgressView);
        mListView.setAdapter(mVehicleAdapter);
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mVehicles.size() == 0) {
            mLoadingProgressView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        sendResult(TargetListener.RESULT_OK, mVehicles.get(position));
    }

    protected void loadVehicles() {
        mVehicles.clear();
        Log.e(TAG, mUser.buildParams().toString());
        mAPIFetch.post("vehicles/postVehicles", mUser.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.e(TAG, response.toString());
                try {
                    JSONArray vehicles = response.getJSONArray(Vehicle.JSON_WRAPPER + "s");
                    addVehiclesFromJSON(vehicles);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d(TAG, response.toString());
                addVehiclesFromJSON(response);
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
                mLoadingProgressView.setVisibility(View.GONE);
                mListView.setEmptyView(mEmptyView);
            }
        });
    }

    protected void addVehiclesFromJSON(JSONArray response) {
        try {
            for (int i = 0; i < response.length(); ++i) {
                mVehicles.add(new Vehicle(response.getJSONObject(i)));
            }
            mVehicleAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResult(int resultCode, Vehicle vehicle) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_VEHICLE, vehicle);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }

    private class VehicleAdapter extends ArrayAdapter<Vehicle> {

        public VehicleAdapter(ArrayList<Vehicle> vehicles) {
            super(getActivity(), 0, vehicles);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_vehicle, null);
            }

            Vehicle vehicle = getItem(position);

            TextView numberTextView = (TextView) convertView.findViewById(R.id.item_vehicle_identifierTextView);
            numberTextView.setText(vehicle.getPlates());

            return convertView;
        }
    }

}
