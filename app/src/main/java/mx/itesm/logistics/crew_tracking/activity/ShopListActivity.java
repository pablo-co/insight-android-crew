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

package mx.itesm.logistics.crew_tracking.activity;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.activity.SingleFragmentActivity;
import edu.mit.lastmite.insight_library.annotation.ServiceConstant;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.ServiceUtils;
import edu.mit.lastmite.insight_library.util.Storage;
import mx.itesm.logistics.crew_tracking.fragment.ShopListFragment;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;
import mx.itesm.logistics.crew_tracking.util.Preferences;


public class ShopListActivity extends BaseActivity implements TargetListener {
    @ServiceConstant
    public static String EXTRA_SHOP;

    public static final int REQUEST_SHOP = 0;

    static {
        ServiceUtils.populateConstants(ShopListActivity.class);
    }

    @Inject
    protected Storage mStorage;

    @Override
    protected Fragment createFragment() {
        ShopListFragment fragment = ShopListFragment.newInstance(
                mStorage.getGlobalFloat(Preferences.PREFERENCES_LATITUDE),
                mStorage.getGlobalFloat(Preferences.PREFERENCES_LONGITUDE)
        );
        fragment.setTargetListener(this, REQUEST_SHOP);
        return fragment;
    }

    @Override
    public void injectActivity(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    public void onResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != TargetListener.RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_SHOP:
                Intent intent = new Intent();
                intent.putExtra(EXTRA_SHOP, data.getSerializableExtra(ShopListFragment.EXTRA_CARD));
                setResult(Activity.RESULT_OK, intent);
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
    }
}
