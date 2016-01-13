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
import android.os.Parcelable;
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

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.model.Visit;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import edu.mit.lastmite.insight_library.util.StaticGoogleMaps;
import edu.mit.lastmite.insight_library.view.CircleImageView;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;

public class VisitListFragment extends FragmentResponder implements ListView.OnItemClickListener {

    public static final String TAG = "VisitListFragment";

    public static final String EXTRA_VISITS = "mx.itesm.logistics.crew_tracking.extra_visits";
    public static final String EXTRA_VISIT = "mx.itesm.logistics.crew_tracking.extra_visit";

    protected VisitAdapter mVisitAdapter;
    protected ArrayList<Visit> mVisits;

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Helper mHelper;

    @Inject
    protected Bus mBus;

    @Bind(R.id.visit_list_listView)
    protected ListView mListView;

    @Bind(android.R.id.empty)
    protected View mEmptyView;

    public static VisitListFragment newInstance(ArrayList<Visit> visits) {
        Bundle arguments = new Bundle();
        arguments.putParcelableArrayList(EXTRA_VISITS, visits);

        VisitListFragment fragment = new VisitListFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVisits = getArguments().getParcelableArrayList(EXTRA_VISITS);
        mVisitAdapter = new VisitAdapter(mVisits);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_visit_list, parent, false);
        ButterKnife.bind(this, view);

        mListView.setAdapter(mVisitAdapter);
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        sendResult(TargetListener.RESULT_OK, mVisits.get(position));
    }

    protected void sendResult(int resultCode, Visit visit) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_VISIT, (Parcelable) visit);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }


    protected String getLatLngString(double latitude, double longitude) {
        return String.format("%f,%f", latitude, longitude);
    }

    private class VisitAdapter extends ArrayAdapter<Visit> {

        public VisitAdapter(ArrayList<Visit> visits) {
            super(getActivity(), 0, visits);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_cstop, null);
            }

            Visit visit = getItem(position);

            TextView startTimeTextView = (TextView) convertView.findViewById(R.id.item_cstop_startTimeTextView);
            startTimeTextView.setText(mHelper.getDateStringFromTimestamp(visit.getStartTime()));

            TextView endTimeTextView = (TextView) convertView.findViewById(R.id.item_cstop_endTimeTextView);
            endTimeTextView.setText(mHelper.getDateStringFromTimestamp(visit.getEndTime()));

            CircleImageView positionCircleImageView = (CircleImageView) convertView.findViewById(R.id.item_cstop_locationCircleImageView);

            String firstMarker = getLatLngString(visit.getLatitudeStart(), visit.getLongitudeStart());
            String lastMarker = getLatLngString(visit.getLatitudeEnd(), visit.getLongitudeEnd());
            StaticGoogleMaps googleMaps = StaticGoogleMaps.builder()
                    .addArgument("zoom", 15)
                    .addArgument("size", "200x200")
                    .addArgument("scale", 2)
                    .addArgument("markers", String.format("color:blue|%s|%s", firstMarker, lastMarker))
                    .build();

            Glide.with(getActivity())
                    .load(googleMaps.getUrl())
                    .crossFade()
                    .into(positionCircleImageView);

            return convertView;
        }
    }

}
