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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import edu.mit.lastmite.insight_library.communication.TargetListener;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.fragment.ShopListFragment;


public class ShopListActivity extends SingleFragmentActivity implements TargetListener {

    public static final String EXTRA_SHOP = "com.gruporaido.tasker.extra_shop";

    public static final int REQUEST_SHOP = 0;

    @Override
    protected Fragment createFragment() {
        ShopListFragment fragment = new ShopListFragment();
        fragment.setTargetListener(this, REQUEST_SHOP);
        return fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
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
}
