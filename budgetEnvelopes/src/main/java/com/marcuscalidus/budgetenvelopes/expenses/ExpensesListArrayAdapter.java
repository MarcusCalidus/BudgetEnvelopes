package com.marcuscalidus.budgetenvelopes.expenses;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.ExpenseDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.widgets.TooltipHoverListener;

import java.util.List;

public class ExpensesListArrayAdapter extends ArrayAdapter<ExpenseDataObject> 
	implements OnClickListener, OnFocusChangeListener {
	private Context context;
	private List<ExpenseDataObject> values;
	
	public ExpensesListArrayAdapter(Context context, List<ExpenseDataObject> values) {
		super(context, R.layout.list_item_expenses, values);
		this.context = context;
		this.values = values;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    
	    ExpenseDataObject edo = values.get(position);
	    
	    if (edo.isDeleted())
	    	return new View(context);
	    
	    View rowView;
	    
	    if ((convertView != null) && (!((ExpenseDataObject) convertView.getTag()).isDeleted()))
	    	rowView = convertView;
	    else		
	    	rowView = inflater.inflate(R.layout.list_item_expenses, parent, false);  
	    
	    if  (rowView.getTag() != edo) {	    	
		    rowView.setTag(edo);
		    
		    EditText textEditLabel = (EditText) rowView.findViewById(R.id.textEditLabel); 
		    textEditLabel.setTag(edo);
		    textEditLabel.setText(edo.getLabel());
		    textEditLabel.setOnFocusChangeListener(this);
		    textEditLabel.setOnHoverListener(TooltipHoverListener.getInstance());
		    
		    EditText textEditAmount = (EditText) rowView.findViewById(R.id.textEditAmount);
		    textEditAmount.setTag(edo);
		    textEditAmount.setText(String.format("%.2f", edo.getAmount()));
		    textEditAmount.setOnFocusChangeListener(this);
		    textEditAmount.setOnHoverListener(TooltipHoverListener.getInstance());
		    	    
		    EditText textEditTime = (EditText) rowView.findViewById(R.id.textEditTime);
		    textEditTime.setTag(edo);
		    textEditTime.setText(String.format("%d", edo.getFrequency()));
		    textEditTime.setOnFocusChangeListener(this);
		    textEditTime.setOnHoverListener(TooltipHoverListener.getInstance());
		    
		    View buttonDelete = rowView.findViewById(R.id.buttonDelete);
		    buttonDelete.setTag(edo);
		    buttonDelete.setOnClickListener(this);
		    buttonDelete.setOnHoverListener(TooltipHoverListener.getInstance());
	    }
	    return rowView;
	  }

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.buttonDelete) {
			DBMain db = DBMain.getInstance();
			((ExpenseDataObject) v.getTag()).setDeleted(true);  
			((ExpenseDataObject) v.getTag()).insertOrReplaceIntoDb(db.getWritableDatabase(), true);
			values.remove(v.getTag());
			notifyDataSetChanged();
		}		
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (!hasFocus) {
			if (v.getId() == R.id.textEditAmount) {
				((ExpenseDataObject) v.getTag()).setAmount(BudgetEnvelopes.parseDoubleSafe(((EditText) v).getText().toString()));
			}
			else
			if (v.getId() == R.id.textEditLabel) {
				((ExpenseDataObject) v.getTag()).setLabel(((EditText) v).getText().toString());
			}
			else 
			if (v.getId() == R.id.textEditTime) {
				((ExpenseDataObject) v.getTag()).setFrequency(Integer.parseInt(((EditText) v).getText().toString()));
			}			
		}		
	}

	public void saveToDB(SQLiteDatabase db) {
		db.beginTransaction();
		for (int i = 0; i < values.size(); i++) 
			values.get(i).insertOrReplaceIntoDb(db, false);
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}

}
