package com.marcuscalidus.budgetenvelopes.transactions;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.TransactionDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class TransactionsFragment extends Fragment {
	
	static String ARGUMENT_ENVELOPE_UUID = "envelope_uuid";
	static String ARGUMENT_TRANSACTION_CALENDAR = "transaction_cal";
		
	public static TransactionsFragment newInstance(
			TransactionsMonth month,
			EnvelopeDataObject envelope) {
		TransactionsFragment f = new TransactionsFragment();

		Bundle args = new Bundle();
		args.putParcelable(ARGUMENT_ENVELOPE_UUID, new ParcelUuid(envelope.getId()));
		args.putLong(ARGUMENT_TRANSACTION_CALENDAR, month.getCalendar().getTimeInMillis());
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_transactions,
				container, false);

		this.initView(v);

		return v;
	}
	
	public void initView(View v) {
		Bundle args = getArguments();
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(args.getLong(ARGUMENT_TRANSACTION_CALENDAR));
		
		new InitListAsync(v, args).execute(v.getContext());
	}
	
	private class InitListAsync extends AsyncTask<Context, Void, TransactionListArrayAdapter> {
		private Calendar fromDate, toDate;
		private UUID envelopeUuid;
		private View view;

		public InitListAsync(View v, Bundle args) {		
			view = v;
			fromDate = Calendar.getInstance();
			fromDate.setTimeInMillis(args.getLong(ARGUMENT_TRANSACTION_CALENDAR));
			fromDate.set(Calendar.DAY_OF_MONTH, 1);
			fromDate.set(Calendar.AM_PM, Calendar.AM);
			fromDate.set(Calendar.HOUR, 0);
			fromDate.set(Calendar.MINUTE, 0);
			fromDate.set(Calendar.SECOND, 0);
			fromDate.set(Calendar.MILLISECOND, 0);
			
			toDate = Calendar.getInstance();
			toDate.setTimeInMillis(args.getLong(ARGUMENT_TRANSACTION_CALENDAR));
			toDate.set(Calendar.DAY_OF_MONTH, toDate.getActualMaximum(Calendar.DAY_OF_MONTH));
			toDate.set(Calendar.AM_PM, Calendar.AM);
			toDate.set(Calendar.HOUR, 23);
			toDate.set(Calendar.MINUTE, 59);
			toDate.set(Calendar.SECOND, 59);
			toDate.set(Calendar.MILLISECOND, 999);
			
			envelopeUuid = ((ParcelUuid) args.getParcelable(ARGUMENT_ENVELOPE_UUID)).getUuid();
		}
		
		@Override
		protected TransactionListArrayAdapter doInBackground(Context... params) {
			DBMain db = DBMain.getInstance();
		
			EnvelopeDataObject envelope = EnvelopeDataObject.getById(params[0], db.getReadableDatabase(), envelopeUuid);
			List<TransactionDataObject> tl = TransactionDataObject.getTransactionsBetween(params[0], db.getReadableDatabase(), envelope, fromDate, toDate);
			
			TransactionListArrayAdapter tlaa = new TransactionListArrayAdapter(params[0], tl, envelope, R.layout.list_item_transaction);
			tlaa.setOnTransactionUpdateListener(BudgetEnvelopes.getOnTransactionUpdateListener());
			return tlaa;
		}
		
		@Override
		protected void onPostExecute(TransactionListArrayAdapter result) {
			ListView lv = (ListView) view.findViewById(R.id.list_view_transactions);
			lv.setAdapter(result);	
		}
		
	}

}
