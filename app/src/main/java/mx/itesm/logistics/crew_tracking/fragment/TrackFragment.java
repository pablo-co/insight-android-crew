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
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TableLayout;

import com.loopj.android.http.RequestParams;
import com.rey.material.widget.Button;
import com.rey.material.widget.TextView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.apache.http.Header;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.fragment.InsightMapsFragment;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.CStop;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.User;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.activity.ShopListActivity;
import mx.itesm.logistics.crew_tracking.activity.VehicleListActivity;
import mx.itesm.logistics.crew_tracking.service.LocationManagerService;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;

public class TrackFragment extends FragmentResponder {

    public static final int REQUEST_VEHICLE = 0;
    public static final int REQUEST_SHOP = 1;

    protected static final int TIMER_LENGTH = 1000;
    protected static final float OVERLAY_OPACITY = 0.35f;

    public enum State {
        IDLE,
        WAITING_LOCATION,
        DELIVERING,
        TRACKING
    }

    @Inject
    protected Bus mBus;

    @Inject
    protected APIFetch mAPIFetch;

    protected CountDownTimer mCountDownTimer;
    protected int times = 0;
    protected float mAcumDistance = 0.0f;

    protected State mState = State.IDLE;
    protected Location mLastLocation;
    protected BigDecimal mAcumSpeed = new BigDecimal(0);
    protected BigDecimal mSpeedCount = new BigDecimal(0);

    protected User mUser;
    protected Vehicle mVehicle;
    protected Route mRoute;
    protected CStop mStop;


    @Bind(R.id.track_startButton)
    protected Button mStartButton;

    @Bind(R.id.track_timeTextView)
    protected TextView mTimeTextView;

    @Bind(R.id.track_overlayLayout)
    protected FrameLayout mOverlayLayout;

    @Bind(R.id.track_stateTextView)
    protected TextView mStateTextView;

    @Bind(R.id.track_actionButton)
    protected Button mActionButton;

    @Bind(R.id.track_distanceTextView)
    protected TextView mDistanceTextView;

    @Bind(R.id.track_speedTextView)
    protected TextView mSpeedTextView;

    @Bind(R.id.track_statsLayout)
    protected TableLayout mStatsLayout;

    @Bind(R.id.track_averageSpeedTextView)
    protected TextView mAverageSpeedTextView;

    @Bind(R.id.track_vehicleTextView)
    protected TextView mVehicleTextView;

    @Bind(R.id.track_waitingLocationTextView)
    protected TextView mWaitingLocationTextView;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mUser = Lab.get(getContext()).getUser();
        mVehicle = Lab.get(getContext()).getVehicle();

        resetStop();
        resetRoute();

