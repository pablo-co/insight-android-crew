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
import android.widget.EditText;

import com.dd.CircularProgressButton;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.http.ErrorHandler;
import edu.mit.lastmite.insight_library.model.Session;
import edu.mit.lastmite.insight_library.model.User;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.GcmRegistration;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Lab;


public class LoginFragment extends FragmentResponder {

    private static final String TAG = "LoginFragment";

    public static final String EXTRA_USER = "mx.itesm.cartokm2.extra_user";

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Lab mLab;

    @Bind(R.id.email_sign_in_button)
    protected CircularProgressButton mLoginButton;

    @Bind(R.id.email)
    protected EditText mEmailEditText;

    @Bind(R.id.password)
    protected EditText mPasswordEditText;

    protected User mUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = new User();
    }

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, parent, false);
        ButterKnife.bind(this, view);
        mLoginButton.setIndeterminateProgressMode(true);
        return view;
    }

    @OnClick(R.id.email_sign_in_button)
    protected void onSignInClicked() {
        updateUser();
        try {
            doLogin(mUser);
        } catch (Exception e) {
            Log.e(TAG, "Error doing login: ", e);
        }
    }

    protected void updateUser() {
        mUser.setEmail(mEmailEditText.getText().toString());
        mUser.setPassword(mPasswordEditText.getText().toString());
    }

    protected void lockView() {
        mLoginButton.setProgress(0);
        mLoginButton.setProgress(1);
        switchControlState(false);
    }

    protected void unlockView() {
        switchControlState(true);
    }

    protected void switchControlState(boolean state) {
        mEmailEditText.setEnabled(state);
        mPasswordEditText.setEnabled(state);
    }

    protected void doLogin(final User user) throws JSONException {
        lockView();
        GcmRegistration.get(getActivity()).register(new GcmRegistration.Callbacks() {
            @Override
            public void onRegister(Session session) {
                RequestParams params;
                try {
                    params = user.buildParams();
                    params.put(Session.JSON_WRAPPER, session.toHashMap());
                } catch (Exception e) {
                    e.printStackTrace();
                    requestFinished(false);
                    return;
                }
                mAPIFetch.post("crews/postLogin", params, new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        Log.e(TAG, response.toString());
                        try {
                            loginSuccess(new User(response));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        super.onSuccess(statusCode, headers, response);
                    }

                    @Override
                    public void onFinish(boolean success) {
                        requestFinished(success);
                    }
                });
            }
        }, getActivity());
    }

    protected void requestFinished(boolean success) {
        int progress = 0;
        if (success) progress = -1;
        mLoginButton.setProgress(progress);
        unlockView();
    }

    protected void loginSuccess(User user) {
        if (mLab.setUser(user).saveUser()) {
            sendResult(TargetListener.RESULT_OK, user);
        } else {
            ErrorHandler.handleError(getActivity().getSupportFragmentManager(), -1, "Error");
        }
    }

    private void sendResult(int resultCode, User user) {
        if (getTargetListener() == null) return;

        Intent intent = new Intent();
        intent.putExtra(EXTRA_USER, (Serializable) user);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }
}
