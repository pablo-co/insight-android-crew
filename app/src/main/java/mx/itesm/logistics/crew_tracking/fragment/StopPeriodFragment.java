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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.rey.material.widget.Button;
import com.rey.material.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import mx.itesm.logistics.crew_tracking.R;

public class StopPeriodFragment extends FragmentResponder {

    public static final String EXTRA_TITLE = "mx.itesm.cartokm2.title";
    public static final String EXTRA_CONTENT = "mx.itesm.cartokm2.content";
    public static final String EXTRA_ACTION = "mx.itesm.cartokm2.action";
    public static final String EXTRA_IMAGE_ID = "mx.itesm.cartokm2.image_id";

    @Bind(R.id.titleTextView)
    TextView mTitleTextView;

    @Bind(R.id.contentTextView)
    TextView mContentTextView;

    @Bind(R.id.actionButton)
    Button mActionButton;

    @Bind(R.id.backgroundImageView)
    ImageView mBackgroundImageView;

    public static StopPeriodFragment newInstance(String title, String content, String action, int imageId) {
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_CONTENT, content);
        arguments.putString(EXTRA_ACTION, action);
        arguments.putInt(EXTRA_IMAGE_ID, imageId);

        StopPeriodFragment fragment = new StopPeriodFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stop_period, container, false);
        ButterKnife.bind(this, view);
        mTitleTextView.setText(getArguments().getString(EXTRA_TITLE));
        mContentTextView.setText(getArguments().getString(EXTRA_CONTENT));
        mActionButton.setText(getArguments().getString(EXTRA_ACTION));
        mBackgroundImageView.setImageResource(getArguments().getInt(EXTRA_IMAGE_ID));
        return view;
    }

    @OnClick (R.id.actionButton)
    public void onActionClicked() {
        sendResult(TargetListener.RESULT_OK);
    }

    private void sendResult(int resultCode) {
        if (getTargetListener() == null) return;

        getTargetListener().onResult(getRequestCode(), resultCode, null);
    }
}
