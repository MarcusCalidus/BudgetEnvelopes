package com.marcuscalidus.budgetenvelopes.transactions;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.TransactionDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionDialogFragment.OnTransactionUpdateListener;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class TransactionListArrayAdapter extends
		ArrayAdapter<TransactionDataObject> implements OnLongClickListener {
	
	private UUID envelopeUUID = null;
	private OnTransactionUpdateListener onTransactionUpdateListener;
	private int resourceID = 0;
	
	public TransactionListArrayAdapter(Context context, List<TransactionDataObject> objects, EnvelopeDataObject envelope, int resource) {
		super(context, resource, objects);
		
		resourceID = resource;
		
		if (envelope != null) {
			envelopeUUID = envelope.getId();
		} else {
			envelopeUUID = UUID.randomUUID();
		}			
	}
	
	public void initView(View v, int position) {
		v.setTag(getItem(position));
		v.setOnLongClickListener(this);
		
		TextView txt = (TextView) v.findViewById(R.id.textDate);
		Calendar cal = getItem(position).getTimestamp();
		
		if (resourceID == R.layout.list_item_transaction) {
			txt.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH))+'.');
		} else {
			txt.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH))+'.'+String.valueOf(cal.get(Calendar.MONTH)+1)+'.');
		}
		txt = (TextView) v.findViewById(R.id.textDescription);
		txt.setText(getItem(position).getText());
		txt = (TextView) v.findViewById(R.id.textAmount);
		
		String currencySymbol = BudgetEnvelopes.getCurrentSettingValue(SettingsDataObject.UUID_CURRENCY_SYMBOL, "");
		
		if ((getItem(position).getToEnvelope() != null) &&  (getItem(position).getToEnvelope().compareTo(envelopeUUID) == 0)) {
			txt.setText(String.format("%.2f "+currencySymbol, getItem(position).getAmount()));
		} else {
			txt.setText(String.format("- %.2f "+currencySymbol, getItem(position).getAmount()));
		}

		DBMain db = DBMain.getInstance();
		StringBuilder sb = new StringBuilder();
		UUID uuidFrom = getItem(position).getFromEnvelope();
		UUID uuidTo = getItem(position).getToEnvelope();
		
		if (uuidFrom == null) {
			EnvelopeDataObject envelope = EnvelopeDataObject.getById(v.getContext(), db.getReadableDatabase(), uuidTo);
			sb.append(v.getContext().getResources().getString(R.string.deposit_to));
			sb.append(" ");
			sb.append(envelope.toString());
		} else if(uuidTo == null) {
			EnvelopeDataObject envelope = EnvelopeDataObject.getById(v.getContext(), db.getReadableDatabase(), uuidFrom);
			sb.append(v.getContext().getResources().getString(R.string.withdraw_from));
			sb.append(" ");
			sb.append(envelope.toString());
		} else {
			EnvelopeDataObject envelope = EnvelopeDataObject.getById(v.getContext(), db.getReadableDatabase(), uuidFrom);
			sb.append(v.getContext().getResources().getString(R.string.transfer_from));
			sb.append(" ");
			sb.append(envelope.toString());
			envelope = EnvelopeDataObject.getById(v.getContext(), db.getReadableDatabase(), uuidTo);
			sb.append(" -> ");
			sb.append(envelope.toString());
		}
		
		txt = (TextView) v.findViewById(R.id.textInfo);
		txt.setText(sb.toString());
		
		ImageView img = (ImageView) v.findViewById(R.id.imagePending);
        if (img != null) {
            if (getItem(position).isPending()) {
                img.setVisibility(View.VISIBLE);
            } else {
                img.setVisibility(View.GONE);
            }
        }

        img = (ImageView) v.findViewById(R.id.imageClip);
        if (img != null) {
            if (getItem(position).hasAttachment()) {
                img.setVisibility(View.VISIBLE);
            } else {
                img.setVisibility(View.INVISIBLE);
            }
        }
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	  
	    View rowView;
	    if (convertView == null) {
	    	rowView = inflater.inflate(resourceID, parent, false);
	    } else {
	    	rowView = convertView;
	    }    	
	
	    initView(rowView, position);
	    
	    return rowView;
	}

	private void showPopupMenu(final View v) {
		Vibrator vib = (Vibrator) v.getContext().getSystemService(
				Context.VIBRATOR_SERVICE);
		vib.vibrate(50);

		PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
		popupMenu.getMenuInflater().inflate(R.menu.popup_transaction_listitem,
				popupMenu.getMenu());

		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.action_edit_transaction : editTransaction((TransactionDataObject) v.getTag()); break;
						case R.id.action_cancel_transaction : cancelTransaction(v, (TransactionDataObject) v.getTag()); break;
						}
						return true;
					}
				});
		popupMenu.show();
	}
	
	private void editTransaction(TransactionDataObject transaction) {		
		FragmentManager fragmentManager = BudgetEnvelopes.getFragmentManager();
		FragmentTransaction ft = fragmentManager.beginTransaction();
		Fragment prev = fragmentManager.findFragmentByTag("transaction_dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		TransactionDialogFragment newFragment = TransactionDialogFragment
				.newInstance(transaction);
		newFragment.setOnTransactionUpdateListener(this.onTransactionUpdateListener);
		newFragment.show(ft, "transaction_dialog");
	}

	private void cancelTransaction(final View v, final TransactionDataObject transaction) {
		Resources res = BudgetEnvelopes.getAppContext().getResources();
		AlertDialog dialog = new AlertDialog.Builder(v.getContext()).create();
	    dialog.setTitle(res.getString(R.string.cancel_transaction));
	    dialog.setMessage(res.getString(R.string.cancel_transaction_question));
	    
	    dialog.setButton(DialogInterface.BUTTON_POSITIVE, res.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int buttonId) {
	        	DBMain db = DBMain.getInstance();
	        	transaction.setDeleted(true);
	        	transaction.insertOrReplaceIntoDb(db.getWritableDatabase(), true);
	        	
	        	v.animate().alpha(0).setListener(new AnimatorListener() {						
					@Override
					public void onAnimationStart(Animator animation) {							
					}
					
					@Override
					public void onAnimationRepeat(Animator animation) {							
					}
					
					@Override
					public void onAnimationEnd(Animator animation) {
						v.setVisibility(View.GONE);
			        	BudgetEnvelopes.getOnTransactionUpdateListener().onTransactionUpdate(transaction);
					}						
					@Override
					public void onAnimationCancel(Animator animation) {							
					}
				});	        	
	        	
	        }
	    });
	    
	    dialog.setButton(DialogInterface.BUTTON_NEGATIVE, res.getString(android.R.string.no), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int buttonId) {
	        	return;
	        }
	    });

	    dialog.setIcon(android.R.drawable.ic_dialog_alert);
	    dialog.show();			
	}
	
	@Override
	public boolean onLongClick(View v) {
		showPopupMenu(v);
		return true;
	}

	public OnTransactionUpdateListener getOnTransactionUpdateListener() {
		return onTransactionUpdateListener;
	}

	public void setOnTransactionUpdateListener(
			OnTransactionUpdateListener onTransactionUpdateListener) {
		this.onTransactionUpdateListener = onTransactionUpdateListener;
	}

}
