package mx.itesm.logistics.crew_tracking.queue;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.squareup.otto.Bus;

import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueueWrapper;
import mx.itesm.logistics.crew_tracking.service.CrewNetworkQueueService;
import mx.itesm.logistics.crew_tracking.service.NotificationProgressService;

public class CrewNetworkTaskQueueWrapper extends NetworkTaskQueueWrapper {

    public CrewNetworkTaskQueueWrapper(Context context, Gson gson, Bus bus) {
        super(context, gson, bus);
    }

    @Override
    public void executeQueue(String queueName) {
        super.executeQueue(queueName);
        launchProgressService(queueName);
    }

    @Override
    protected NetworkTaskQueue retrieveQueue(String queueName) {
        return CrewNetworkTaskQueue.createWithoutProcessing(mContext, mGson, mBus, queueName);
    }

    @Override
    protected void launchQueueService(String queueName) {
        Intent intent = new Intent(mContext, CrewNetworkQueueService.class);
        intent.putExtra(CrewNetworkQueueService.EXTRA_QUEUE_NAME, queueName);
        mContext.startService(intent);
    }

    protected void launchProgressService(String queueName) {
        Intent intent = new Intent(mContext, NotificationProgressService.class);
        intent.putExtra(NotificationProgressService.EXTRA_QUEUE_NAME, queueName);
        mContext.startService(intent);
    }
}
