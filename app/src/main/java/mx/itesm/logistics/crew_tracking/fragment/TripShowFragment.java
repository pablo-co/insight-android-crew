package mx.itesm.logistics.crew_tracking.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.maps.model.LatLng;
import com.rey.material.widget.ProgressView;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Iterator;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.annotation.ServiceConstant;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.event.FinishProgressEvent;
import edu.mit.lastmite.insight_library.event.ProgressEvent;
import edu.mit.lastmite.insight_library.event.TrackEvent;
import edu.mit.lastmite.insight_library.fragment.BaseFragment;
import edu.mit.lastmite.insight_library.fragment.InsightMapsFragment;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.model.Visit;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import edu.mit.lastmite.insight_library.util.ServiceUtils;
import edu.mit.lastmite.insight_library.util.Storage;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.activity.RouteListActivity;
import mx.itesm.logistics.crew_tracking.activity.VehicleListActivity;
import mx.itesm.logistics.crew_tracking.model.CrewLocation;
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueueWrapper;
import mx.itesm.logistics.crew_tracking.task.NetworkTaskWrapper;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Preferences;

/**
 * GRUPO RAIDO CONFIDENTIAL
 * __________________
 *
 * [2015] - [2015] Grupo Raido SAPI de CV
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Grupo Raido SAPI de CV and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Grupo Raido SAPI de CV and its
 * suppliers and may be covered by México and Foreign Patents,
 * patents in process, and are protected by trade secret or
 * copyright law. Dissemination of this information or
 * reproduction of this material is strictly forbidden unless
 * prior written permission is obtained from Grupo Raido SAPI
 * de CV.
 */


public class TripShowFragment extends BaseFragment implements TargetListener {
    private static final String TAG = "TripShowFragment";

    @ServiceConstant
    public static String EXTRA_QUEUE_NAME;

    static {
        ServiceUtils.populateConstants(TripShowFragment.class);
    }

    public static final int REQUEST_LIST = 0;
    public static final int REQUEST_VEHICLE = 1;

    @Inject
    protected Helper mHelper;

    @Inject
    protected CrewNetworkTaskQueueWrapper mQueueWrapper;

    @Inject
    protected Storage mStorage;

    @Bind(R.id.loadingProgressView)
    protected ProgressView mProgressView;

    @Bind(R.id.trip_submitButton)
    protected Button mSubmitButton;

    protected Vehicle mVehicle;
    protected String mQueueName;
    protected ArrayList<Visit> mVisits;

    public static TripShowFragment newInstance(String queueName) {
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_QUEUE_NAME, queueName);

        TripShowFragment fragment = new TripShowFragment();
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
        setHasOptionsMenu(true);
        mQueueName = getArguments().getString(EXTRA_QUEUE_NAME);
        mVisits = new ArrayList<>();
        loadVisits();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_show, parent, false);
        ButterKnife.bind(this, view);

        mHelper.inflateFragment(getActivity().getSupportFragmentManager(), R.id.trip_mapsLayout, new Helper.FragmentCreator() {
            @Override
            public Fragment createFragment() {
                return InsightMapsFragment.newInstance(
                        InsightMapsFragment.Flags.ROTATE_WITH_DEVICE |
                                InsightMapsFragment.Flags.DRAW_TRACKS
                );
            }
        }, R.animator.slide_up_in, R.animator.slide_down_out);

        mHelper.inflateFragment(getActivity().getSupportFragmentManager(), R.id.cstop_visitsLayout, new Helper.FragmentCreator() {
            @Override
            public Fragment createFragment() {
                VisitListFragment fragment = VisitListFragment.newInstance(mVisits);
                fragment.setTargetListener(TripShowFragment.this, REQUEST_LIST);
                return fragment;
            }
        }, R.animator.slide_up_in, R.animator.slide_down_out);

        mProgressView.start();
        mStorage.putLocalLong(mQueueName, Preferences.PREFERENCES_ROUTE_ID, 200);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_trip, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.cstop_menu_item_destroy:
                mQueueWrapper.destroyQueue(mQueueName);
                closeFragment();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != TargetListener.RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_LIST:
                Visit visit = data.getParcelableExtra(VisitListFragment.EXTRA_VISIT);
                publishTrace(visit);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_VEHICLE:
                mVehicle = (Vehicle) data.getSerializableExtra(VehicleListActivity.EXTRA_VEHICLE);
                startSync();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.trip_submitButton)
    public void onSubmitClicked() {
        Intent intent = new Intent(getContext(), VehicleListActivity.class);
        startActivityForResult(intent, REQUEST_VEHICLE);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onProgressEvent(ProgressEvent event) {
        if (event.getQueueName().equals(mQueueName)) {
            mProgressView.setProgress(event.getProgress());
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onFinishProgressEvent(FinishProgressEvent event) {
        if (event.getQueueName().equals(mQueueName)) {
            closeFragment();
        }
    }

    protected void startSync() {
        showProgressView();
        mStorage.putLocalLong(mQueueName, Preferences.PREFERENCES_VEHICLE_ID, mVehicle.getId());
        mQueueWrapper.executeQueue(mQueueName);
    }

    protected ArrayList<LatLng> mapToLatLng(ArrayList<Location> locations) {
        ArrayList<LatLng> latLngs = new ArrayList<>();
        Iterator<Location> iterator = locations.iterator();
        while (iterator.hasNext()) {
            Location location = iterator.next();
            latLngs.add(new LatLng(location.getLatitude(), location.getLongitude()));
        }
        return latLngs;
    }

    protected void publishTrace(Visit visit) {
        ArrayList<LatLng> latLngs = mapToLatLng(visit.getLocations());
        mBus.post(new TrackEvent(latLngs));
    }

    protected void loadVisits() {
        mVisits.clear();
        addQueue(mQueueName);
    }

    protected void addQueue(String queueName) {
        mQueueWrapper.changeToQueue(queueName);
        NetworkTaskQueue queue = mQueueWrapper.getQueue();
        NetworkTaskWrapper taskWrapper = new NetworkTaskWrapper(queue);
        ArrayList<Object> objects = taskWrapper.getModels();

        Iterator<Object> iterator = objects.iterator();
        while (iterator.hasNext()) {
            Visit visit = getNextVisitObject(iterator);
            if (visit != null) {
                visit.setLocations(getLocationObjects(iterator));
                mVisits.add(visit);
            }
        }
    }

    protected Visit getNextVisitObject(Iterator<Object> iterator) {
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (isAVisitObject(object)) {
                return (Visit) object;
            }
        }
        return null;
    }

    /**
     * Will ignore the last object that is not a Location, in theory this would be
     * a Visit object taken from a StopVisitTask. Although this might not be true.
     */
    protected ArrayList<Location> getLocationObjects(Iterator<Object> iterator) {
        ArrayList<Location> locations = new ArrayList<>();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (isALocationObject(object)) {
                locations.add((Location) object);
            } else {
                break;
            }
        }
        return locations;
    }

    protected boolean isAVisitObject(Object object) {
        return object != null && object.getClass() == Visit.class;
    }

    protected boolean isALocationObject(Object object) {
        return object != null && object.getClass() == CrewLocation.class;
    }

    protected void showProgressView() {
        mSubmitButton.setVisibility(View.GONE);
    }

    protected void closeFragment() {
        try {

            getFragmentManager().popBackStack();
        } catch (IllegalStateException e) {
        }
    }
}