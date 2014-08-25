package com.marcuscalidus.budgetenvelopes.transactions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.marcuscalidus.budgetenvelopes.dataobjects.TransactionDataObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TransactionsMonth {
		private Calendar _monthDate;
		
		public TransactionsMonth(Date monthDate) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(monthDate);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			
			_monthDate = cal;
		}
		
		public TransactionsMonth(int year, int month) {
			Calendar cal = Calendar.getInstance();
			cal.set(year, month, 1);
			_monthDate = cal;
		}
		
		public TransactionsMonth(String monthDateAsString) throws ParseException {
			this(SimpleDateFormat.getDateInstance().parse(monthDateAsString));
		}
		
		@SuppressLint("SimpleDateFormat")
		public String getDisplayName() {
			return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(_monthDate.getTime());
		}		
		
		@Override
		public String toString() {
			return SimpleDateFormat.getDateInstance().format(_monthDate.getTime());		
		}
		
		public Calendar getCalendar() {
			return _monthDate;
		}
		
		public static class MonthSpinnerAdapter extends ArrayAdapter<TransactionsMonth> {

			public MonthSpinnerAdapter(Context context) {
				super(context, android.R.layout.simple_spinner_item,  TransactionDataObject.getMonthsList());
				this.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
			}
			
			@Override
		    public View getView(int position, View convertView, ViewGroup parent)
		    {
		        View view = super.getView(position, convertView, parent);

		        TextView text = (TextView)view.findViewById(android.R.id.text1);
		        text.setTextColor(Color.BLACK);     
		        text.setText(getItem(position).getDisplayName());

		        return view;

		    }
			
		}
 }

