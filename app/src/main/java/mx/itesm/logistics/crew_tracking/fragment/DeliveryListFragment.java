package mx.itesm.logistics.crew_tracking.fragment;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueueWrapper;
import mx.itesm.logistics.crew_tracking.task.CreateDeliveryTask;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;

public class DeliveryListFragment extends FragmentResponder implements ListView.OnItemClickListener {

    public static final String TAG = "ShopListFragment";

    @ServiceConstant
    public static String EXTRA_CARD;

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
        mShopAdapter = new ShopAdapter(mShops);

        mNearby = new ArrayList<>();
        mNearbyAdapter = new ShopAdapter(mNearby);

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
                sendDelivery((Delivery) data.getSerializableExtra(DeliveryNewActivity.EXTRA_DELIVERY));
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

    protected void sendDelivery(Delivery delivery) {
        if (delivery != null) {
            CreateDeliveryTask task = new CreateDeliveryTask(delivery);
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

        FrameLayout.LayoutParams shopLayoutParams = (FrameLayout.LayoutParams) mShopListsLayout.getLayoutParams();
        shopLayoutParams.setMargins(0, height, 0, 0);
        mShopListsLayout.setLayoutParams(shopLayoutParams);
    }

    protected void loadShops() {
        mShops.clear();
        mAPIFetch.get("shops/getShops", null, new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray vehicles = response.getJSONArray(Shop.JSON_WRAPPER + "s");
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

        public ShopAdapter(ArrayList<Shop> cards) {
            super(getActivity(), 0, cards);
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

            return convertView;
        }
    }

}

