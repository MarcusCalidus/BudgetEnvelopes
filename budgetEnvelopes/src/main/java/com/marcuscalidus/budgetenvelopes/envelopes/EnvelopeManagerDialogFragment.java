package com.marcuscalidus.budgetenvelopes.envelopes;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.envelopes.EnvelopeManagerListArrayAdapter.OnDeleteEnvelopeListener;
import com.marcuscalidus.budgetenvelopes.envelopes.EnvelopeSettingsDialogFragment.OnEnvelopeSettingsChangedListener;
import com.marcuscalidus.budgetenvelopes.widgets.TooltipHoverListener;
import com.slezica.tools.widget.RearrangeableListView;

import java.util.List;

public class EnvelopeManagerDialogFragment 
	extends DialogFragment 
	implements OnDeleteEnvelopeListener, 
			   OnEnvelopeSettingsChangedListener,
			   OnClickListener{
	
	private OnDismissListener onDismissListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(DialogFragment.STYLE_NORMAL,
				android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_dialog_envelope_manager,
				container, false);

		this.initView(v);

		return v;
	}
	
	@Override
	public void onStart()
	{
	  super.onStart();

	  // safety check
	  if (getDialog() == null)
	    return;

	  Display display = getActivity().getWindowManager().getDefaultDisplay();
	  Point size = new Point();
	  display.getSize(size);
	  int dialogWidth = size.x;
	  int dialogHeight = (int) (size.y * 0.9);
	  
	  getDialog().getWindow().setLayout(dialogWidth, dialogHeight);
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		if (onDismissListener != null)
			onDismissListener.onDismiss(dialog);
	}

	public void initView(View v) {
		RearrangeableListView _listView = (RearrangeableListView) v.findViewById(R.id.rearrangeableListView1);
		_listView.setRearrangeEnabled(true);
		//_listView.setOnItemClickListener(this);		
		updateListView(v);

		ImageButton btn;
		btn = (ImageButton) v.findViewById(R.id.buttonAddEnvelope);
		btn.setOnHoverListener(TooltipHoverListener.getInstance());
		btn.setOnClickListener(this);
		btn = (ImageButton) v.findViewById(R.id.buttonAddSpace);
		btn.setOnHoverListener(TooltipHoverListener.getInstance());
		btn.setOnClickListener(this);
		
		Button btn2 = (Button) v.findViewById(R.id.buttonDone);
		btn2.setOnClickListener(this); 
	}
	
	public static EnvelopeManagerDialogFragment newInstance() {
		EnvelopeManagerDialogFragment f = new EnvelopeManagerDialogFragment();

		return f;
	}
	
	public void updateListView(View v) {
		DBMain dbMain = DBMain.getInstance();
		RearrangeableListView _listView = (RearrangeableListView) v.findViewById(R.id.rearrangeableListView1);
		
		List<EnvelopeDataObject> values = EnvelopeDataObject.getAllEnvelopes(this.getActivity(), dbMain.getReadableDatabase(), true, false);
		EnvelopeManagerListArrayAdapter adapter = new EnvelopeManagerListArrayAdapter(getFragmentManager(), this.getDialog().getContext(), values);
		adapter.setOnDeleteEnvelopeListener(this);
		adapter.setOnEnvelopeSettingsChangedListener(this);
		_listView.setAdapter(adapter); 
		_listView.setRearrangeListener(adapter);
	}
	
	public void onClick(View v) {
		RearrangeableListView _listView = (RearrangeableListView) getView().findViewById(R.id.rearrangeableListView1);
		EnvelopeManagerListArrayAdapter adapter;
		
		switch (v.getId()) {
		case R.id.buttonAddEnvelope:
			adapter = (EnvelopeManagerListArrayAdapter) _listView.getAdapter();
			adapter.insert( new EnvelopeDataObject(this.getActivity(), getString(R.string.new_envelope_title), 0xFFAAAAAA), 0);
			adapter.updateDatabase();
			break;
		case R.id.buttonAddSpace: 
			adapter = (EnvelopeManagerListArrayAdapter) _listView.getAdapter();
			adapter.insert(new EnvelopeManagerListArrayAdapter.DummyEnvelopeDataObject(), 0);
			adapter.updateDatabase();
			break;
		case R.id.buttonDone:
			this.dismiss();
			break;
		}
	}

	@Override
	public void onEnvelopeSettingsChanged(EnvelopeDataObject envelope) {
		updateListView(getView());
	}

	@Override
	public void onEnvelopeDeleted(EnvelopeDataObject envelope) {
		RearrangeableListView _listView = (RearrangeableListView) getView().findViewById(R.id.rearrangeableListView1);
		
		EnvelopeManagerListArrayAdapter adapter = (EnvelopeManagerListArrayAdapter) _listView.getAdapter();
		adapter.updateDatabase(); //a first time to delete closed envelopes
		adapter.remove(envelope);
		adapter.updateDatabase(); //a second time... for the spaces to disappear
		adapter.notifyDataSetChanged();	
		
		/*
		EnvelopeSettingsDialogFragment envelopeSettings = (EnvelopeSettingsDialogFragment) 
				 getFragmentManager().findFragmentById(R.id.envelopeSettingsFrame);
		if (envelopeSettings != null) {		
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.remove(envelopeSettings);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}*/
	}

	public OnDismissListener getOnDismissListener() {
		return onDismissListener;
	}

	public void setOnDismissListener(OnDismissListener onDismissListener) {
		this.onDismissListener = onDismissListener;
	}
}
