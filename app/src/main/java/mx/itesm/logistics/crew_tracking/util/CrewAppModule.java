package mx.itesm.logistics.crew_tracking.util;

import android.app.Application;

import com.google.gson.Gson;
import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import edu.mit.lastmite.insight_library.util.Storage;
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueue;
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueueWrapper;

@Module
public class CrewAppModule {

    protected Application mApplication;

    public CrewAppModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    Lab provideLab(Application application) {
        return new Lab(application);
    }

    @Provides
    @Singleton
    CrewNetworkTaskQueueWrapper provideCrewNetworkTaskQueueWrapper(Application application, Gson gson, Bus bus) {
        return new CrewNetworkTaskQueueWrapper(application, gson, bus);
    }

    @Provides
    @Singleton
    Api provideApi(Application application, Lab lab, Storage storage) {
        return new Api(application, lab, storage);
    }
}