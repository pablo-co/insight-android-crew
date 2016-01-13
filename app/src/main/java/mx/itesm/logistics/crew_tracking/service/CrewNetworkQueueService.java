package mx.itesm.logistics.crew_tracking.service;

import android.util.Log;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.service.NetworkQueueService;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueue;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;

public class CrewNetworkQueueService extends NetworkQueueService {
    @Override
    public void injectService(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    /** Warning use dependency injection and copy the reference to local variables,
     * otherwise you might end up with two instances of the queue and thus potential
     * corrupt file.
     */
    @Override
    protected NetworkTaskQueue createQueue(String queueName) {
        return NetworkTaskQueue.create(this, mGson, mBus, queueName);
    }
}