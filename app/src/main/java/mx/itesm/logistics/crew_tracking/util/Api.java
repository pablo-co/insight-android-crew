/*
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

package mx.itesm.logistics.crew_tracking.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import edu.mit.lastmite.insight_library.util.Storage;
import mx.itesm.logistics.crew_tracking.activity.LoginActivity;

public class Api {
    public static final String THEME_COLOR = "theme_color";
    public static final int BASE_COLOR = Color.rgb(96, 125, 139);

    protected Lab mLab;
    protected Context mAppContext;
    protected Storage mStorage;

    public Api(Context appContext, Lab lab, Storage storage) {
        mAppContext = appContext;
        mLab = lab;
        mStorage = storage;
    }

    public void setThemeColor(int color) {
        mStorage.putGlobalInteger(THEME_COLOR, color);
    }

    public int getThemeColor() {
        int storageColor = mStorage.getGlobalInteger(THEME_COLOR);
        if (storageColor == -1) {
            storageColor = BASE_COLOR;
        }
        return storageColor;
    }

    public void logout() {
        mLab.deleteUser();
        mStorage.clear();
        Intent intent = new Intent(mAppContext, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mAppContext.startActivity(intent);

    }
}
