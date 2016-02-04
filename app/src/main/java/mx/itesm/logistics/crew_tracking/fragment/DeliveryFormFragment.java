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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.dd.CircularProgressButton;
import com.rey.material.widget.CheckBox;
import com.rey.material.widget.Spinner;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.Serializable;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.annotation.ServiceConstant;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.event.TimerEvent;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.Delivery;
import edu.mit.lastmite.insight_library.model.Errorable;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.ServiceUtils;
import edu.mit.lastmite.insight_library.util.StringUtils;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.model.CDelivery;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;

public class DeliveryFormFragment extends FragmentResponder implements Errorable {

    private static final String TAG = "DeliveryFormFragment";

    @ServiceConstant
    public static String EXTRA_LATITUDE;

    @ServiceConstant
    public static String EXTRA_LONGITUDE;

    @ServiceConstant
    public static String EXTRA_DELIVERY;

    static {
        ServiceUtils.populateConstants(DeliveryFormFragment.class);
    }

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Bus mBus;

    @Bind(R.id.actionButton)
    protected CircularProgressButton mActionButton;

    @Bind(R.id.delivery_typeSpinner)
    protected Spinner mTypeSpinner;

    @Bind(R.id.delivery_orderIdEditText)
    protected EditText mOrderIdEditText;

    @Bind(R.id.delivery_servedCheckBox)
    protected CheckBox mServedCheckBox;


    @Bind(R.id.delivery_timeTextView)
    protected TextView mTimeTextView;

    protected long mSectionsSeconds = 0l;
    protected CDelivery mDelivery;
    protected float mLatitude;
    protected float mLongitude;

    public static DeliveryFormFragment newInstance(float latitude, float longitude) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(EXTRA_LATITUDE, latitude);
        arguments.putSerializable(EXTRA_LONGITUDE, longitude);

        DeliveryFormFragment fragment = new DeliveryFormFragment();
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
        mLatitude = getArguments().getFloat(EXTRA_LATITUDE);
        mLongitude = getArguments().getFloat(EXTRA_LONGITUDE);
        mDelivery = new CDelivery();
        mDelivery.measureTime();
        mDelivery.setLatitude(Double.valueOf(mLatitude));
        mDelivery.setLongitude(Double.valueOf(mLongitude));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_form, parent, false);
        ButterKnife.bind(this, view);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.delivery_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeSpinner.setAdapter(adapter);

        mActionButton.setIdleText(getString(R.string.action_next));
        mActionButton.setIndeterminateProgressMode(true);

        return view;
    }


    @Override
    public void setErrors(JSONObject errors) {
        clearErrors();
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.actionButton)
    public void onActionClicked() {
        updateDelivery();
        sendResult(TargetListener.RESULT_OK, mDelivery);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onTimerEvent(TimerEvent event) {
        mSectionsSeconds = event.getSectionSeconds();
        mTimeTextView.setText(StringUtils.secondsToString(mSectionsSeconds));
    }

    protected void clearErrors() {
    }

    protected void updateDelivery() {
        mDelivery.setType(mTypeSpinner.getSelectedItemPosition() + 1);
        mDelivery.setServed(!mServedCheckBox.isChecked());
        mDelivery.measureTime();
        String orderId = mOrderIdEditText.getText().toString();
        if (!orderId.isEmpty()) {
            mDelivery.setOrderId(orderId);
        }
    }

    private void sendResult(int resultCode, Delivery delivery) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_DELIVERY, delivery);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }
}
