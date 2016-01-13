package mx.itesm.logistics.crew_tracking.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import edu.mit.lastmite.insight_library.activity.SingleFragmentActivity;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.fragment.DeliveryFormFragment;
import mx.itesm.logistics.crew_tracking.fragment.RouteListFragment;
import mx.itesm.logistics.crew_tracking.fragment.VehicleListFragment;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;

public class RouteListActivity extends SingleFragmentActivity implements TargetListener {

    public static final String EXTRA_ROUTE = "com.gruporaido.tasker.extra_route";

    public static final int REQUEST_VEHICLE = 0;
    public static final int REQUEST_ROUTE = 1;

    @Override
    protected Fragment createFragment() {
        VehicleListFragment fragment = new VehicleListFragment();
        fragment.setTargetListener(this, REQUEST_VEHICLE);
        return fragment;
    }

    @Override
    public void injectActivity(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(getString(R.string.vehicle_list_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void onResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != TargetListener.RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_VEHICLE:
                final Vehicle vehicle = (Vehicle) data.getSerializableExtra(VehicleListFragment.EXTRA_VEHICLE);
                inflateFragment(R.id.fragmentContainer, new Helper.FragmentCreator() {
                    @Override
                    public Fragment createFragment() {
                        RouteListFragment fragment = RouteListFragment.newInstance(vehicle);
                        fragment.setTargetListener(RouteListActivity.this, REQUEST_ROUTE);
                        return fragment;
                    }
                }, R.animator.no_animation, R.animator.no_animation, true);
                break;
            case REQUEST_ROUTE:
                Route route = (Route) data.getSerializableExtra(RouteListActivity.EXTRA_ROUTE);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_ROUTE, route);
                setResult(Activity.RESULT_OK, intent);
                finish();
        }
    }
}
