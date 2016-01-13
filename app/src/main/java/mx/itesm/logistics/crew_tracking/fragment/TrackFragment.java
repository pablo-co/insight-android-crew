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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.loopj.android.http.RequestParams;
import com.rey.material.widget.TextView;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.User;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.model.Visit;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.activity.CStopListActivity;
import mx.itesm.logistics.crew_tracking.activity.ShopListActivity;
import mx.itesm.logistics.crew_tracking.activity.VehicleListActivity;
import mx.itesm.logistics.crew_tracking.model.CStop;
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueueWrapper;
import mx.itesm.logistics.crew_tracking.service.LocationManagerService;
import mx.itesm.logistics.crew_tracking.task.CreateCStopTask;
import mx.itesm.logistics.crew_tracking.task.CreateVisitTask;
import mx.itesm.logistics.crew_tracking.task.StopCStopTask;
import mx.itesm.logistics.crew_tracking.task.StopVisitTask;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;

public class TrackFragment extends edu.mit.lastmite.insight_library.fragment.TrackFragment {
    public static final int REQUEST_VEHICLE = 0;
    public static final int REQUEST_SHOP = 1;

    public enum TrackState implements State {
        IDLE,
        WAITING_LOCATION,
        DELIVERING,
        TRACKING
    }

    @Inject
    protected Lab mLab;

    @Inject
    protected CrewNetworkTaskQueueWrapper mNetworkTaskQueueWrapper;

    protected User mUser;
    protected Vehicle mVehicle;
    protected Route mRoute;
    protected CStop mStop;
    protected Visit mVisit;

    @Bind(R.id.track_deliveringButton)
    protected FloatingActionButton mDeliveringButton;

    @Bind(R.id.track_deliveredButton)
    protected FloatingActionButton mDeliveredButton;

    @Bind(R.id.track_stopButton)
    protected FloatingActionButton mStopButton;

    @Bind(R.id.track_vehicleTextView)
    protected TextView mVehicleTextView;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mUser = mLab.getUser();
        mVehicle = mLab.getVehicle();
        mState = TrackState.IDLE;

