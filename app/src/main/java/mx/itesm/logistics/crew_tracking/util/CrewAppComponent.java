package mx.itesm.logistics.crew_tracking.util;

import javax.inject.Singleton;

import dagger.Component;
import edu.mit.lastmite.insight_library.util.AppModule;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.crew_tracking.activity.LoginActivity;
import mx.itesm.logistics.crew_tracking.activity.MainActivity;
import mx.itesm.logistics.crew_tracking.fragment.DeliveryFormFragment;
import mx.itesm.logistics.crew_tracking.fragment.LoginFragment;
import mx.itesm.logistics.crew_tracking.fragment.ShopListFragment;
import mx.itesm.logistics.crew_tracking.fragment.TrackFragment;
import mx.itesm.logistics.crew_tracking.fragment.VehicleListFragment;
import mx.itesm.logistics.crew_tracking.service.LocationManagerService;

@Singleton
@Component(modules = {AppModule.class})
public interface CrewAppComponent extends ApplicationComponent {
    void inject(MainActivity activity);

    void inject(LoginActivity activity);

    void inject(TrackFragment fragment);

    void inject(LocationManagerService service);

    void inject(LoginFragment fragment);

    void inject(DeliveryFormFragment fragment);

    void inject(VehicleListFragment fragment);

    void inject(ShopListFragment fragment);
}