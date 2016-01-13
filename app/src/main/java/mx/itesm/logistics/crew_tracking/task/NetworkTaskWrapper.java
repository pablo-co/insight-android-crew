package mx.itesm.logistics.crew_tracking.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.task.NetworkTask;

public class NetworkTaskWrapper {
    protected NetworkTaskQueue mQueue;
    protected ArrayList<Object> mObjects;

    public NetworkTaskWrapper(NetworkTaskQueue queue) {
        mQueue = queue;
    }

    public ArrayList<Object> getModels() {
        if (mObjects == null) {
            mObjects = extractModelsFromQueue();
        }
        return mObjects;
    }

    protected ArrayList<Object> extractModelsFromQueue() {
        ArrayList<Object> objects = new ArrayList<>();
        List<NetworkTask> tasks = getTasks();
        Iterator<NetworkTask> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            NetworkTask task = iterator.next();
            objects.add(task.getModel());
        }
        return objects;
    }

    protected List<NetworkTask> getTasks() {
        return mQueue.peek(mQueue.size());
    }
}
