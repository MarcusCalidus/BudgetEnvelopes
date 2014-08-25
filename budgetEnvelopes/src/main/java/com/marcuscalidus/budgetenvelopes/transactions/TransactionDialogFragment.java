package com.marcuscalidus.budgetenvelopes.transactions;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;

import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.TransactionDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.widgets.TooltipHoverListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TransactionDialogFragment extends DialogFragment implements OnClickListener, OnDateSetListener {

	static String ARGUMENT_TRANSACTION_UUID = "transaction_uuid";
	static String ARGUMENT_ENVELOPE_UUID = "envelope_uuid";
	static String ARGUMENT_TYPE = "type";
	
	public static interface OnTransactionUpdateListener {
		public void onTransactionUpdate(TransactionDataObject transaction);
	}
	
	private OnTransactionUpdateListener onTransactionUpdateListener;
	
	public static final int TYPE_DEPOSIT = 1;
	public static final int TYPE_WITHDRAWAL = 2;
	public static final int TYPE_TRANSFER = 3;
	
	public static TransactionDialogFragment newInstance(
			TransactionDataObject transaction) {
		TransactionDialogFragment f = new TransactionDialogFragment();

		Bundle args = new Bundle();
		if (transaction != null) {
			args.putParcelable(ARGUMENT_TRANSACTION_UUID, new ParcelUuid(transaction.getId()));
			
			if (transaction.getToEnvelope() == null) {
				args.putInt(ARGUMENT_TYPE, TYPE_WITHDRAWAL);
				args.putParcelable(ARGUMENT_ENVELOPE_UUID, new ParcelUuid(transaction.getFromEnvelope()));
			} 
			else 
			if (transaction.getFromEnvelope() == null) {
				args.putInt(ARGUMENT_TYPE, TYPE_DEPOSIT);
				args.putParcelable(ARGUMENT_ENVELOPE_UUID, new ParcelUuid(transaction.getToEnvelope()));
			} else {
				args.putInt(ARGUMENT_TYPE, TYPE_TRANSFER);
				args.putParcelable(ARGUMENT_ENVELOPE_UUID, new ParcelUuid(transaction.getFromEnvelope()));
			}			
		}
		f.setArguments(args);

		return f;
	}	
	
	public static TransactionDialogFragment newInstance(
			EnvelopeDataObject envelope, int type) {
		TransactionDialogFragment f = new TransactionDialogFragment();

		Bundle args = new Bundle();
		args.putParcelable(ARGUMENT_ENVELOPE_UUID, new ParcelUuid(envelope.getId()));
		args.putInt(ARGUMENT_TYPE, type);
		f.setArguments(args);

		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(DialogFragment.STYLE_NORMAL,
				android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_dialog_transaction,
				container, false);

		this.initView(v);

		return v;
	}	
	
	public UUID getActiveEnvelopeId() {
		return ((ParcelUuid) getArguments().getParcelable(ARGUMENT_ENVELOPE_UUID))
				.getUuid();
	}
	
	public int getActiveType() {
		return getArguments().getInt(ARGUMENT_TYPE);
	}
	
	public UUID getActiveTransactionId() {
		ParcelUuid puuid = (ParcelUuid) getArguments().getParcelable(ARGUMENT_TRANSACTION_UUID);
		if (puuid != null)
			return puuid.getUuid();
		else
			return null;
	}
	
	public void initView(View v) {
		if (this.getDialog() != null) {
			this.getDialog()
					.getWindow()
					.setSoftInputMode(
							WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}

		DBMain db = DBMain.getInstance();
		
		EnvelopeDataObject envelope = EnvelopeDataObject.getById(this.getActivity(),
				db.getReadableDatabase(), getActiveEnvelopeId());
		
		String typeString = "";
		
		TableRow tr = (TableRow) v.findViewById(R.id.rowTargetEnvelope);
		
		Resources res = getResources();
		switch (getActiveType()) {
		case TYPE_DEPOSIT : 
			typeString = res.getString(R.string.deposit_to);
			tr.setVisibility(View.GONE);
			break;
		case TYPE_WITHDRAWAL : 
			typeString = res.getString(R.string.withdraw_from);
			tr.setVisibility(View.GONE);
			break;
		case TYPE_TRANSFER : 
			typeString = res.getString(R.string.transfer_from);
			tr.setVisibility(View.VISIBLE);
			tr.setOnHoverListener(TooltipHoverListener.getInstance());
			
			DBMain dbMain = DBMain.getInstance();			
			List<EnvelopeDataObject> values = EnvelopeDataObject.getAllEnvelopes(this.getActivity(), dbMain.getReadableDatabase(), false, true);
			values.remove(envelope);
			ArrayAdapter<EnvelopeDataObject> aafdo = new ArrayAdapter<EnvelopeDataObject>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, values);
			Spinner spinner = (Spinner) v.findViewById(R.id.spinnerTargetEnvelope);
			spinner.setAdapter(aafdo);
			
			break;
		}
		
		TextView titleText = (TextView) v.findViewById(R.id.titleText);
		titleText.setText(typeString + " " +envelope.getTitle());
		
		EditText editDate = (EditText) v.findViewById(R.id.editDate);
		editDate.setOnClickListener(this);
		AutoCompleteTextView editDescription = (AutoCompleteTextView) v.findViewById(R.id.editDescription);
		EditText editAmount = (EditText) v.findViewById(R.id.editAmount);
		Switch switchPending = (Switch) v.findViewById(R.id.switchPending);
		switchPending.setOnHoverListener(TooltipHoverListener.getInstance());
		Button btnDone = (Button) v.findViewById(R.id.buttonDone);
		btnDone.setOnClickListener(this);
		
		//initialize fields
		editDescription.setAdapter(envelope.getDescriptionsAdapter(this.getActivity(), db.getReadableDatabase(), getActiveType()));
		
		TransactionDataObject trans = TransactionDataObject.getById(this.getActivity(), db.getReadableDatabase(), getActiveTransactionId());
		
		if (trans != null) {
			//editing existing transaction	
			editDate.setText(SimpleDateFormat.getDateInstance(DateFormat.LONG).format(trans.getTimestamp().getTime()));
			editDescription.setText(trans.getText());
			editAmount.setText(trans.getAmount().toString());
			switchPending.setChecked(trans.isPending());
			
			if (getActiveType() == TYPE_TRANSFER) {
				trans.getFromEnvelope();
				Spinner spinner = (Spinner) v.findViewById(R.id.spinnerTargetEnvelope);
				@SuppressWarnings("unchecked")
				ArrayAdapter<EnvelopeDataObject> aafdo = (ArrayAdapter<EnvelopeDataObject>) spinner.getAdapter();
				for (int i = 0; i < aafdo.getCount(); i++) {
					if (aafdo.getItem(i).getId().compareTo(trans.getToEnvelope()) == 0) {
						spinner.setSelection(i, true);
						break;
					}
				}
			}
		}
		else {
			//adding new transaction set fields to defaults
			editDate.setText(SimpleDateFormat.getDateInstance(DateFormat.LONG).format(new Date()));
		}		

	}
	
	public Date getDateEditValue() {
		EditText ed = (EditText) this.getView().findViewById(R.id.editDate);
		try { 
			return SimpleDateFormat.getDateInstance(DateFormat.LONG).parse(ed.getText().toString());
		} catch (ParseException e) {
			Log.e("BudgetEnvelopes", e.getMessage());
			return null;
		}		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.editDate :
			    int year, month, day;
				Date currentDate = getDateEditValue();
		    	Calendar cal = Calendar.getInstance();
			    if (currentDate != null) {
			    	cal.setTime(currentDate);
			    }
			    
			    year = cal.get(Calendar.YEAR);
			    month = cal.get(Calendar.MONTH);
			    day = cal.get(Calendar.DAY_OF_MONTH);
			    
			    DatePickerDialog dpd = new DatePickerDialog(this.getActivity(), this, year, month, day);
			    dpd.show();
				break;
		case R.id.buttonDone : 
			saveToDb();
			break;
		}
		
	}
	
	public void saveToDb() {		
		Resources res = getResources();

		AutoCompleteTextView editDescription = (AutoCompleteTextView) getView().findViewById(R.id.editDescription);
		EditText editAmount = (EditText) getView().findViewById(R.id.editAmount);
		Switch switchPending = (Switch) getView().findViewById(R.id.switchPending);
		
		float valueAmount = 0; 
		try {
			valueAmount = BudgetEnvelopes.parseFloatSafe(editAmount.getText().toString());
		} catch(Exception e) {}
		
		if ((valueAmount <= 0) || (Float.isNaN(valueAmount))) {
			AlertDialog alertDialog = new AlertDialog.Builder(this.getActivity()).create();
			alertDialog.setTitle(res.getString(R.string.error_title));
			alertDialog.setMessage(res.getString(R.string.error_invalid_amount));
			alertDialog.show();
			return;		
		}
		
		DBMain db = DBMain.getInstance();
		
		TransactionDataObject trans = TransactionDataObject.getById(this.getActivity(), db.getReadableDatabase(), getActiveTransactionId());
		
		if (trans == null) {
			//adding new transaction	
			trans = new TransactionDataObject(this.getActivity(), null);
			//test what type and accordingly set the envelope
			switch (getActiveType()) {
			case TYPE_DEPOSIT : 
				trans.setToEnvelope(getActiveEnvelopeId());
				break;
			case TYPE_WITHDRAWAL : 
				trans.setFromEnvelope(getActiveEnvelopeId());
				break;
			case TYPE_TRANSFER :
				trans.setFromEnvelope(getActiveEnvelopeId());
				Spinner spinner = (Spinner) getView().findViewById(R.id.spinnerTargetEnvelope);
				trans.setToEnvelope(((EnvelopeDataObject) spinner.getSelectedItem()).getId());
				
				if (trans.getFromEnvelope().compareTo(trans.getToEnvelope())==0) {
					AlertDialog alertDialog = new AlertDialog.Builder(this.getActivity()).create();
					alertDialog.setTitle(res.getString(R.string.error_title));
					alertDialog.setMessage(res.getString(R.string.error_invalid_target_envelope));
					alertDialog.show();
					return;	
				}
				break;
			}
		}

		trans.setAmount(BudgetEnvelopes.parseFloatSafe(editAmount.getText().toString()));
		trans.setText(editDescription.getText().toString());
		trans.setTimestamp(getDateEditValue());
		trans.setPending(switchPending.isChecked());
		
		trans.insertOrReplaceIntoDb(db.getWritableDatabase(), true);
		
		if (onTransactionUpdateListener != null) {
			onTransactionUpdateListener.onTransactionUpdate(trans);
		}
		
		if (this.getDialog() != null) 
			this.getDialog().dismiss();		
	}

	@Override
	public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, dayOfMonth);
		EditText ed = (EditText) this.getView().findViewById(R.id.editDate);
		ed.setText(SimpleDateFormat.getDateInstance(DateFormat.LONG).format(cal.getTime()));
	}

	public OnTransactionUpdateListener getOnTransactionUpdateListener() {
		return onTransactionUpdateListener;
	}

	public void setOnTransactionUpdateListener(
			OnTransactionUpdateListener onTransactionUpdateListener) {
		this.onTransactionUpdateListener = onTransactionUpdateListener;
	}

}
