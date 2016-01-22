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

package mx.itesm.logistics.crew_tracking.util;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import edu.mit.lastmite.insight_library.model.JSONSerializer;
import edu.mit.lastmite.insight_library.model.JSONable;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.User;
import edu.mit.lastmite.insight_library.model.Vehicle;
import mx.itesm.logistics.crew_tracking.model.VehicleType;

// TODO change serializers and objets to ArrayList and key extraction
public class Lab {

    protected static final String TAG = "Lab";
    protected static final String USER_FILENAME = "user.json";
    protected static final String VEHICLE_FILENAME = "vehicle.json";
    protected static final String ROUTE_FILENAME = "route.json";
    protected static final String VEHICLE_TYPES_FILENAME = "vehicle_types.json";

    protected static User mUser;
    protected static Vehicle mVehicle;
    protected static Route mRoute;
    protected static ArrayList<VehicleType> mVehicleTypes;

    protected JSONSerializer mUserSerializer;
    protected JSONSerializer mVehicleSerializer;
    protected JSONSerializer mRouteSerializer;
    protected JSONSerializer<VehicleType> mVehicleTypesSerializer;

    private Context mAppContext;

    public Lab(Context appContext) {
        mAppContext = appContext;

        mUserSerializer = new JSONSerializer<>(appContext, USER_FILENAME);
        try {
            mUser = (User) mUserSerializer.loadObject("edu.mit.lastmite.insight_library.model.User");
        } catch (Exception e) {
            Log.e(TAG, "Error loading user: ", e);
        } finally {
            if (mUser == null) {
                mUser = new User();
            }
        }

        mVehicleSerializer = new JSONSerializer<>(appContext, VEHICLE_FILENAME);
        try {
            mVehicle = (Vehicle) mVehicleSerializer.loadObject("edu.mit.lastmite.insight_library.model.Vehicle");
        } catch (Exception e) {
            Log.e(TAG, "Error loading vehicle: ", e);
        } finally {
            if (mVehicle == null) {
                mVehicle = new Vehicle();
            }
        }

        mRouteSerializer = new JSONSerializer<>(appContext, ROUTE_FILENAME);
        try {
            mRoute = (Route) mRouteSerializer.loadObject("edu.mit.lastmite.insight_library.model.Route");
        } catch (Exception e) {
            Log.e(TAG, "Error loading Route: ", e);
        } finally {
            if (mRoute == null) {
                mRoute = new Route();
            }
        }

        mVehicleTypesSerializer = new JSONSerializer<>(appContext, VEHICLE_TYPES_FILENAME);
        try {
            mVehicleTypes = mVehicleTypesSerializer.loadArray("mx.itesm.logistics.crew_tracking.model.VehicleType");
        } catch (Exception e) {
            Log.e(TAG, "Error loading VehicleType: ", e);
        } finally {
            if (mVehicleTypes == null) {
                mVehicleTypes = new ArrayList<>();
            }
        }
    }

    public User getUser() {
        return mUser;
    }

    public Lab setUser(User user) {
        mUser = user;
        return this;
    }

    public boolean saveUser() {
        try {
            mUserSerializer.saveObject(mUser);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser() {
        try {
            mUserSerializer.deleteObject();
            mUser = new User();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Vehicle getVehicle() {
        return mVehicle;
    }

    public Lab setVehicle(Vehicle vehicle) {
        mVehicle = vehicle;
        return this;
    }

    public boolean saveVehicle() {
        try {
            mVehicleSerializer.saveObject(mVehicle);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteVehicle() {
        try {
            mVehicleSerializer.deleteObject();
            mVehicle = new Vehicle();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Route getRoute() {
        return mRoute;
    }

    public Lab setRoute(Route Route) {
        mRoute = Route;
        return this;
    }

    public boolean saveRoute() {
        try {
            mRouteSerializer.saveObject(mRoute);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteRoute() {
        try {
            mRouteSerializer.deleteObject();
            mRoute = new Route();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<VehicleType> getVehicleTypes() {
        return mVehicleTypes;
    }

    public Lab setVehicleTypes(List<VehicleType> vehicleTypes) {
        mVehicleTypes = (ArrayList<VehicleType>) vehicleTypes;
        return this;
    }

    public boolean saveVehicleTypes() {
        try {
            mVehicleTypesSerializer.saveArray(mVehicleTypes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteVehicleTypes() {
        try {
            mVehicleTypesSerializer.deleteObject();
            mVehicleTypes = new ArrayList<>();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
