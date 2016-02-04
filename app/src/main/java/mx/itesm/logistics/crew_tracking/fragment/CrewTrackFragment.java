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

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.rey.material.app.SimpleDialog;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.activity.SingleFragmentActivity;
import edu.mit.lastmite.insight_library.event.TimerEvent;
import edu.mit.lastmite.insight_library.fragment.TrackFragment;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.User;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.model.Visit;
import edu.mit.lastmite.insight_library.service.TimerService;
import edu.mit.lastmite.insight_library.task.QueueHeaderTask;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.ColorTransformation;
import edu.mit.lastmite.insight_library.util.Storage;
import edu.mit.lastmite.insight_library.util.TextSpeaker;
import edu.mit.lastmite.insight_library.util.ViewUtils;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.activity.SettingsActivity;
import mx.itesm.logistics.crew_tracking.activity.ShopListActivity;
import mx.itesm.logistics.crew_tracking.activity.SyncListActivity;
import mx.itesm.logistics.crew_tracking.activity.VehicleListActivity;
import mx.itesm.logistics.crew_tracking.model.CStop;
import mx.itesm.logistics.crew_tracking.model.Loading;
import mx.itesm.logistics.crew_tracking.model.Return;
import edu.mit.lastmite.insight_library.model.VehicleType;
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueueWrapper;
import mx.itesm.logistics.crew_tracking.service.LocationManagerService;
import mx.itesm.logistics.crew_tracking.task.CreateCStopTask;
import mx.itesm.logistics.crew_tracking.task.CreateLoadingTask;
import mx.itesm.logistics.crew_tracking.task.CreateReturnTask;
import mx.itesm.logistics.crew_tracking.task.CreateRouteTask;
import mx.itesm.logistics.crew_tracking.task.CreateVisitTask;
import mx.itesm.logistics.crew_tracking.task.StopCStopTask;
import mx.itesm.logistics.crew_tracking.task.StopReturnTask;
import mx.itesm.logistics.crew_tracking.task.StopRouteTask;
import mx.itesm.logistics.crew_tracking.task.StopVisitTask;
import mx.itesm.logistics.crew_tracking.util.Api;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;
import mx.itesm.logistics.crew_tracking.util.Mode;
import mx.itesm.logistics.crew_tracking.util.Preferences;

public class CrewTrackFragment extends TrackFragment {
    public static final int REQUEST_VEHICLE = 0;
    public static final int REQUEST_SHOP = 1;

    public static final int PEDESTRIAN_COLOR = Color.rgb(96, 125, 139);
    public static final int GROUP_VEHICLE_COLOR = Color.rgb(51, 172, 113);
    public static final int SINGLE_VEHICLE_COLOR = Color.rgb(255, 61, 0);

    public static final long COLOR_ANIMATION_DURATION = 500;

    public enum TrackState implements State {
        IDLE,
        WAITING_LOCATION,
        TRACKING,
        TRACKING_PEDESTRIAN,
        TRACKING_DELIVERY,
        PAUSED,
        VISITING,
        DELIVERING,
        RETURNING,
        LOADING
    }

    @Inject
    protected Lab mLab;

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Api mApi;

    @Inject
    protected CrewNetworkTaskQueueWrapper mNetworkTaskQueueWrapper;

    @Inject
    protected Bus mBus;

    @Inject
    protected TextSpeaker mTextSpeaker;

    @Inject
    protected Storage mStorage;

    protected int mPreviousColor = PEDESTRIAN_COLOR;
    protected int mColor = PEDESTRIAN_COLOR;

    protected Mode mPreviousMode;
    protected Mode mMode = Mode.PEDESTRIAN;

    protected boolean mIsShowingButtons = false;

    protected User mUser;
    protected Vehicle mVehicle;
    protected Route mRoute;
    protected CStop mStop;
    protected Visit mVisit;
    protected Return mReturn;
    protected Loading mLoading;
    protected ArrayList<VehicleType> mVehicleTypes;
    protected VehicleType mVehicleType;

    protected boolean mShouldSyncQueue = false;

    @Bind(R.id.panelLayout)
    protected FrameLayout mPanelLayout;

    @Bind(R.id.track_contentLayout)
    protected LinearLayout mContentLayout;

    /**
     * Delivering
     */