        mBus.register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);
        ButterKnife.bind(this, view);

        mOverlayLayout.setAlpha(OVERLAY_OPACITY);
        startIdle();
        updateVehicleView();

        Helper.get(getContext()).inflateFragment(getChildFragmentManager(), R.id.track_mapLayout, new Helper.FragmentCreator() {
            @Override
            public Fragment createFragment() {
                return InsightMapsFragment.newInstance(InsightMapsFragment.Flags.DRAW_MARKER |
                        InsightMapsFragment.Flags.DRAW_PATH |
                        InsightMapsFragment.Flags.ROTATE_WITH_DEVICE);
            }
        }, R.animator.no_animation, R.animator.no_animation);

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
            case R.id.track_menu_item_stop:
                startIdle();
                return true;
            case R.id.track_menu_item_logout:
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
                Lab.get(getContext()).setVehicle(mVehicle).saveVehicle();
                updateVehicleView();
                break;
            case REQUEST_SHOP:
                sendStopTracking();
                resetStats();
                startWaitingLocation();
                break;
        }
    }


    @OnClick(R.id.track_startButton)
    protected void onStartClicked() {
        if (mLastLocation != null) {
            startDelivering();
        } else {
            startWaitingLocation();
        }
    }

    @OnClick(R.id.track_actionButton)
    protected void onActionClicked() {
        switch (mState) {
            case DELIVERING:
                sendStartTracking();
                startTracking();
                break;
            case TRACKING:
                Intent intent = new Intent(getContext(), ShopListActivity.class);
                startActivityForResult(intent, REQUEST_SHOP);
        }
    }

    @OnClick(R.id.track_vehicleLayout)
    protected void onVehicleClicked() {
        Intent intent = new Intent(getContext(), VehicleListActivity.class);
        startActivityForResult(intent, REQUEST_VEHICLE);
    }

    /**
     * Models
     */

    protected void resetRoute() {
        mRoute = Lab.get(getContext()).getRoute();
        mRoute.setVehicleId(Lab.get(getContext()).getVehicle().getId());
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


    /**
     * States
     **/

    protected void startIdle() {
        resetStats();
        goToState(State.IDLE);
        stopTracking();
        hideAllViews();
        showIdleView();
    }

    protected void startWaitingLocation() {
        goToState(State.WAITING_LOCATION);
        hideAllViews();
        showWaitingLocationView();
        startBackgroundServices();
    }

    protected void startDelivering() {
        goToState(State.DELIVERING);
        hideAllViews();
        showDeliveringView();
        startBackgroundServices();
    }

    protected void startTracking() {
        goToState(State.TRACKING);
        hideAllViews();
        showTrackingView();
        startTimer(TIMER_LENGTH);
        startBackgroundServices();
    }

    protected void checkIfWaitingForLocation() {
        if (mState == State.WAITING_LOCATION) {
            startDelivering();
        }
    }

    /**
     * State views
     */

    protected void hideAllViews() {
        mStatsLayout.setVisibility(View.GONE);
        mStartButton.setVisibility(View.GONE);
        mTimeTextView.setVisibility(View.GONE);
        mActionButton.setVisibility(View.GONE);
        mWaitingLocationTextView.setVisibility(View.GONE);
    }

    protected void showIdleView() {
        Animation fadeInAnimation = createFadeInAnimation(200);

        mStartButton.setVisibility(View.VISIBLE);
        mOverlayLayout.setVisibility(View.VISIBLE);

        fadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    protected void showWaitingLocationView() {
        Animation fadeOutAnimation = createFadeOutAnimation(200);

        mWaitingLocationTextView.setVisibility(View.VISIBLE);

        mOverlayLayout.startAnimation(fadeOutAnimation);

        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mOverlayLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    protected void showDeliveringView() {
        Animation fadeOutAnimation = createFadeOutAnimation(200);

        mActionButton.setVisibility(View.VISIBLE);

        mOverlayLayout.startAnimation(fadeOutAnimation);

        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mOverlayLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    protected void showTrackingView() {
        mActionButton.setVisibility(View.VISIBLE);
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
    }

    protected void stopTracking() {
        mStartButton.setVisibility(View.VISIBLE);
        stopTimer();
        //sendResult(TargetListener.RESULT_OK);
        stopBackgroundServices();

        Animation fadeInAnimation = createFadeInAnimation(200);

        mOverlayLayout.setVisibility(View.VISIBLE);
        mOverlayLayout.startAnimation(fadeInAnimation);
    }

    /**
     * View handling
     */

    protected void goToState(State state) {
        mState = state;
        updateStateView();
    }

    protected void updateStateView() {
        String label = "";
        switch (mState) {
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

    /**
     * General utilities
     */

    protected Animation createFadeInAnimation(int duration) {
        Animation fadeIn = new AlphaAnimation(OVERLAY_OPACITY, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(duration);

        return fadeIn;
    }

    protected Animation createFadeOutAnimation(int duration) {
        Animation fadeOut = new AlphaAnimation(1, OVERLAY_OPACITY);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(duration);

        return fadeOut;
    }

    @Subscribe
    public void onLocationEvent(Location location) {
        updateStats(location);
        checkIfWaitingForLocation();
    }

    protected void updateStats(Location location) {
        if (mLastLocation != null) {
            android.location.Location lastLocation = new android.location.Location("");
            lastLocation.setLatitude(mLastLocation.getLatitude());
            lastLocation.setLongitude(mLastLocation.getLongitude());

            android.location.Location newLocation = new android.location.Location("");
            newLocation.setLatitude(location.getLatitude());
            newLocation.setLongitude(location.getLongitude());

            /* Calculate distance */
            float distanceInMeters = lastLocation.distanceTo(newLocation);
            mAcumDistance += distanceInMeters / 1000.0f;
            String distance = Helper.get(getActivity()).formatDouble(mAcumDistance);
            mDistanceTextView.setText(distance + " km");

            /* Calculate speed */
            String speed = Helper.get(getActivity()).formatDouble(location.getSpeed());
            mSpeedTextView.setText(speed + " kph");

            /* Average speed */
            mAcumSpeed = mAcumSpeed.add(new BigDecimal(location.getSpeed()));
            mSpeedCount = mSpeedCount.add(new BigDecimal(1));
            String averageSpeed = Helper.get(getActivity()).formatDouble(mAcumSpeed.divide(mSpeedCount, 2, RoundingMode.HALF_UP).doubleValue());
            mAverageSpeedTextView.setText(averageSpeed + " kph");
        }
        mLastLocation = location;

    }

    protected void updateTime() {
        mTimeTextView.setText(secondsToString(times));
    }

    protected void resetStats() {
        mAcumSpeed = new BigDecimal(0);
        mSpeedCount = new BigDecimal(0);
        mAcumDistance = 0;
        times = 0;
    }

    private String secondsToString(int time) {
        int mins = time / 60;
        int secs = time % 60;

        String strMin = String.format("%02d", mins);
        String strSec = String.format("%02d", secs);
        return String.format("%s:%s", strMin, strSec);
    }

    protected void startTimer(final int time) {
        stopTimer();
        mCountDownTimer = new CountDownTimer(time, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                times++;
                updateTime();
                startTimer(time);
            }
        };
        mCountDownTimer.start();
    }

    protected void stopTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    /**
     * Vehicles
     */

    protected void sendAssignVehicle(Vehicle vehicle, User user) {
        RequestParams params = user.buildParams();
        params.put(Vehicle.JSON_WRAPPER, vehicle.toHashMap());
        mAPIFetch.post("routes/postAssign", params, new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
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
        });
    }

    protected void routeCreated(Route route) {
        mRoute = route;
        Lab.get(getContext()).setRoute(mRoute).saveRoute();
        startBackgroundServices();
    }

    /**
     * Stop
     */

    protected void sendStartTracking() {
        resetStop();
        RequestParams params = mStop.buildParams();
        Log.d("startTracking", params.toString());
        mAPIFetch.post("cstops/postInitialcstop", params, new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    stopCreated(new CStop(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
            }
        });
    }

    protected void sendStopTracking() {
        mStop.measureTime();
        RequestParams params = mStop.buildParams();
        params.put(User.JSON_ID, mUser.getId());
        mAPIFetch.post("cstops/postEndcstop", params, new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    stopEnded(new CStop(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
            }
        });
    }


    protected void stopCreated(CStop stop) {
        mStop = stop;
    }

    protected void stopEnded(CStop stop) {
        resetStop();
    }

    protected void startBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().startService(intent);
    }

    protected void stopBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().stopService(intent);
    }

    private void sendResult(int resultCode) {
        if (getTargetListener() == null) return;

        getTargetListener().onResult(getRequestCode(), resultCode, null);
    }
}