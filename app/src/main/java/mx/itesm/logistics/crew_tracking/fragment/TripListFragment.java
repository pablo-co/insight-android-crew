/*
 * [2015] - [2015] Grupo Raido SAPI de CV.
 * All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Created by Pablo Cárdenas on 25/10/15.
 */

package mx.itesm.logistics.crew_tracking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import edu.mit.lastmite.insight_library.annotation.ServiceConstant;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.model.User;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import edu.mit.lastmite.insight_library.util.ServiceUtils;
import edu.mit.lastmite.insight_library.util.StaticGoogleMaps;
import edu.mit.lastmite.insight_library.view.CircleImageView;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.model.CStop;
import mx.itesm.logistics.crew_tracking.queue.CrewNetworkTaskQueueWrapper;
import mx.itesm.logistics.crew_tracking.task.NetworkTaskWrapper;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;
import mx.itesm.logistics.crew_tracking.util.Preferences;

public class TripListFragment extends FragmentResponder implements ListView.OnItemClickListener {

    public static final String TAG = "TripListFragment";

    @ServiceConstant
    public static String EXTRA_QUEUE_NAME;

    static {
        ServiceUtils.populateConstants(TripListFragment.class);
    }

    @Inject
    protected CrewNetworkTaskQueueWrapper mQueueWrapper;

    @Inject
    protected Lab mLab;

    protected User mUser;
    protected HashMapWrapperAdapter mAdapter;
    protected ArrayList<HashMapWrapper> mWrappers;

    @Inject
    protected Helper mHelper;

    @Inject
    protected Bus mBus;

    @Bind(R.id.cstop_list_listView)
    protected ListView mListView;

    @Bind(android.R.id.empty)
    protected View mEmptyView;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = mLab.getUser();
        mWrappers = new ArrayList<>();
        mAdapter = new HashMapWrapperAdapter(mWrappers);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_trip_list, parent, false);
        ButterKnife.bind(this, view);

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        loadCStops();

        return view;
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        sendResult(TargetListener.RESULT_OK, mWrappers.get(position).getQueueName());
    }

    protected void loadCStops() {
        mWrappers.clear();
        ArrayList<String> queueNames = mQueueWrapper.getQueueNames();
        Iterator<String> iterator = queueNames.iterator();
        while (iterator.hasNext()) {
            addQueue(iterator.next());
        }
        showModelsLoaded();
    }

    protected void addQueue(String queueName) {
        mQueueWrapper.changeToQueue(queueName);
        NetworkTaskQueue queue = mQueueWrapper.getQueue();
        NetworkTaskWrapper taskWrapper = new NetworkTaskWrapper(queue);

        ArrayList<Object> objects = taskWrapper.getModels();
        if (objects.isEmpty()) {
            return;
        }

        Object firstObject = objects.get(0);
        if (isAHashMap(firstObject)) {
            mWrappers.add(new HashMapWrapper((HashMap) firstObject, queueName));
        }
    }

    protected boolean isAHashMap(Object object) {
        if (object == null) {
            return false;
        }
        return object.getClass() == HashMap.class;
    }

    protected boolean isACStopObject(Object object) {
        if (object == null) {
            return false;
        }
        return object.getClass() == CStop.class;
    }

    protected void showModelsLoaded() {
        mListView.setEmptyView(mEmptyView);
        mAdapter.notifyDataSetChanged();
    }

    protected void sendResult(int resultCode, String string) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_QUEUE_NAME, string);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }

    private class HashMapWrapper {
        private HashMap mHashMap;
        private String mQueueName;

        HashMapWrapper(HashMap hashMap, String queueName) {
            mHashMap = hashMap;
            mQueueName = queueName;
        }

        public HashMap getHashMap() {
            return mHashMap;
        }

        public String getQueueName() {
            return mQueueName;
        }
    }

    private class HashMapWrapperAdapter extends ArrayAdapter<HashMapWrapper> {

        public HashMapWrapperAdapter(ArrayList<HashMapWrapper> cstops) {
            super(getActivity(), 0, cstops);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_cstop, null);
            }

            HashMapWrapper wrapper = getItem(position);
            HashMap hashMap = wrapper.getHashMap();

            Long startTime =((Double) hashMap.get(Preferences.PREFERENCES_START_TIME)).longValue();
            TextView startTimeTextView = (TextView) convertView.findViewById(R.id.item_cstop_startTimeTextView);
            startTimeTextView.setText(mHelper.getDateStringFromTimestamp(startTime));

            Long endTime = ((Double) hashMap.get(Preferences.PREFERENCES_START_TIME)).longValue();
            TextView endTimeTextView = (TextView) convertView.findViewById(R.id.item_cstop_endTimeTextView);
            endTimeTextView.setText(mHelper.getDateStringFromTimestamp(endTime));

            CircleImageView positionCircleImageView = (CircleImageView) convertView.findViewById(R.id.item_cstop_locationCircleImageView);

            StaticGoogleMaps googleMaps = StaticGoogleMaps.builder()
                    .addArgument("center", hashMap.get(Preferences.PREFERENCES_LATITUDE) + "," + hashMap.get(Preferences.PREFERENCES_LONGITUDE))
                    .addArgument("zoom", 15)
                    .addArgument("size", "200x200")
                    .addArgument("scale", 2)
                    .build();

            Log.d(TAG, googleMaps.getUrl());

            Glide.with(getActivity()).load(googleMaps.getUrl()).crossFade().into(positionCircleImageView);

            return convertView;
        }
    }
}
