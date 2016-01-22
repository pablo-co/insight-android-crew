package mx.itesm.logistics.crew_tracking.fragment;

import android.os.Bundle;

import com.devspark.progressfragment.ProgressFragment;

public class LoadingFragment extends ProgressFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setContentShown(false);
    }
}