    @Bind(R.id.track_visitingButton)
    protected FloatingActionButton mDeliveringButton;

    @Bind(R.id.track_deliveringLayout)
    protected FrameLayout mDeliveringLayout;

    /**
     * Delivered
     */

    @Bind(R.id.track_deliveringButton)
    protected FloatingActionButton mDeliveredButton;

    @Bind(R.id.track_deliveredLayout)
    protected FrameLayout mDeliveredLayout;

    /**
     * Stop
     */

    @Bind(R.id.track_stopButton)
    protected FloatingActionButton mStopButton;

    @Bind(R.id.track_stopLayout)
    protected FrameLayout mStopLayout;

    /**
     * Returning to truck
     */

    @Bind(R.id.track_returningButton)
    protected FloatingActionButton mReturningButton;

    @Bind(R.id.track_returningLayout)
    protected FrameLayout mReturningLayout;

    /**
     * Loading
     */

    @Bind(R.id.track_loadingButton)
    protected FloatingActionButton mLoadingButton;

    @Bind(R.id.track_loadingLayout)
    protected FrameLayout mLoadingLayout;

    /**
     * Boarding
     */

    @Bind(R.id.track_boardingButton)
    protected FloatingActionButton mBoardingButton;

    @Bind(R.id.track_boardingLayout)
    protected FrameLayout mBoardingLayout;

    @Bind(R.id.track_modeSpinner)
    protected Spinner mModeSpinner;

    @Bind(R.id.track_vehicleButton)
    protected Button mVehicleButton;

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
        mVehicleTypes = (ArrayList<VehicleType>) mLab.getVehicleTypes();

