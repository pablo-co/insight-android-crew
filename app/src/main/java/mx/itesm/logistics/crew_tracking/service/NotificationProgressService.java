package mx.itesm.logistics.crew_tracking.service;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.event.FinishProgressEvent;
import edu.mit.lastmite.insight_library.event.ProgressEvent;
import edu.mit.lastmite.insight_library.service.DaggerIntentService;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;


public class NotificationProgressService extends DaggerIntentService {
    private static final String TAG = "NotificationProgress";

    private static final int NOTIFICATION_ID = 10;

    public static final String EXTRA_QUEUE_NAME = "edu.mit.lastmite.insight_library.queue_name";

    @Inject
    Bus mBus;

    protected NotificationManager mNotificationManager;
    protected NotificationCompat.Builder mBuilder;
    protected String mQueueName;

    @Override
    public void injectService(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Service starting!");
        mBus.register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getQueueName(intent);
        buildNotification();
        updateProgress(0.0f);
        return START_NOT_STICKY;
    }

    protected void getQueueName(Intent intent) {
        mQueueName = intent.getExtras().getString(EXTRA_QUEUE_NAME);
        if (mQueueName == null) {
            throw new RuntimeException("No queue name was specified.");
        }
    }

    protected void buildNotification() {
        mBuilder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder.setContentTitle(getString(R.string.notification_queue_uploading))
                .setContentText(mQueueName)
                .setSmallIcon(R.mipmap.ic_action_cancel);
    }

    protected void updateProgress(float progress) {
        updateProgress(100, (int) (progress * 100));
    }

    protected void updateProgress(int total, int progress) {
        mBuilder.setProgress(total, progress, false);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onProgressEvent(ProgressEvent event) {
        if (event.getQueueName().equals(mQueueName)) {
            updateProgress(event.getProgress());
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onFinishProgressEvent(FinishProgressEvent event) {
        if (event.getQueueName().equals(mQueueName)) {
            mBuilder.setContentText(getString(R.string.notification_queue_finished));
            updateProgress(0, 0);
            stopSelf();
        }
    }

}