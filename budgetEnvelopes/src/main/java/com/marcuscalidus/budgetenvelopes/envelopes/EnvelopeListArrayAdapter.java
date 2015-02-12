package com.marcuscalidus.budgetenvelopes.envelopes;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.TransactionDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionsMonth;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionsMonth.MonthSpinnerAdapter;

import java.util.Calendar;
import java.util.List;

public class EnvelopeListArrayAdapter extends ArrayAdapter<EnvelopeDataObject> {
	  private final Context context;
	  private final List<EnvelopeDataObject> values;
	  	  
	  public EnvelopeListArrayAdapter(Context context, List<EnvelopeDataObject> values) {
	    super(context, R.layout.list_item_envelope, values);
	    this.context = context;
	    this.values = values;
	  }
	  
	  private class InitIncomeExpenseAsync extends AsyncTask<Context, Void, SparseArray<Double>> {
			private Calendar fromDate, toDate;
			private View view;

			public InitIncomeExpenseAsync(View v, Calendar calendar) {		
				view = v;				
				
				fromDate = Calendar.getInstance();
				fromDate.setTimeInMillis(calendar.getTimeInMillis());
				fromDate.set(Calendar.DAY_OF_MONTH, 1);
				fromDate.set(Calendar.AM_PM, Calendar.AM);
				fromDate.set(Calendar.HOUR, 0);
				fromDate.set(Calendar.MINUTE, 0);
				fromDate.set(Calendar.SECOND, 0);
				fromDate.set(Calendar.MILLISECOND, 0);
				
				toDate = Calendar.getInstance();
				toDate.setTimeInMillis(calendar.getTimeInMillis());
				toDate.set(Calendar.DAY_OF_MONTH, toDate.getActualMaximum(Calendar.DAY_OF_MONTH));
				toDate.set(Calendar.AM_PM, Calendar.AM);
				toDate.set(Calendar.HOUR, 23);
				toDate.set(Calendar.MINUTE, 59);
				toDate.set(Calendar.SECOND, 59);
				toDate.set(Calendar.MILLISECOND, 999);	
			}
			
			@Override
			protected SparseArray<Double> doInBackground(Context... params) {
				DBMain db = DBMain.getInstance();
				
				return TransactionDataObject.getIncomeExpenseDetailBetween(params[0], db.getReadableDatabase(), fromDate, toDate);
			}
			
			@Override
			protected void onPostExecute(SparseArray<Double> result) {
				String currencySymbol = BudgetEnvelopes.getCurrentSettingValue(SettingsDataObject.UUID_CURRENCY_SYMBOL, "");
				
				TextView tv = (TextView) view.findViewById(R.id.TextViewIncomeMonth);
				tv.setText(String.format("%.2f "+currencySymbol, result.get(TransactionDataObject.KEY_INCOME)));
				tv = (TextView) view.findViewById(R.id.TextViewExpenseMonth);
				tv.setText(String.format("%.2f "+currencySymbol, result.get(TransactionDataObject.KEY_EXPENSE)));
			}
			
		}
   
	  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	  
	    View rowView;
	    if (values.get(position) == null)
	        rowView = inflater.inflate(R.layout.list_item_envelope_null, parent, false);
	    else {
	    	TextView textView;
	    	
    		String currencySymbol = BudgetEnvelopes.getCurrentSettingValue(SettingsDataObject.UUID_CURRENCY_SYMBOL, "");
	    	
	    	if (values.get(position).getId().compareTo(EnvelopeDataObject.baseEnvelopeID) == 0) {
	    		rowView = inflater.inflate(R.layout.list_item_envelope_base, parent, false);
	    		final View baseRowView = rowView;
	    		rowView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						ListView listView = (ListView) v.getParent();
						listView.performItemClick(v, 0, 0);
					}
				});
	    		final Spinner spinner = (Spinner) rowView.findViewById(R.id.spinnerMonth); 
	    		spinner.setAdapter(new TransactionsMonth.MonthSpinnerAdapter(this.getContext()));	
	    		spinner.setSelection(spinner.getCount()-1);
	    		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view,
							int position, long id) {
						MonthSpinnerAdapter msa = (MonthSpinnerAdapter) spinner.getAdapter();
						InitIncomeExpenseAsync iiea = new InitIncomeExpenseAsync(baseRowView, msa.getItem(position).getCalendar());
						iiea.execute(getContext());
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						TextView tv = (TextView) baseRowView.findViewById(R.id.TextViewIncomeMonth);
						tv.setText("");
					}
				});
	    		
			    textView = (TextView) rowView.findViewById(R.id.textViewBudget);
			    textView.setText(String.format("%.2f",values.get(position).getBudget()) + " " + currencySymbol);
			    
			    textView = (TextView) rowView.findViewById(R.id.textViewPlannedExpenses);
			    textView.setText(String.format("%.2f",values.get(position).getExpenses()) + " " + currencySymbol);
			    
			    textView = (TextView) rowView.findViewById(R.id.textViewTotalCredit);
			    Double f = EnvelopeDataObject.queryTotalCredit(context, DBMain.getInstance().getReadableDatabase());
			    textView.setText(String.format("%.2f",f) + " " + currencySymbol);
	    	}
	    	else {	
	    		
	    	//	if (convertView != null)
	    	//		return convertView;
	    					
	    		rowView = inflater.inflate(R.layout.list_item_envelope, parent, false);
	    		
	    		ImageView image = (ImageView) rowView.findViewById(R.id.imageTab);
			    Drawable drawableTab = context.getResources().getDrawable(R.drawable.envelope).mutate();
			    drawableTab.setColorFilter( values.get(position).getTabColor() , Mode.MULTIPLY);
			    
			    image.setImageDrawable(drawableTab);
		 
			    textView = (TextView) rowView.findViewById(R.id.label);  	    
			    textView.setText(values.get(position).getTitle());
			    			    			    
			    textView = (TextView) rowView.findViewById(R.id.textViewBudget);
			    textView.setText(String.format("%.2f",values.get(position).getBudget()) + " " + currencySymbol);
			    
			    textView = (TextView) rowView.findViewById(R.id.textViewPlannedExpenses);
			    textView.setText(String.format("%.2f",values.get(position).getExpenses()) + " " + currencySymbol);
			    
			    textView = (TextView) rowView.findViewById(R.id.imageOverdrawn);
			    if (textView != null) {
			    	if (values.get(position).getBudget()<0) {
			    		textView.setVisibility(View.VISIBLE);
			    		textView.setTextColor(values.get(position).getTabColor());
			    	} else {
			    		textView.setVisibility(View.GONE);
			    	}
			    }
			    
			    image = (ImageView) rowView.findViewById(R.id.imageViewStamp);
			    if (image != null) {
				    image.setImageDrawable(BudgetEnvelopes.getStampAsset(values.get(position).getStamp()));
			    }
	    	}
	    	
	   	    	
	    	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
	            rowView.setId(BudgetEnvelopes.generateViewId());
	        } else {
		    	rowView.setId(View.generateViewId());
	        }	    	  
		    	    
		    rowView.setTag(values.get(position));		  
	    }

	    return rowView;
	  }

}
