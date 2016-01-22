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
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.rey.material.widget.ProgressView;
import com.rey.material.widget.TextView;
import com.squareup.otto.Bus;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.annotation.ServiceConstant;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.event.ClearSectionTimerEvent;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.fragment.InsightMapsFragment;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.Delivery;
import edu.mit.lastmite.insight_library.model.Shop;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import edu.mit.lastmite.insight_library.util.ServiceUtils;
import edu.mit.lastmite.insight_library.view.NestedListView;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.activity.DeliveryNewActivity;
import mx.itesm.logistics.crew_tracking.model.CDelivery;
import mx.itesm.logistics.crew_tracking.model.CShop;
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueueWrapper;
import mx.itesm.logistics.crew_tracking.task.CreateCDeliveryTask;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;

public class ShopListFragment extends FragmentResponder implements ListView.OnItemClickListener {

    public static final String TAG = "ShopListFragment";

    @ServiceConstant
    public static String EXTRA_CARD;

    @ServiceConstant
    public static String EXTRA_LATITUDE;

    @ServiceConstant
    public static String EXTRA_LONGITUDE;


    static {
        ServiceUtils.populateConstants(ShopListFragment.class);
    }

    public static final int REQUEST_NEW = 0;
    public static final double MAP_PERCENTAGE = 0.5;
    public static final int MAP_OFFSET = 32;

    @Inject
    protected CrewNetworkTaskQueueWrapper mQueueWrapper;

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Bus mBus;

    @Inject
    protected Helper mHelper;

    /**
     * Layouts
     **/

    @Bind(R.id.list_shop_rootLayout)
    protected FrameLayout mRootLayout;

    @Bind(R.id.shopListsLayout)
    protected FrameLayout mShopListsLayout;


    /**
     * Shops
     **/

    protected Shop mShop;
    protected ArrayList<Shop> mShops;
    protected ShopAdapter mShopAdapter;
    protected double mLatitude;
    protected double mLongitude;

    @Bind(R.id.shopsListView)
    protected NestedListView mShopsListView;

    @Bind(R.id.shopsLoadingProgressView)
    protected ProgressView mShopsLoadingProgressView;

    @Bind(R.id.shopsEmptyView)
    protected View mShopsEmptyView;


    /**
     * Nearby
     **/

    protected ArrayList<Shop> mNearby;
    protected ShopAdapter mNearbyAdapter;

    @Bind(R.id.nearbyListView)
    protected NestedListView mNearbyListView;

    @Bind(R.id.nearbyLoadingProgressView)
    protected ProgressView mNearbyLoadingProgressView;

    @Bind(R.id.nearbyEmptyView)
    protected View mNearbyEmptyView;

    @Bind(R.id.list_shop_mapLayout)
    protected FrameLayout mMapLayout;

