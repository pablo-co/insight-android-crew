package mx.itesm.logistics.crew_tracking.queue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.internal.$Gson$Types;
import com.squareup.otto.Bus;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueueWrapper;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import edu.mit.lastmite.insight_library.task.QueueHeaderTask;
import mx.itesm.logistics.crew_tracking.service.CrewNetworkQueueService;
import mx.itesm.logistics.crew_tracking.service.NotificationProgressService;

public class CrewNetworkTaskQueueWrapper extends NetworkTaskQueueWrapper {

    private static final String PREFERENCES_EXECUTING_QUEUE_NAMES = "executing_queue_names";

    protected ArrayList<String> mExecutingQueueNames;

    public CrewNetworkTaskQueueWrapper(Context context, Gson gson, Bus bus) {
        super(context, gson, bus);
        mExecutingQueueNames = loadExecutingQueueNames();
        executeRemainingQueues();
    }

    @Override
    public void addTask(NetworkTask entry) {
        reloadQueueFromDisk();
        super.addTask(entry);
        if (shouldExecuteQueue(entry)) {
            executeQueue(mQueue.getFileName());
        }
    }

    @Override
    public void executeQueue(String queueName) {
        super.executeQueue(queueName);
        addExecutingQueueName(queueName);
        launchProgressService(queueName);
    }

    @Override
    public void changeOrCreateIfNoQueue() {
        super.changeOrCreateIfNoQueue();
        if (mQueue != null) {
            reloadQueueFromDisk();
        }
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

    protected void addExecutingQueueName(String queueName) {
        mExecutingQueueNames.add(queueName);
        saveExecutingQueueNames();
    }

    protected void removeExecutingQueueName(String queueName) {
        mExecutingQueueNames.remove(queueName);
        saveExecutingQueueNames();
    }

    protected boolean shouldExecuteQueue(NetworkTask entry) {
        boolean headerSync = isHeaderTask(entry) && ((QueueHeaderTask) entry).shouldSync();
        boolean inQueue = isInExecutingQueue();
        return headerSync || inQueue;
    }

    protected boolean isHeaderTask(NetworkTask task) {
        return task.getClass() == QueueHeaderTask.class;
    }

    protected ArrayList<String> loadExecutingQueueNames() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String json = sharedPreferences.getString(PREFERENCES_EXECUTING_QUEUE_NAMES, null);
        ArrayList<String> queueNames = new ArrayList<>();
        if (json != null) {
            Type type = $Gson$Types.newParameterizedTypeWithOwner(null, ArrayList.class, String.class);
            queueNames = mGson.fromJson(json, type);
        }
        return queueNames;
    }

    protected void saveExecutingQueueNames() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        sharedPreferences.edit().putString(PREFERENCES_EXECUTING_QUEUE_NAMES, mGson.toJson(mExecutingQueueNames)).apply();
    }

    protected boolean isInExecutingQueue() {
        return mExecutingQueueNames.contains(mQueue.getFileName());
    }

    private void executeRemainingQueues() {
        for (String queueName : new ArrayList<>(mExecutingQueueNames)) {
            NetworkTaskQueue queue = retrieveQueue(queueName);
            if (queue.size() == 0) {
                removeExecutingQueueName(queueName);
            } else {
                executeQueue(queueName);
                break;
            }
        }
    }
}
