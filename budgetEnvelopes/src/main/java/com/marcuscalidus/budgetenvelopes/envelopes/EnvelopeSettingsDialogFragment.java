package com.marcuscalidus.budgetenvelopes.envelopes;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;

import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.ExpenseDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.expenses.ExpensesListArrayAdapter;
import com.marcuscalidus.budgetenvelopes.widgets.TooltipHoverListener;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import afzkl.development.colorpickerview.dialog.ColorPickerDialog;
import afzkl.development.colorpickerview.view.ColorPanelView;

public class EnvelopeSettingsDialogFragment extends DialogFragment implements
		OnClickListener {

	static String ARGUMENT_ENVELOPE_UUID = "envelope_uuid";

	public static interface OnEnvelopeSettingsChangedListener {
		public void onEnvelopeSettingsChanged(EnvelopeDataObject envelope);
	}
	
	public static class StampSelection implements Entry<String, Drawable> {
		
		private String key;
		private Drawable drawable;
		
		public StampSelection(String key, Drawable drawable) {
			this.key = key;
			this.drawable = drawable;
		}
		
		@Override
		public String getKey() {
			return key;
		}

		@Override
		public Drawable getValue() {
			return drawable;
		}

		@Override
		public Drawable setValue(Drawable arg0) {
			this.drawable = arg0;
			return drawable;
		}		
	}
		
	public static class StampSelector extends ArrayAdapter<StampSelection> {
		
		public StampSelection currentItem = null;
		
		public StampSelector(Context context, String currentStamp) {
			super(context, R.layout.list_item_stamp);

			String[] fileNames = null;
			try {
				fileNames = context.getAssets().list("stamps");			
			} catch (IOException e) {
			}
			
			for (String fName: fileNames) {
				StampSelection d = new StampSelection(fName, null);
				try {
					d.setValue(Drawable.createFromStream( context.getAssets().open("stamps/"+fName), null));
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.add(d);
				if (fName.equals(currentStamp)) {
					currentItem = d;
				}
			}			
		} 
		 
		@Override
		public View getDropDownView(int position, View cnvtView, ViewGroup prnt) {
			return getCustomView(position, cnvtView, prnt);
		}
		@Override
		public View getView(int pos, View cnvtView, ViewGroup prnt) {
			return getCustomView(pos, cnvtView, prnt);
		}
		
		public View getCustomView(int position, View convertView, ViewGroup parent) {
	        View view = convertView;
	        if (view == null) {
	            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            view = inflater.inflate(R.layout.list_item_stamp, null);
	        }
	        
	        StampSelection item = getItem(position);
	        if (item!= null) {
	            // My layout has only one TextView
	            ImageView itemView = (ImageView) view.findViewById(R.id.imageViewStamp);
	            if (itemView != null) {
	                // do whatever you want with your string and long
	                itemView.setImageDrawable(item.getValue());
	            }
	         }
	        return view;
	    }		
	}

	private ColorPanelView _colorPanelView;
	//private ImageView _imageTab;
	private EditText _editEnvelopeLabel;
	private Button _btnCancel;
	private Button _btnDone;
	private OnEnvelopeSettingsChangedListener _changeListener;
	private ListView _expensesList;
	private ImageButton _btnAddPayment;
	private Spinner _spinnerSetStamp;
    private CheckBox _cbKeepBudgetOnReset;

	public static EnvelopeSettingsDialogFragment newInstance(
			EnvelopeDataObject envelope) {
		EnvelopeSettingsDialogFragment f = new EnvelopeSettingsDialogFragment();

		Bundle args = new Bundle();
		args.putParcelable(ARGUMENT_ENVELOPE_UUID, new ParcelUuid(envelope.getId()));
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(DialogFragment.STYLE_NORMAL,
				android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
	}

	public UUID getActiveEnvelopeId() {
		return ((ParcelUuid) getArguments().getParcelable(ARGUMENT_ENVELOPE_UUID))
				.getUuid();
	}

	private void initView(View v) {
		if (this.getDialog() != null) {
			this.getDialog()
					.getWindow()
					.setSoftInputMode(
							WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}

		DBMain db = DBMain.getInstance();
		EnvelopeDataObject envelope = EnvelopeDataObject.getById(this.getActivity(),
				db.getReadableDatabase(), getActiveEnvelopeId());

	//	TextView lbl = (TextView) v.findViewById(R.id.label);
//		lbl.setText(envelope.getTitle());

		_colorPanelView = (ColorPanelView) v.findViewById(R.id.colorPanelView1);
		_colorPanelView.setColor(envelope.getTabColor());
		_colorPanelView.setOnClickListener(this);
		_colorPanelView.setOnHoverListener(TooltipHoverListener.getInstance());

	/*	_imageTab = (ImageView) v.findViewById(R.id.imageTab);
		Drawable drawableTab = getResources().getDrawable(
				R.drawable.tab_rearrange).mutate();
		drawableTab.setColorFilter(envelope.getTabColor(), Mode.MULTIPLY);
		_imageTab.setImageDrawable(drawableTab);*/

		_editEnvelopeLabel = (EditText) v.findViewById(R.id.editEnvelopeLabel);
		_editEnvelopeLabel.setText(envelope.getTitle());
		_editEnvelopeLabel.setOnHoverListener(TooltipHoverListener.getInstance());

		_btnCancel = (Button) v.findViewById(R.id.buttonCancel);
		_btnCancel.setOnClickListener(this);
		_btnCancel.setOnHoverListener(TooltipHoverListener.getInstance());
		_btnDone = (Button) v.findViewById(R.id.buttonDone);
		_btnDone.setOnClickListener(this);
		_btnDone.setOnHoverListener(TooltipHoverListener.getInstance());

		_btnAddPayment = (ImageButton) v.findViewById(R.id.btnPaymentAdd);
		_btnAddPayment.setOnClickListener(this);
		_btnAddPayment.setOnHoverListener(TooltipHoverListener.getInstance());
		
		_spinnerSetStamp = (Spinner) v.findViewById(R.id.spinnerStamp);
		_spinnerSetStamp.setOnHoverListener(TooltipHoverListener.getInstance());
		StampSelector stampSelector = new StampSelector(getActivity(), envelope.getStamp());
		_spinnerSetStamp.setAdapter(stampSelector);
		_spinnerSetStamp.setSelection(stampSelector.getPosition(stampSelector.currentItem));

        _cbKeepBudgetOnReset = (CheckBox) v.findViewById(R.id.checkBoxKeepOnEmpty);
        _cbKeepBudgetOnReset.setOnHoverListener(TooltipHoverListener.getInstance());
        _cbKeepBudgetOnReset.setChecked(envelope.getIgnoreOnReset());

		_expensesList = (ListView) v.findViewById(R.id.listViewExpenses);
	}

	private void saveToDb() {
		getView().requestFocus();

		DBMain db = DBMain.getInstance();
		EnvelopeDataObject envelope = EnvelopeDataObject.getById(this.getActivity(),
				db.getReadableDatabase(), getActiveEnvelopeId());

		envelope.setTitle(_editEnvelopeLabel.getText().toString());
		envelope.setTabColor(_colorPanelView.getColor());
        envelope.setIgnoreOnReset(_cbKeepBudgetOnReset.isChecked());
		
		StampSelection ssl = (StampSelection) _spinnerSetStamp.getSelectedItem();
		envelope.setStamp(ssl.getKey());

		envelope.insertOrReplaceIntoDb(db.getWritableDatabase(), true);

		ExpensesListArrayAdapter eaa = (ExpensesListArrayAdapter) _expensesList
				.getAdapter();
		eaa.saveToDB(db.getWritableDatabase());

		if (_changeListener != null) {
			_changeListener.onEnvelopeSettingsChanged(envelope);
		}
	}

	public void updateExpensesListView() {
		DBMain dbMain = DBMain.getInstance();
		SQLiteDatabase db = dbMain.getReadableDatabase();

		EnvelopeDataObject envelope = EnvelopeDataObject.getById(this.getActivity(), db,
				getActiveEnvelopeId());

		if (envelope == null)
			return;

		List<ExpenseDataObject> values = ExpenseDataObject
				.getExpensesForEnvelope(this.getActivity(), db, envelope);
		_expensesList.setAdapter(new ExpensesListArrayAdapter(getActivity(),
				values));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_dialog_envelope_settings,
				container, false);

		this.initView(v);

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateExpensesListView();
	}

	public void onClick(final View v) {
		if (v == _btnAddPayment) {
			((ExpensesListArrayAdapter) _expensesList.getAdapter()).add(new ExpenseDataObject(this.getActivity(), getActiveEnvelopeId()));
			((ExpensesListArrayAdapter) _expensesList.getAdapter()).notifyDataSetChanged();
		} else if (v == _btnCancel) {
			if (this.getDialog() != null) {
				this.getDialog().dismiss();
			} else {
				initView(this.getView());
				updateExpensesListView();
			}
		} else if (v == _btnDone) {
			saveToDb();
			if (this.getDialog() != null) {
				this.getDialog().dismiss();
			}
		} else if (v == _colorPanelView) {
			final ColorPickerDialog colorDialog = new ColorPickerDialog(
					this.getActivity(), _colorPanelView.getColor());

			colorDialog.setAlphaSliderVisible(true);
			colorDialog.setTitle(getResources().getText(R.string.pick_a_color));

			colorDialog.setButton(DialogInterface.BUTTON_POSITIVE,
					getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {

						@Override 
						public void onClick(DialogInterface dialog, int which) {
							((ColorPanelView) v).setColor(colorDialog
									.getColor());
							Drawable drawableTab = getResources().getDrawable(
									R.drawable.envelope).mutate();
							drawableTab.setColorFilter(colorDialog.getColor(),
									Mode.MULTIPLY);
						//	_imageTab.setImageDrawable(drawableTab);
						}
					});

			colorDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
					getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Nothing to do here.
						}
					});

			colorDialog.show();
		}
	}

	public OnEnvelopeSettingsChangedListener getOnChangeListener() {
		return _changeListener;
	}

	public void setOnChangeListener(
			OnEnvelopeSettingsChangedListener _changeListener) {
		this._changeListener = _changeListener;
	}
}