    public static ShopListFragment newInstance(float latitude, float longitude) {
        Bundle arguments = new Bundle();
        arguments.putDouble(EXTRA_LATITUDE, latitude);
        arguments.putDouble(EXTRA_LONGITUDE, longitude);

        ShopListFragment fragment = new ShopListFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * Map
     **/

    @Override
    public void injectFragment(ApplicationComponent component) {
        super.injectFragment(component);
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mShops = new ArrayList<>();
        mShopAdapter = new ShopAdapter(mShops, false);

        mNearby = new ArrayList<>();
        mNearbyAdapter = new ShopAdapter(mNearby, true);

        mLatitude = getArguments().getDouble(EXTRA_LATITUDE);
        mLongitude = getArguments().getDouble(EXTRA_LONGITUDE);

        mBus.register(this);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_list_shop, parent, false);
        ButterKnife.bind(this, view);

        mShopsListView.setEmptyView(mShopsEmptyView);
        mShopsListView.setAdapter(mShopAdapter);

        mNearbyListView.setEmptyView(mNearbyLoadingProgressView);
        mNearbyListView.setAdapter(mNearbyAdapter);
        mNearbyListView.setOnItemClickListener(this);

        checkForConnectivity(view);
        loadShops();
        restartTimer();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHelper.inflateFragment(getChildFragmentManager(), R.id.list_shop_mapLayout, new Helper.FragmentCreator() {
            @Override
            public Fragment createFragment() {
                return InsightMapsFragment.newInstance(InsightMapsFragment.Flags.DRAW_MARKER);
            }
        }, R.animator.no_animation, R.animator.no_animation);

        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mRootLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                adjustLayoutPositioning();
            }
        });
        restartTimer();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mShops.isEmpty()) {
            mShopsLoadingProgressView.setVisibility(View.GONE);
            mNearbyLoadingProgressView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_shop_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.shop_menu_item_close:
                sendResult(TargetListener.RESULT_OK, null);
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
            case REQUEST_NEW:
                sendDelivery((CDelivery) data.getSerializableExtra(DeliveryNewActivity.EXTRA_DELIVERY));
                restartTimer();
                if (mShop != null) {
                    mShops.add(mShop);
                    mNearby.remove(mShop);
                    notifyNearbyList();
                    notifyShopList();
                    mShop = null;
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        mShop = (Shop) adapterView.getAdapter().getItem(position);
        launchNewDelivery();
    }

    @OnClick(R.id.newDeliveryButton)
    protected void onActionClick() {
        launchNewDelivery();
    }

    protected void restartTimer() {
        mBus.post(new ClearSectionTimerEvent());
    }

    protected void sendDelivery(CDelivery delivery) {
        if (delivery != null) {
            CreateCDeliveryTask task = new CreateCDeliveryTask(delivery);
            mQueueWrapper.addTask(task);
        }
    }

    protected void notifyNearbyList() {
        mNearbyAdapter.notifyDataSetChanged();
    }

    protected void notifyShopList() {
        mShopAdapter.notifyDataSetChanged();
    }

    protected void adjustLayoutPositioning() {
        int height = (int) (mRootLayout.getHeight() * MAP_PERCENTAGE) - mHelper.dpToPx(MAP_OFFSET);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        layoutParams.setMargins(0, 0, 0, height);
        mMapLayout.setLayoutParams(layoutParams);

        mShopListsLayout.setPadding(0,height, 0, 0);
    }

    protected void loadShops() {
        mShops.clear();
        CShop shop = new CShop();
        shop.setLatitude(mLatitude);
        shop.setLongitude(mLongitude);
        mAPIFetch.get("cshops/getCshops", shop.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d(TAG, response.toString());
                    JSONArray vehicles = response.getJSONArray(CShop.JSON_WRAPPER + "s");
                    addShopsFromJSON(vehicles);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
                mShopsLoadingProgressView.setVisibility(View.GONE);
                mShopsListView.setEmptyView(mShopsEmptyView);
            }
        });
    }

    protected void addShopsFromJSON(JSONArray response) {
        try {
            for (int i = 0; i < response.length(); ++i) {
                Shop shop = new Shop(response.getJSONObject(i));
                shop.setLatitude(Math.random() % 120 - 120);
                shop.setLongitude(Math.random() % 120 - 120);
                shop.setDistance(Math.random() * i * 1000);
                mNearby.add(shop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mNearbyAdapter.notifyDataSetChanged();
        mNearbyLoadingProgressView.setVisibility(View.GONE);
        mNearbyListView.setEmptyView(mNearbyEmptyView);
    }

    protected boolean checkForConnectivity(View view) {
        if (!mAPIFetch.isNetworkAvailable()) {
            Snackbar.make(view, getString(R.string.error_no_connectivity), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.action_ok), null).show();
            return false;
        }
        return true;
    }

    protected void launchNewDelivery() {
        Intent intent = new Intent(getActivity(), DeliveryNewActivity.class);
        startActivityForResult(intent, REQUEST_NEW);
    }

    protected void sendResult(int resultCode, Shop shop) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_CARD, (Parcelable) shop);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }

    private class ShopAdapter extends ArrayAdapter<Shop> {

        protected boolean mShowIcon;

        public ShopAdapter(ArrayList<Shop> cards, boolean showIcon) {
            super(getActivity(), 0, cards);
            mShowIcon = showIcon;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_shop, null);
            }

            Shop shop = getItem(position);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.item_shop_nameTextView);
            nameTextView.setText(shop.getName());

            TextView addressTextView = (TextView) convertView.findViewById(R.id.item_shop_addressTextView);
            addressTextView.setText("Avenida de los Bosques #45, Alvaro Obregon, Edo. de Mex.");

            TextView distanceTextView = (TextView) convertView.findViewById(R.id.item_shop_distanceTextView);
            distanceTextView.setText(mHelper.formatDouble(shop.getDistance() / 1000.0));

            FloatingActionButton iconView = (FloatingActionButton) convertView.findViewById(R.id.item_shop_iconView);
            if (!mShowIcon) {
                iconView.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

}

