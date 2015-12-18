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

import com.dd.CircularProgressButton;
import com.rey.material.widget.Spinner;

import org.apache.http.Header;
import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.Delivery;
import edu.mit.lastmite.insight_library.model.Errorable;
import mx.itesm.logistics.crew_tracking.R;

public class DeliveryFormFragment extends FragmentResponder implements Errorable {

    private static final String TAG = "DeliveryFormFragment";

    public static final String EXTRA_DELIVERY = "mx.itesm.cartokm2.delivery";

    @Inject
    protected APIFetch mAPIFetch;

    @Bind(R.id.actionButton)
    protected CircularProgressButton mActionButton;

    @Bind(R.id.delivery_typeSpinner)
    protected Spinner mTypeSpinner;

    @Bind(R.id.delivery_packageTypeSpinner)
    protected Spinner mPackageSpinner;

    protected Delivery mDelivery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mDelivery = (Delivery) getArguments().getSerializable(EXTRA_VEHICLE);
        if (mDelivery == null) {
            mDelivery = new Delivery();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_form, parent, false);
        ButterKnife.bind(this, view);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.delivery_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeSpinner.setAdapter(adapter);

        ArrayAdapter<CharSequence> packageAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.delivery_packages_array, android.R.layout.simple_spinner_item);
        packageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPackageSpinner.setAdapter(packageAdapter);

        mActionButton.setIdleText(getString(R.string.action_next));
        mActionButton.setIndeterminateProgressMode(true);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDelivery();
                sendResult(TargetListener.RESULT_OK, null);
                //saveDelivery();
            }
        });
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

    protected void clearErrors() {
    }

    protected void updateDelivery() {
        //mDelivery.setIdentifier(mDeliveryIdentifier.getText().toString());
    }

    private void lockView() {
        mActionButton.setProgress(0);
        mActionButton.setProgress(1);
        switchControlState(false);
    }

    private void unlockView() {
        switchControlState(true);
    }

    private void switchControlState(boolean state) {
        //mDeliveryIdentifier.setEnabled(state);
    }

    private void unlock(boolean success) {
        int progress = 0;
        if (!success) progress = -1;
        mActionButton.setProgress(progress);
        unlockView();
    }

    protected void saveDelivery() {
        mAPIFetch.post("deliveries.json", mDelivery.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                unlock(true);
                try {
                    sendResult(TargetListener.RESULT_OK, new Delivery(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                unlock(false);
                switch (statusCode) {
                    case 422:
                        try {
                            JSONObject errors = errorResponse.getJSONObject("errors");
                            setErrors(errors);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

        });


        lockView();
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
