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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.activity.SingleFragmentActivity;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.crew_tracking.R;
import mx.itesm.logistics.crew_tracking.fragment.TrackFragment;
import mx.itesm.logistics.crew_tracking.receiver.LocationReceiver;
import mx.itesm.logistics.crew_tracking.service.LocationManagerService;
import mx.itesm.logistics.crew_tracking.util.CrewAppComponent;

public class MainActivity extends SingleFragmentActivity {

    @Inject
    protected Helper mHelper;

    @Override
    protected Fragment createFragment() {
        return  new TrackFragment();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    public void injectActivity(ApplicationComponent component) {
        ((CrewAppComponent) component).inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overrideTransitions = false;
        super.onCreate(savedInstanceState);
    }

    protected void startBackgroundServices() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferences.edit().putBoolean(getString(R.string.location_enabled), true).apply();
        Intent intent = new Intent(this, LocationManagerService.class);
        startService(intent);
    }

    protected void stopBackgroundServices() {
        Intent destroyIntent = new Intent(this, LocationManagerService.class);
        stopService(destroyIntent);
    }

    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {
        @Override
        protected void onLocationReceived(Context context, Location loc) {
        }

        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_LONG).show();
        }
    };

}
