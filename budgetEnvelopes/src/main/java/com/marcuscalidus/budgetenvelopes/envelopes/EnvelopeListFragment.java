package com.marcuscalidus.budgetenvelopes.envelopes;

import android.app.ListFragment;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;

import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;

import java.util.List;

public class EnvelopeListFragment extends ListFragment implements
		EnvelopeSettingsDialogFragment.OnEnvelopeSettingsChangedListener {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getView().setBackgroundColor(Color.argb(0, 100, 100, 100));
		getListView().setDivider(null);
		getListView().setDividerHeight(-30);
		getListView().setSelector(R.drawable.transparent);
	}

	@Override
	public void onStart() {
		super.onStart();
		updateListView();
	}

	public void updateListView() {
		DBMain dbMain = DBMain.getInstance();
		SQLiteDatabase db = dbMain.getReadableDatabase();
		List<EnvelopeDataObject> values = EnvelopeDataObject
				.getAllEnvelopes(this.getActivity(), db, true, false);
		
		values.add(0, EnvelopeDataObject.getBaseEnvelope(this.getActivity(), db));
		values.add(null);
		EnvelopeListArrayAdapter adapter = new EnvelopeListArrayAdapter(
				getActivity(), values);
		setListAdapter(adapter);
	}

	@Override
	public void onEnvelopeSettingsChanged(EnvelopeDataObject envelope) {
		updateListView();
	}

}
