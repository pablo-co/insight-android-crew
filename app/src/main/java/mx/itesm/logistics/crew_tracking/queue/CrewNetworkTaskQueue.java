package mx.itesm.logistics.crew_tracking.queue;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.squareup.otto.Bus;
import com.squareup.tape.FileObjectQueue;

import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import mx.itesm.logistics.crew_tracking.service.CrewNetworkQueueService;

public class CrewNetworkTaskQueue extends NetworkTaskQueue {

    public CrewNetworkTaskQueue(FileObjectQueue<NetworkTask> delegate, Context context, Bus bus, String fileName) {
        super(delegate, context, bus, fileName);
    }

    @Override
    public void startService() {
        Intent intent = new Intent(mContext, CrewNetworkQueueService.class);
        intent.putExtra(CrewNetworkQueueService.EXTRA_QUEUE_NAME, mFileName);
        mContext.startService(intent);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static CrewNetworkTaskQueue createWithoutProcessing(Context context, Gson gson, Bus bus) {
        return createWithoutProcessing(context, gson, bus, NetworkTaskQueue.FILENAME);
    }

    public static CrewNetworkTaskQueue createWithoutProcessing(Context context, Gson gson, Bus bus, String fileName) {
        FileObjectQueue<NetworkTask> delegate = createFileObjectQueue(context, gson, fileName);
        CrewNetworkTaskQueue taskQueue = new CrewNetworkTaskQueue(delegate, context, bus, fileName);
        return taskQueue;
    }
}