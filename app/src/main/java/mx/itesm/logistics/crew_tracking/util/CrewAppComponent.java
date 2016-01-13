package mx.itesm.logistics.crew_tracking.util;

import javax.inject.Singleton;

import dagger.Component;
import edu.mit.lastmite.insight_library.fragment.BaseFragment;
import edu.mit.lastmite.insight_library.util.AppModule;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.crew_tracking.activity.CStopListActivity;
import mx.itesm.logistics.crew_tracking.activity.DeliveryNewActivity;
import mx.itesm.logistics.crew_tracking.activity.LoginActivity;
import mx.itesm.logistics.crew_tracking.activity.MainActivity;
import mx.itesm.logistics.crew_tracking.activity.RouteListActivity;
import mx.itesm.logistics.crew_tracking.activity.ShopListActivity;
import mx.itesm.logistics.crew_tracking.activity.VehicleListActivity;
import mx.itesm.logistics.crew_tracking.fragment.CStopListFragment;
import mx.itesm.logistics.crew_tracking.fragment.CStopShowFragment;
import mx.itesm.logistics.crew_tracking.fragment.DeliveryFormFragment;
import mx.itesm.logistics.crew_tracking.fragment.DeliveryListFragment;
import mx.itesm.logistics.crew_tracking.fragment.LoginFragment;
import mx.itesm.logistics.crew_tracking.fragment.RouteListFragment;
import mx.itesm.logistics.crew_tracking.fragment.ShopListFragment;
import mx.itesm.logistics.crew_tracking.fragment.TrackFragment;
import mx.itesm.logistics.crew_tracking.fragment.VehicleListFragment;
import mx.itesm.logistics.crew_tracking.fragment.VisitListFragment;
import mx.itesm.logistics.crew_tracking.service.CrewNetworkQueueService;
import mx.itesm.logistics.crew_tracking.service.LocationManagerService;
import mx.itesm.logistics.crew_tracking.service.NotificationProgressService;
import mx.itesm.logistics.crew_tracking.task.CreateCStopTask;

@Singleton
@Component(modules = {AppModule.class, CrewAppModule.class})
public interface CrewAppComponent extends ApplicationComponent {
    void inject(MainActivity activity);

    void inject(LoginActivity activity);

    void inject(ShopListActivity activity);

    void inject(DeliveryNewActivity activity);

    void inject(VehicleListActivity activity);

    void inject(CStopListActivity activity);

    void inject(RouteListActivity activity);

    void inject(BaseFragment fragment);

    void inject(TrackFragment fragment);

    void inject(CStopListFragment fragment);

    void inject(LocationManagerService service);

    void inject(NotificationProgressService service);

    void inject(CrewNetworkQueueService service);

    void inject(LoginFragment fragment);

    void inject(DeliveryFormFragment fragment);

    void inject(VehicleListFragment fragment);

    void inject(ShopListFragment fragment);

    void inject(CStopShowFragment fragment);

    void inject(VisitListFragment fragment);

    void inject(RouteListFragment fragment);

    void inject(DeliveryListFragment fragment);

    void inject(CreateCStopTask task);
}