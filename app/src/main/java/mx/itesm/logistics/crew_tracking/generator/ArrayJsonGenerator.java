package mx.itesm.logistics.crew_tracking.generator;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;

import edu.mit.lastmite.insight_library.model.JSONable;

/**
 * GRUPO RAIDO CONFIDENTIAL
 * __________________
 *
 * [2015] - [2015] Grupo Raido Incorporated
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Grupo Raido SAPI de CV and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Grupo Raido SAPI de CV and its
 * suppliers and may be covered by México and Foreign Patents,
 * patents in process, and are protected by trade secret or
 * copyright law. Dissemination of this information or
 * reproduction of this material is strictly forbidden unless
 * prior written permission is obtained from Grupo Raido SAPI
 * de CV.
 */
public class ArrayJsonGenerator<T extends JSONable> {

    protected ArrayList<T> mArray;

    public ArrayJsonGenerator(ArrayList<T> array) {
        mArray = array;
    }

    public JSONArray generateJson() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        Iterator<T> iterator = mArray.iterator();
        while (iterator.hasNext()) {
            jsonArray.put(iterator.next().toJSON());
        }
        return jsonArray;
    }
}