        resetRoute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);
        ButterKnife.bind(this, view);

        findTrackViews(view);
        startIdle();
        updateVehicleView();
        inflateMapsFragment(R.id.track_mapLayout);

        mOverlayLayout.setAlpha(OVERLAY_OPACITY);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_track, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.track_menu_item_logout:
                return true;
            case R.id.track_menu_item_sync:
                Intent intent = new Intent(getContext(), CStopListActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_VEHICLE:
                mVehicle = (Vehicle) data.getSerializableExtra(VehicleListActivity.EXTRA_VEHICLE);
                sendAssignVehicle(mVehicle, mUser);
                mLab.setVehicle(mVehicle).saveVehicle();
                updateVehicleView();
                break;
            case REQUEST_SHOP:
                resetStats();
                startMenu();
                break;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_startButton)
    protected void onStartClicked() {
        if (mLastLocation != null) {
            startMenu();
        } else {
            startWaitingLocation();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_deliveringButton)
    protected void onDeliveringClicked() {
        startVisit();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_deliveredButton)
    protected void onDeliveredClicked() {
        sendStopVisit();
        Intent intent = new Intent(getContext(), ShopListActivity.class);
        startActivityForResult(intent, REQUEST_SHOP);
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_stopButton)
    protected void onStopClicked() {
        sendStopStop();
        mStop = null;
        mLastLocation = null;
        startIdle();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_vehicleLayout)
    protected void onVehicleClicked() {
        Intent intent = new Intent(getContext(), VehicleListActivity.class);
        startActivityForResult(intent, REQUEST_VEHICLE);
    }

    /**
     * Models
     */

    protected void resetRoute() {
        mRoute = mLab.getRoute();
        mRoute.setVehicleId(mLab.getVehicle().getId());
    }

    protected void resetStop() {
        mStop = new CStop();
        if (mLastLocation != null) {
            mStop.setLatitude(mLastLocation.getLatitude());
            mStop.setLongitude(mLastLocation.getLongitude());
        }
        mStop.setCrewId(mUser.getId());
        mStop.measureTime();
        if (mRoute != null) {
            mStop.setRouteId(mRoute.getId());
        }
    }

    protected void resetVisit() {
        mVisit = new Visit();
        if (mLastLocation != null) {
            mVisit.setLatitudeStart(mLastLocation.getLatitude());
            mVisit.setLongitudeStart(mLastLocation.getLongitude());
            mVisit.setLatitudeEnd(mLastLocation.getLatitude());
            mVisit.setLongitudeEnd(mLastLocation.getLongitude());
        }
        mVisit.measureTime();
    }


    /**
     * States
     **/

    protected void startIdle() {
        resetStats();
        goToState(TrackState.IDLE);
        stopTracking();
        hideAllViews();
        showIdleView();
    }

    protected void startWaitingLocation() {
        goToState(TrackState.WAITING_LOCATION);
        hideAllViews();
        showWaitingLocationView();
        startBackgroundServices();
    }

    protected void startMenu() {
        goToState(TrackState.DELIVERING);
        hideAllViews();
        showMenuView();
    }

    protected void startVisit() {
        goToState(TrackState.TRACKING);
        hideAllViews();
        showTrackingView();
        startTimer(TIMER_LENGTH);
        sendStartVisit();
        startBackgroundServices();
    }

    protected void checkIfWaitingForLocation() {
        if (mState == TrackState.WAITING_LOCATION) {
            sendStartStop();
            startVisit();
            /*if (mStop == null) {
                sendStartStop();
                startVisit();
            } else {
                startMenu();
            }*/
        }
    }

    /**
     * State views
     */

    protected void hideAllViews() {
        super.hideAllViews();
        mDeliveringButton.setVisibility(View.GONE);
        mDeliveredButton.setVisibility(View.GONE);
        mStopButton.setVisibility(View.GONE);
    }

    protected void showIdleView() {
        mStartButton.setVisibility(View.VISIBLE);
    }

    protected void showMenuView() {
        mDeliveringButton.setVisibility(View.VISIBLE);
        mStopButton.setVisibility(View.VISIBLE);
    }

    protected void showTrackingView() {
        mDeliveredButton.setVisibility(View.VISIBLE);
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
    }

    protected void stopTracking() {
        stopTimer();
        stopBackgroundServices();
    }

    /**
     * View handling
     */

    protected void updateStateView() {
        String label = "";
        switch ((TrackState) mState) {
            case IDLE:
                label = "idle";
                break;
            case WAITING_LOCATION:
                label = "waiting location";
                break;
            case DELIVERING:
                label = "delivering";
                break;
            case TRACKING:
                label = "tracking";
                break;
        }
        mStateTextView.setText(label);
    }

    protected void updateVehicleView() {
        mVehicleTextView.setText(mVehicle.getPlates());
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onLocationEvent(Location location) {
        updateStats(location);
        mLastLocation = location;
        checkIfWaitingForLocation();
    }

    /**
     * Vehicles
     */

    protected void sendAssignVehicle(Vehicle vehicle, User user) {
        RequestParams params = user.buildParams();
        params.put(Vehicle.JSON_WRAPPER, vehicle.toHashMap());
        /*mAPIFetch.post("routes/postAssign", params, new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    routeCreated(new Route(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
            }
        });*/
    }

    protected void routeCreated(Route route) {
        mRoute = route;
        mLab.setRoute(mRoute).saveRoute();
        startBackgroundServices();
    }

    /**
     * Stop
     */

    protected void sendStartStop() {
        resetStop();
        CreateCStopTask task = new CreateCStopTask(mStop);
        mNetworkTaskQueueWrapper.changeToNewQueue();
        mNetworkTaskQueueWrapper.addTask(task);
    }

    protected void sendStopStop() {
        mStop.measureTime();
        StopCStopTask task = new StopCStopTask(mStop);
        mNetworkTaskQueueWrapper.addTask(task);
    }

    /**
     * Visit
     */

    protected void sendStartVisit() {
        resetVisit();
        CreateVisitTask task = new CreateVisitTask(mVisit);
        mNetworkTaskQueueWrapper.addTask(task);
    }

    protected void sendStopVisit() {
        mVisit.measureTime();
        StopVisitTask task = new StopVisitTask(mVisit);
        mNetworkTaskQueueWrapper.addTask(task);
    }

    protected void startBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().startService(intent);
    }

    protected void stopBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().stopService(intent);
    }
}