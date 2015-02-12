package com.marcuscalidus.budgetenvelopes.transactions;

import android.app.DialogFragment;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioGroup;

import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.TransactionDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionDialogFragment.OnTransactionUpdateListener;
import com.marcuscalidus.budgetenvelopes.widgets.TooltipHoverListener;

import java.util.Date;
import java.util.List;

public class DistributionDialogFragment extends DialogFragment implements OnClickListener {
	
	private View _btnCancel;
	private View _btnDone;

	private OnTransactionUpdateListener	onTransactionUpdateListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(DialogFragment.STYLE_NORMAL,
				android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_dialog_distribution,
				container, false);

		this.initView(v);

		return v;
	}	
	
	public void initView(View v) {
		if (this.getDialog() != null) {
			this.getDialog()
					.getWindow()
					.setSoftInputMode(
							WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
		
		_btnCancel = (Button) v.findViewById(R.id.buttonCancel);
		_btnCancel.setOnClickListener(this);
		_btnCancel.setOnHoverListener(TooltipHoverListener.getInstance());
		_btnDone = (Button) v.findViewById(R.id.buttonDone);
		_btnDone.setOnClickListener(this);
		_btnDone.setOnHoverListener(TooltipHoverListener.getInstance());
	}
	
	public static DistributionDialogFragment newInstance() {
		DistributionDialogFragment f = new DistributionDialogFragment();
/*
		Bundle args = new Bundle();
		args.putParcelable(ARGUMENT_ENVELOPE_UUID, new ParcelUuid(envelope.getId()));
		args.putInt(ARGUMENT_TYPE, type);
		f.setArguments(args);
*/
		return f;
	}

	@Override
	public void onClick(View v) {
		if (v == _btnCancel) {
			if (this.getDialog() != null) {
				this.getDialog().dismiss();
			} 
		} else if (v == _btnDone) {
			saveToDb();
			if (this.getDialog() != null) {
				this.getDialog().dismiss();
			}		
		}
	}

	private void saveToDb() {
		RadioGroup rg = (RadioGroup) this.getView().findViewById(R.id.radioGroup1);
		switch (rg.getCheckedRadioButtonId()) {
		case R.id.radioProRata : distributeProRata(); break;
		case R.id.radioExact : distributeExact(); break;
		}
	}
	
	private void distributeProRata() {
		SQLiteDatabase db = DBMain.getInstance().getWritableDatabase();
		db.beginTransaction();
		
		EnvelopeDataObject baseEnvelope = EnvelopeDataObject.getBaseEnvelope(getActivity(), db);
		double toDistribute = baseEnvelope.getBudget();
		double totalExpenses = baseEnvelope.getExpenses();
		
		if ((toDistribute <= 0) || (totalExpenses <= 0)) 
			return;
				
		List<EnvelopeDataObject> envelopes = EnvelopeDataObject.getAllEnvelopes(getActivity(), db, false, false);
		
		for (EnvelopeDataObject envelope : envelopes) {
			double percent = envelope.getExpenses() / totalExpenses;
			
			TransactionDataObject transaction = new TransactionDataObject(getActivity(), null);
			transaction.setFromEnvelope(baseEnvelope.getId());
			transaction.setToEnvelope(envelope.getId());
			transaction.setText(getActivity().getResources().getString(R.string.transaction_text_budget));
			transaction.setTimestamp(new Date());
			transaction.setAmount((double)Math.floor(100*toDistribute*percent)/100);
			
			transaction.insertOrReplaceIntoDb(db, false);			
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
		
		onTransactionUpdateListener.onTransactionUpdate(null);
	}
	
	private void distributeExact() {
		SQLiteDatabase db = DBMain.getInstance().getWritableDatabase();
		db.beginTransaction();
		
		EnvelopeDataObject baseEnvelope = EnvelopeDataObject.getBaseEnvelope(getActivity(), db);
		double toDistribute = baseEnvelope.getBudget();
		double totalExpenses = baseEnvelope.getExpenses();
		
		if ((toDistribute <= 0) || (totalExpenses <= 0)) 
			return;
		
		List<EnvelopeDataObject> envelopes = EnvelopeDataObject.getAllEnvelopes(getActivity(), db, false, false);
		
		for (EnvelopeDataObject envelope : envelopes) {
			if (toDistribute <= 0)
				break;
			
			double amount = (double)Math.floor(100 * Math.min(toDistribute, envelope.getExpenses()))/100;
			toDistribute -= amount;
			
			TransactionDataObject transaction = new TransactionDataObject(getActivity(), null);
			transaction.setFromEnvelope(baseEnvelope.getId());
			transaction.setToEnvelope(envelope.getId());
			transaction.setText(getActivity().getResources().getString(R.string.transaction_text_budget));
			transaction.setTimestamp(new Date());
			transaction.setAmount(amount);
			
			transaction.insertOrReplaceIntoDb(db, false);
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
		
		onTransactionUpdateListener.onTransactionUpdate(null);
	}
	
	public OnTransactionUpdateListener getOnTransactionUpdateListener() {
		return onTransactionUpdateListener;
	}

	public void setOnTransactionUpdateListener(
			OnTransactionUpdateListener onTransactionUpdateListener) {
		this.onTransactionUpdateListener = onTransactionUpdateListener;
	}
}
