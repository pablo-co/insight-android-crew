package mx.itesm.logistics.crew_tracking;


import edu.mit.lastmite.insight_library.BaseLibrary;
import edu.mit.lastmite.insight_library.util.AppModule;
import mx.itesm.logistics.crew_tracking.util.DaggerCrewAppComponent;

public class CrewTrackingApplication extends BaseLibrary {

    @Override
    protected void createComponent() {
        mComponent = DaggerCrewAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
        mComponent.inject(this);
    }
}