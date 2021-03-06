package com.librelio.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.analytics.tracking.android.EasyTracker;
import com.librelio.adapter.MagazineAdapter;
import com.librelio.base.BaseActivity;
import com.librelio.event.InvalidateGridViewEvent;
import com.librelio.event.LoadPlistEvent;
import com.librelio.event.UpdateMagazinesEvent;
import com.librelio.loader.PlistParserLoader;
import com.librelio.model.DictItem;
import com.librelio.utils.PlistDownloader;
import com.niveales.wind.R;

import de.greenrobot.event.EventBus;

public class MagazinesFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<DictItem>> {

	private GridView grid;
	private ArrayList<DictItem> magazines;
	private MagazineAdapter adapter;
	
    private String plistName;
    
    private static final int PLIST_PARSER_LOADER = 0;
	private static final String PLIST_NAME = "plist_name";

    private Handler handler = new Handler();

    private Runnable loadPlistTask = new Runnable() {
        @Override
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(PLIST_PARSER_LOADER, null, MagazinesFragment.this);
                }
            });
        }
    };
    
	public static MagazinesFragment newInstance(String plistName) {
		MagazinesFragment f = new MagazinesFragment();
		Bundle a = new Bundle();
		a.putString(PLIST_NAME, plistName);
		f.setArguments(a);
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.issue_list_layout, container, false);

		grid = (GridView) view.findViewById(R.id.issue_list_grid_view);

		magazines = new ArrayList<DictItem>();

		adapter = new MagazineAdapter(magazines, getActivity(), getBaseActivity().hasTestMagazine());
		grid.setAdapter(adapter);
		
		plistName = getArguments().getString(PLIST_NAME);
		
		getLoaderManager().initLoader(PLIST_PARSER_LOADER, null, this);
		
		return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        setHasOptionsMenu(true);

	}
	
    public void onEventMainThread(UpdateMagazinesEvent event) {
        if (event.getMagazines() != null && event.getPlistName().equals(plistName)) {
            magazines.clear();
            magazines.addAll(event.getMagazines());
        }
        reloadGrid();
    }

    public void onEventMainThread(InvalidateGridViewEvent event) {
            reloadGrid();
    }

    public void onEvent(LoadPlistEvent event) {
        startLoadPlistTask(0);
    }

	private void reloadGrid() {
        grid.invalidate();
        grid.invalidateViews();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		EasyTracker.getTracker().sendView("Library/Magazines");
		EventBus.getDefault().register(this);
        startLoadPlistTask(0);
        PlistDownloader.doLoad(getActivity(), plistName, false);
    }

    @Override
	public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        handler.removeCallbacks(loadPlistTask);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	super.onCreateOptionsMenu(menu, inflater);
    	inflater.inflate(R.menu.fragment_magazines, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
		case R.id.options_menu_reload:
            // force a redownload of the plist
            PlistDownloader.doLoad(getActivity(), plistName, true);
			return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public Loader<ArrayList<DictItem>> onCreateLoader(int id, Bundle args) {
//        return new PlistParserLoader(getApplicationContext(), args.getString(PLIST_NAME));

        return new PlistParserLoader(getActivity().getApplicationContext(), plistName, getBaseActivity().hasTestMagazine());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<DictItem>> loader, ArrayList<DictItem> data) {
//        magazines.clear();
//        if (data != null) {
//            magazines.addAll(data);
//        }
        EventBus.getDefault().post(new InvalidateGridViewEvent());
        startLoadPlistTask(2000);
    }

    private void startLoadPlistTask(int delay) {
        handler.removeCallbacks(loadPlistTask);
        handler.postDelayed(loadPlistTask, delay);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<DictItem>> loader) {
        magazines.clear();
        EventBus.getDefault().post(new InvalidateGridViewEvent());
    }
    
    private BaseActivity getBaseActivity() {
    	return (BaseActivity) getActivity();
    }

}