        resetRoute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);
        ButterKnife.bind(this, view);

        findTrackViews(view);
        findPanelLayout(view, R.id.track_slidingUpPanel);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startIdle();
        changeThemeColor(PEDESTRIAN_COLOR);
        inflateMapsFragment(R.id.track_mapLayout);
        mOverlayLayout.setAlpha(OVERLAY_OPACITY);
        populateModeSpinner();
        registerPaneListener();
        applyLabelsSettings();
        applyKPISettings();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_track, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem cancelItem = menu.findItem(R.id.track_menu_item_pause);

        boolean actionsVisible = mState == TrackState.TRACKING;
        cancelItem.setVisible(actionsVisible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.track_menu_item_pause:
                getPauseConfirmation();
                return true;
            case R.id.track_menu_item_sync: {
                Intent intent = new Intent(getContext(), SyncListActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.track_menu_item_settings: {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) return;

        switch (requestCode) {
            case REQUEST_VEHICLE:
                if (resultCode == Activity.RESULT_OK) {
                    mVehicle = (Vehicle) data.getSerializableExtra(VehicleListActivity.EXTRA_VEHICLE);
                    if (mLab.setVehicle(mVehicle).saveVehicle()) {
                        mShouldSyncQueue = true;
                    }
                }
                waitForLocation(new WaitForLocationCallback() {
                    @Override
                    public void onReceivedLocation(Location location) {
                        sendQueueHeader();
                        sendStartRoute();
                        startTracking();
                    }
                });
                break;
            case REQUEST_SHOP:
                onDeliveryCreated();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_startButton)
    protected void onStartClicked() {
        switch (mMode) {
            case PEDESTRIAN:
                waitForLocation(new WaitForLocationCallback() {
                    @Override
                    public void onReceivedLocation(Location location) {
                        mShouldSyncQueue = true;
                        sendQueueHeader();
                        sendStartRoute();
                        startTrackingPedestrian();
                    }
                });
                break;
            default:
                waitForLocation(new WaitForLocationCallback() {
                    @Override
                    public void onReceivedLocation(Location location) {
                        sendQueueHeader();
                        sendStartRoute();
                        startTracking();
                    }
                });
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_visitingButton)
    protected void onDeliveringClicked() {
        switch ((TrackState) mState) {
            case TRACKING:
                sendStartStop();
                startVisit();
                break;
            case LOADING:
                sendLoading();
            default:
                startVisit();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_deliveringButton)
    protected void onDeliveredClicked() {
        if (mState != TrackState.TRACKING_PEDESTRIAN) {
            sendStopVisit();
        }
        Intent intent = new Intent(getContext(), ShopListActivity.class);
        startActivityForResult(intent, REQUEST_SHOP);
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_stopButton)
    protected void onStopClicked() {
        switch ((TrackState) mState) {
            case DELIVERING:
                sendStopStop();
                mStop = null;
                mLastLocation = null;
                startTracking();
                break;
            case TRACKING_PEDESTRIAN:
            default:
                sendStopRoute();
                stopTracking();
                startIdle();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_vehicleButton)
    protected void onVehicleClicked() {
        Intent intent = new Intent(getContext(), VehicleListActivity.class);
        startActivityForResult(intent, REQUEST_VEHICLE);
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_returningButton)
    protected void onReturningClicked() {
        startReturning();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_loadingButton)
    protected void onLoadingClicked() {
        switch ((TrackState) mState) {
            case RETURNING:
                sendStopReturn();
                resetLoading();
            default:
                startLoading();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_boardingButton)
    protected void onBoardingClicked() {
        switch ((TrackState) mState) {
            case RETURNING:
                sendStopReturn();
            default:
                startTracking();
        }
    }

    /**
     * Mode & Spinner
     */

    protected void setSpinnerAdapter(ArrayList<ModeWrapper> list) {
        ModeSpinnerAdapter adapter = new ModeSpinnerAdapter(list);
        mModeSpinner.setAdapter(adapter);
        mModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Mode mode = convertTypeToMode(mVehicleTypes.get(position).getGroup());
                changeToMode(mode);
                hideAllViews();
                showIdleView();
                changeIdleViewForMode(mode);

                if (mMode == mPreviousMode) {
                    return;
                }

                changeThemeColor(getModeColor(mMode));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    protected int getModeResource(Mode mode) {
        switch (mode) {
            case PEDESTRIAN:
                return R.mipmap.ic_pedestrian;
            case GROUP_VEHICLE:
                return R.mipmap.ic_truck;
            case SINGLE_VEHICLE:
                return R.mipmap.ic_cycling;
            default:
                return R.mipmap.ic_cycling;
        }
    }

    protected void changeToMode(Mode mode) {
        mPreviousMode = mMode;
        mMode = mode;
    }

    protected void populateModeSpinner() {
        ArrayList<ModeWrapper> list = new ArrayList<>();

        Iterator<VehicleType> iterator = mVehicleTypes.iterator();
        while (iterator.hasNext()) {
            VehicleType type = iterator.next();
            Mode mode = convertTypeToMode(type.getGroup());
            ModeWrapper vehicle = new ModeWrapper(
                    getModeResource(mode),
                    getModeColor(mode),
                    type.getName()
            );
            list.add(vehicle);
        }

        setSpinnerAdapter(list);
    }

    protected int getModeColor(Mode mode) {
        switch (mode) {
            case PEDESTRIAN:
                return PEDESTRIAN_COLOR;
            case SINGLE_VEHICLE:
                return SINGLE_VEHICLE_COLOR;
            case GROUP_VEHICLE:
                return GROUP_VEHICLE_COLOR;
            default:
                return PEDESTRIAN_COLOR;
        }
    }

    protected Mode convertTypeToMode(int type) {
        switch (type) {
            case VehicleType.Group.WALKING:
                return Mode.PEDESTRIAN;
            case VehicleType.Group.VEHICLES:
                return Mode.GROUP_VEHICLE;
            case VehicleType.Group.INDIVIDUAL:
                return Mode.SINGLE_VEHICLE;
        }
        return null;
    }

    protected void changeIdleViewForMode(Mode mode) {
        switch (mode) {
            case PEDESTRIAN:
                mStartButton.setVisibility(View.VISIBLE);
                break;
            case GROUP_VEHICLE:
            case SINGLE_VEHICLE:
                if (checkForConnectivity(getView())) {
                    mVehicleButton.setVisibility(View.VISIBLE);
                } else {
                    mStartButton.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    protected boolean checkForConnectivity(View view) {
        if (!mAPIFetch.isNetworkAvailable()) {
            final Snackbar snackbar = Snackbar.make(view, getString(R.string.error_no_connectivity), Snackbar.LENGTH_INDEFINITE);
            snackbar
                    .setAction(getString(R.string.action_ok), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackbar.dismiss();
                        }
                    }).show();
            return false;
        }
        return true;
    }

    protected void onDeliveryCreated() {
        switch ((TrackState) mState) {
            case TRACKING_PEDESTRIAN:
                startTrackingPedestrian();
                break;
            default:
                resetStats();
                startMenu();
        }
    }

    protected void applyLabelsSettings() {
        if (mStorage.getSharedPreferences().getBoolean(Preferences.PREFERENCES_SHOW_LABELS, true)) {
            showLabels();
        } else {
            hideLabels();
        }
    }

    protected void applyKPISettings() {
        if (!mStorage.getSharedPreferences().getBoolean(Preferences.PREFERENCES_EXTRA_KPIS, false)) {
            hideSpeedKPIs();
        }
    }

    protected int convertModeToType(Mode mode) {
        switch (mode) {
            case PEDESTRIAN:
                return Route.Type.PEDESTRIAN;
            case GROUP_VEHICLE:
                return Route.Type.CREW;
            case SINGLE_VEHICLE:
                return Route.Type.INDIVIDUAL_VEHICLE;
            default:
                return Route.Type.CREW;
        }
    }

    /**
     * Dialogs
     */

    protected void getPauseConfirmation() {
        final SimpleDialog dialog = new SimpleDialog(getContext());
        dialog
                .message(getString(R.string.dialog_pause_message))
                .title(getString(R.string.dialog_pause_title))
                .positiveAction(getString(R.string.action_ok))
                .negativeAction(getString(R.string.action_cancel))
                .positiveActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startPause();
                        dialog.dismiss();
                    }
                })
                .negativeActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .show();
    }

    /**
     * Models
     */

    protected void resetRoute() {
        mRoute = mLab.getRoute();
        if (mLastLocation != null) {
            mRoute.setLatitude(mLastLocation.getLatitude());
            mRoute.setLongitude(mLastLocation.getLongitude());
        }

        if (mVehicleType != null) {
            mRoute.setVehicleTypeId(mVehicleType.getId());
        }

        mRoute.setType(convertModeToType(mMode));
        mRoute.setVehicleId(mLab.getVehicle().getId());
        mRoute.setCrewId(mUser.getId());
    }

    protected void resetStop() {
        mStop = new CStop();
        if (mLastLocation != null) {
            mStop.setLatitude(mLastLocation.getLatitude());
            mStop.setLongitude(mLastLocation.getLongitude());
        }
        mStop.setCrewId(mUser.getId());
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
    }

    protected void resetReturn() {
        mReturn = new Return();
        if (mLastLocation != null) {
            mReturn.setLatitude(mLastLocation.getLatitude());
            mReturn.setLongitude(mLastLocation.getLongitude());
        }
    }

    protected void resetLoading() {
        mLoading = new Loading();
        if (mLastLocation != null) {
            mLoading.setLatitude(mLastLocation.getLatitude());
            mLoading.setLongitude(mLastLocation.getLongitude());
        }
    }

    /**
     * States
     **/

    @Override
    public void goToState(State state) {
        super.goToState(state);
        if (mState == TrackState.TRACKING || mLastState == TrackState.TRACKING) {
            getActivity().supportInvalidateOptionsMenu();
        }
        if (mStorage.getSharedPreferences().getBoolean(Preferences.PREFERENCES_TEXT_TO_SPEECH, true)) {
            speakState((TrackState) state);
        }
    }

    protected void speakState(TrackState state) {
        String text = "";
        switch (state) {
            case IDLE:
                text = getString(R.string.state_idle_speak);
                break;
            case PAUSED:
                text = getString(R.string.state_paused_speak);
                break;
            case LOADING:
                text = getString(R.string.state_loading_speak);
                break;
            case RETURNING:
                text = getString(R.string.state_returning_speak);
                break;
            case WAITING_LOCATION:
                text = getString(R.string.state_waiting_speak);
                break;
            case VISITING:
                text = getString(R.string.state_delivering_speak);
                break;
            case TRACKING:
            case TRACKING_DELIVERY:
            case TRACKING_PEDESTRIAN:
                text = getString(R.string.state_tracking_speak);
                break;
            case DELIVERING:
                text = getString(R.string.state_delivered_speak);
                break;
        }
        mTextSpeaker.say(text);
    }

    protected void waitForLocation(WaitForLocationCallback callback) {
        mWaitForLocationCallback = callback;
        startWaitingLocation();
    }

    protected void startIdle() {
        runIdleActions();
        showIdleViews();
    }

    protected void runIdleActions() {
        goToState(TrackState.IDLE);
        resetStats();
        stopTracking();
        stopTimer();
    }

    protected void showIdleViews() {
        updatePanelShowingStatus();
        hideAllViews();
        showIdleView();
        changeIdleViewForMode(mMode);
    }

    protected void startWaitingLocation() {
        goToState(TrackState.WAITING_LOCATION);
        startBackgroundServices();

        updatePanelShowingStatus();
        hideAllViews();
        showWaitingLocationView();
    }

    protected void startTracking() {
        goToState(TrackState.TRACKING);
        startTimer();
        startBackgroundServices();

        if (mMode == Mode.GROUP_VEHICLE) {
            sendStartRoute();
        }

        updatePanelShowingButtons();
        hideAllViews();
        showTrackingView();
    }

    /**
     * Paused
     */

    protected void startPause() {
        runPausedActions();
        showPausedViews();
        stopTimer();
    }

    protected void runPausedActions() {
        goToState(TrackState.PAUSED);
    }

    protected void showPausedViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showPausedView();
    }

    protected void startDeliveryTracking() {
        goToState(TrackState.TRACKING_DELIVERY);
        startTimer();
        startBackgroundServices();
        sendQueueHeader();

        updatePanelShowingButtons();
        hideAllViews();
        showDeliveryTrackingView();
    }

    protected void startTrackingPedestrian() {
        goToState(TrackState.TRACKING_PEDESTRIAN);
        startTimer();
        resetSectionStats();
        startBackgroundServices();

        updatePanelShowingButtons();
        hideAllViews();
        showTrackingPedestrianView();
    }

    protected void startMenu() {
        goToState(TrackState.VISITING);

        updatePanelShowingButtons();
        hideAllViews();
        showDeliveryTrackingView();
    }

    protected void startVisit() {
        goToState(TrackState.DELIVERING);
        resetSectionStats();
        sendStartVisit();

        updatePanelShowingButtons();
        hideAllViews();
        showVisitingView();
    }

    protected void startReturning() {
        goToState(TrackState.RETURNING);
        resetSectionStats();
        sendStartReturn();

        updatePanelShowingButtons();
        hideAllViews();
        showReturningView();
    }

    protected void startLoading() {
        goToState(TrackState.LOADING);
        resetSectionStats();
        //sendStartVisit();

        updatePanelShowingButtons();
        hideAllViews();
        showLoadingView();
    }

    protected void checkIfWaitingForLocation(Location location) {
        if (mState == TrackState.WAITING_LOCATION) {
            mWaitForLocationCallback.onReceivedLocation(location);
        }
    }

    /**
     * Colors*
     */

    protected void updateActionButtonColors(int color) {
        ViewUtils.changeDrawableColor(getContext(), R.mipmap.ic_stop, color, mStopButton);
        ViewUtils.changeDrawableColor(getContext(), R.mipmap.ic_delivering, color, mDeliveringButton);
        ViewUtils.changeDrawableColor(getContext(), R.mipmap.ic_package, color, mDeliveredButton);
        ViewUtils.changeDrawableColor(getContext(), R.mipmap.ic_pedestrian, color, mReturningButton);
        ViewUtils.changeDrawableColor(getContext(), R.mipmap.ic_package_down, color, mLoadingButton);
        ViewUtils.changeDrawableColor(getContext(), R.mipmap.ic_truck, color, mBoardingButton);
    }


    protected void changeThemeColor(int color) {
        changeToColor(color);
        animateViewColor(mPanelLayout, mPreviousColor, mColor, COLOR_ANIMATION_DURATION);
        animateViewColor(((SingleFragmentActivity) getActivity()).getActionBarView(), mPreviousColor, mColor, COLOR_ANIMATION_DURATION);
        animateStatusBar(mPreviousColor, mColor, COLOR_ANIMATION_DURATION);
        updateActionButtonColors(mColor);
    }

    protected void changeToColor(int color) {
        mPreviousColor = mColor;
        mColor = color;
        mApi.setThemeColor(color);
    }

    protected void animateStatusBar(int fromColor, int toColor, long duration) {
        ValueAnimator colorAnimation = createColorAnimation(fromColor, toColor, duration);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                ((SingleFragmentActivity) getActivity())
                        .setDarkenedStatusBarColor(
                                (int) animator.getAnimatedValue()
                        );
            }
        });
        colorAnimation.start();
    }

    /**
     * Panel
     */

    public void showPanel() {
        showPanel(PANEL_DELAY);
    }

    public void hidePanel() {
        hidePanel(PANEL_DELAY);
    }

    protected void registerPaneListener() {
        mSlidingUpPanel.setPanelSlideListener(new SlidingUpPanelLayout.SimplePanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if (mIsShowingButtons) {
                    fadeContentLayout(slideOffset);
                }
            }
        });
    }

    protected void fadeContentLayout(float amount) {
        mContentLayout.setAlpha(amount);
    }

    public void updatePanelShowingStatus() {
        mIsShowingButtons = false;
        setFloatingButtonsSpace(0);
        setPanelHeight(mHelper.dpToPx(PANEL_STATUS_HEIGHT));
    }

    protected void updatePanelShowingButtons() {
        mIsShowingButtons = true;
        int actionHeight = mHelper.dpToPx(PANEL_ACTION_HEIGHT);
        setPanelHeight(actionHeight);
        setFloatingButtonsSpace(actionHeight);
    }

    protected void setFloatingButtonsSpace(int space) {
        setBottomPadding(mContentLayout, space);
    }


    /**
     * TrackState views
     */

    protected void hideAllViews() {
        super.hideAllViews();
        mDeliveringLayout.setVisibility(View.GONE);
        mDeliveredLayout.setVisibility(View.GONE);
        mStopLayout.setVisibility(View.GONE);
        mModeSpinner.setVisibility(View.GONE);
        mStatusLayout.setVisibility(View.GONE);
        mVehicleButton.setVisibility(View.GONE);
        mReturningLayout.setVisibility(View.GONE);
        mLoadingLayout.setVisibility(View.GONE);
        mBoardingLayout.setVisibility(View.GONE);
        mPausedTextView.setVisibility(View.GONE);
    }

    protected void showIdleView() {
        mModeSpinner.setVisibility(View.VISIBLE);
    }

    protected void showWaitingLocationView() {
        super.showWaitingLocationView();
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void showMenuView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mDeliveringLayout.setVisibility(View.VISIBLE);
        mStopLayout.setVisibility(View.VISIBLE);
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void showPausedView() {
        mPausedTextView.setVisibility(View.VISIBLE);
        mStopLayout.setVisibility(View.VISIBLE);
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void showTrackingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mDeliveringLayout.setVisibility(View.VISIBLE);
        mStopLayout.setVisibility(View.VISIBLE);
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void showDeliveryTrackingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mDeliveringLayout.setVisibility(View.VISIBLE);
        mReturningLayout.setVisibility(View.VISIBLE);
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void showTrackingPedestrianView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mDeliveredLayout.setVisibility(View.VISIBLE);
        mStopLayout.setVisibility(View.VISIBLE);
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void showVisitingView() {
        mDeliveredLayout.setVisibility(View.VISIBLE);
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void showReturningView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mBoardingLayout.setVisibility(View.VISIBLE);
        mLoadingLayout.setVisibility(View.VISIBLE);
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void showLoadingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mDeliveringLayout.setVisibility(View.VISIBLE);
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    protected void stopTracking() {
        stopTimer();
        stopBackgroundServices();
    }

    /**
     * View handling
     */

    @Override
    protected void updateStateView() {
        String label = "";
        switch ((TrackState) mState) {
            case IDLE:
                label = getString(R.string.state_idle);
                break;
            case PAUSED:
                label = getString(R.string.state_paused);
                break;
            case LOADING:
                label = getString(R.string.state_loading);
                break;
            case RETURNING:
                label = getString(R.string.state_returning);
                break;
            case WAITING_LOCATION:
                label = getString(R.string.state_waiting);
                break;
            case VISITING:
                label = getString(R.string.state_delivering_action);
                break;
            case TRACKING:
            case TRACKING_DELIVERY:
            case TRACKING_PEDESTRIAN:
                label = getString(R.string.state_tracking);
                break;
            case DELIVERING:
                label = getString(R.string.state_delivering);
                break;
        }
        mStateTextView.setText(label);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onLocationEvent(Location location) {
        updateStats(location);
        mLastLocation = location;
        checkIfWaitingForLocation(location);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onTimerEvent(TimerEvent event) {
        super.onTimerEvent(event);
    }

    /**
     * Information header
     */

    protected void sendQueueHeader() {
        QueueHeaderTask task = new QueueHeaderTask(createQueueHeader(), mShouldSyncQueue);
        mNetworkTaskQueueWrapper.changeToNewQueue();
        mNetworkTaskQueueWrapper.addTask(task);
    }


    /**
     * Route
     */

    protected void sendStartRoute() {
        resetRoute();
        mRoute.measureTime();
        CreateRouteTask task = new CreateRouteTask(mRoute);
        mNetworkTaskQueueWrapper.addTask(task);
        startBackgroundServices();
    }

    protected void sendStopRoute() {
        mRoute.measureTime();
        StopRouteTask task = new StopRouteTask(mRoute);
        mNetworkTaskQueueWrapper.addTask(task);
        stopBackgroundServices();
        resetRoute();
    }

    /**
     * Stop
     */

    protected void sendStartStop() {
        resetStop();
        CreateCStopTask task = new CreateCStopTask(mStop);
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

    /**
     * Returns
     */

    protected void sendStartReturn() {
        resetReturn();
        CreateReturnTask task = new CreateReturnTask(mReturn);
        mNetworkTaskQueueWrapper.addTask(task);
    }

    protected void sendStopReturn() {
        mReturn.measureTime();
        StopReturnTask task = new StopReturnTask(mReturn);
        mNetworkTaskQueueWrapper.addTask(task);
    }

    /**
     * Loadings
     */

    protected void sendLoading() {
        mLoading.measureTime();
        CreateLoadingTask task = new CreateLoadingTask(mLoading);
        mNetworkTaskQueueWrapper.addTask(task);
    }

    protected HashMap<String, Object> createQueueHeader() {
        HashMap<String, Object> data = new HashMap<>();
        data.put(Preferences.PREFERENCES_MODE, mMode);
        data.put(Preferences.PREFERENCES_START_TIME, System.currentTimeMillis());
        data.put(Preferences.PREFERENCES_END_TIME, System.currentTimeMillis());
        data.put(Preferences.PREFERENCES_LATITUDE, mLastLocation.getLatitude());
        data.put(Preferences.PREFERENCES_LONGITUDE, mLastLocation.getLongitude());
        return data;
    }

    private class ModeWrapper {
        private String mName;
        private int mColor;
        private int mImageSource;

        ModeWrapper(int imageSource, int color, String name) {
            mImageSource = imageSource;
            mColor = color;
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public int getImageSource() {
            return mImageSource;
        }

        public int getColor() {
            return mColor;
        }
    }

    private class ModeSpinnerAdapter extends ArrayAdapter<ModeWrapper> {
        public ModeSpinnerAdapter(ArrayList<ModeWrapper> modeWrappers) {
            super(getActivity(), 0, modeWrappers);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.spinner_dropdown_item_type, null);
            }

            ModeWrapper wrapper = getItem(position);
            updateView(convertView, wrapper);

            return convertView;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.spinner_item_type, null);
            }

            ModeWrapper wrapper = getItem(position);
            updateView(convertView, wrapper);

            return convertView;
        }

    }

    private void updateView(View view, ModeWrapper wrapper) {

        TextView nameText = (TextView) view.findViewById(android.R.id.text1);
        nameText.setText(wrapper.getName());

        ImageView imageView = (ImageView) view.findViewById(R.id.item_mode_iconImageView);

        Picasso.with(getActivity())
                .load(wrapper.getImageSource())
                .transform(new ColorTransformation(wrapper.getColor()))
                .into(imageView);
    }

    /**
     * Services
     */

    protected void startBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().startService(intent);
    }

    protected void stopBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().stopService(intent);
    }

    protected void startTimer() {
        Intent intent = new Intent(getActivity(), TimerService.class);
        getActivity().startService(intent);
    }

    protected void stopTimer() {
        Intent intent = new Intent(getActivity(), TimerService.class);
        getActivity().stopService(intent);
    }
}