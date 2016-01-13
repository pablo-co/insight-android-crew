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
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;

public class RouteListFragment extends FragmentResponder implements ListView.OnItemClickListener {
    public static final String TAG = "RouteListFragment";

    public static final String EXTRA_VEHICLE = "com.gruporaido.tasker.extra_vehicle";
    public static final String EXTRA_ROUTE = "com.gruporaido.tasker.extra_route";

    @Inject
    protected Lab mLab;

    @Inject
    protected Helper mHelper;

    protected Vehicle mVehicle;
    protected User mUser;
    protected RouteAdapter mRouteAdapter;
    protected ArrayList<Route> mRoutes;

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Bus mBus;

    @Bind(R.id.route_list_listView)
    protected ListView mListView;

    @Bind(R.id.loadingProgressView)
    protected ProgressView mLoadingProgressView;

    @Bind(android.R.id.empty)
    protected View mEmptyView;

    public static RouteListFragment newInstance(Vehicle vehicle) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(EXTRA_VEHICLE, vehicle);

        RouteListFragment fragment = new RouteListFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = mLab.getUser();
        mVehicle = (Vehicle) getArguments().getSerializable(EXTRA_VEHICLE);
        mRoutes = new ArrayList<>();
        mRouteAdapter = new RouteAdapter(mRoutes);
        loadRoutes();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_list_route, parent, false);
        ButterKnife.bind(this, view);

        mListView.setEmptyView(mLoadingProgressView);
        mListView.setAdapter(mRouteAdapter);
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mRoutes.size() == 0) {
            mLoadingProgressView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        sendResult(TargetListener.RESULT_OK, mRoutes.get(position));
    }

    protected void loadRoutes() {
        mRoutes.clear();
        Log.e(TAG, mVehicle.buildParams().toString());
        mAPIFetch.get("routes/getRoutes", mVehicle.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.e(TAG, response.toString());
                try {
                    JSONArray routes = response.getJSONArray(Route.JSON_WRAPPER + "s");
                    addRoutesFromJSON(routes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d(TAG, response.toString());
                addRoutesFromJSON(response);
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
                mLoadingProgressView.setVisibility(View.GONE);
                mListView.setEmptyView(mEmptyView);
            }
        });
    }

    protected void addRoutesFromJSON(JSONArray response) {
        try {
            for (int i = 0; i < response.length(); ++i) {
                mRoutes.add(new Route(response.getJSONObject(i)));
            }
            mRouteAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResult(int resultCode, Route route) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_ROUTE, route);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }

    private class RouteAdapter extends ArrayAdapter<Route> {

        public RouteAdapter(ArrayList<Route> routes) {
            super(getActivity(), 0, routes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_route, null);
            }

            Route route = getItem(position);

            TextView startTimeTextView = (TextView) convertView.findViewById(R.id.item_route_startTimeTextView);
            startTimeTextView.setText(mHelper.getDateStringFromTimestamp(route.getStartTime()));

            TextView endTimeTextView = (TextView) convertView.findViewById(R.id.item_route_endTimeTextView);
            endTimeTextView.setText(mHelper.getDateStringFromTimestamp(route.getEndTime()));

            return convertView;
        }
    }
}
