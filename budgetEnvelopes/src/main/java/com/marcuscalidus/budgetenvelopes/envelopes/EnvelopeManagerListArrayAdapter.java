package com.marcuscalidus.budgetenvelopes.envelopes;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.envelopes.EnvelopeSettingsDialogFragment.OnEnvelopeSettingsChangedListener;
import com.marcuscalidus.budgetenvelopes.widgets.TooltipHoverListener;
import com.slezica.tools.widget.RearrangeableListView.MovableView;
import com.slezica.tools.widget.RearrangeableListView.RearrangeListener;

import java.util.List;

public class EnvelopeManagerListArrayAdapter extends
		ArrayAdapter<EnvelopeDataObject> implements RearrangeListener {
	 private final Context context;
	 private final List<EnvelopeDataObject> values;
	 private OnDeleteEnvelopeListener _onDeleteEnvelopeListener;
	 private OnEnvelopeSettingsChangedListener _onEnvelopeSettingsChangedListener;
	 private final FragmentManager fragmentManager;
	  
	 private static String TAG_IN_MOTION = "inMotion";
	 
	 public static interface OnDeleteEnvelopeListener {
		 public void onEnvelopeDeleted(EnvelopeDataObject envelope);
	 }
	 
	 public static class DummyEnvelopeDataObject extends EnvelopeDataObject {

		public DummyEnvelopeDataObject() {
			super(null, null);
		}
	 }

	 public EnvelopeManagerListArrayAdapter(FragmentManager fm, Context context, List<EnvelopeDataObject> values) {
	    super(context, R.layout.list_item_envelope, values);
	    this.context = context;
	    this.fragmentManager = fm;
	    
	    for (int i=0; i < values.size(); i++) {
	    	if (values.get(i) == null)
	    		values.set(i, new DummyEnvelopeDataObject());
	    }	    	
	    
	    this.values = values;
	 }	 
	 
	 @Override
	 public void onGrab(int index) {
		EnvelopeDataObject obj = getItem(index);
		obj.setTag(TAG_IN_MOTION);
        notifyDataSetChanged();
     }
        
	 public boolean onRearrangeRequested(int fromIndex, int toIndex) {
	        
		 if (toIndex >= 0 && toIndex < getCount()) {
			 EnvelopeDataObject item = getItem(fromIndex);
	            
			 remove(item);
			 insert(item, toIndex);
			 notifyDataSetChanged();
            
			 return true;
		 }
	        
	     return false;
	 }
 
     @Override
     public void onDrop(int index) {
 		 EnvelopeDataObject obj = getItem(index);
 	     obj.setTag("");
     	 notifyDataSetChanged();
     	 
     	 updateDatabase();	      
     }
     
     public void updateDatabase() {
    	 SQLiteDatabase db = DBMain.getInstance().getWritableDatabase();
    	 db.beginTransaction();
    	 
	     for (int i=0; i<this.values.size(); i++) {
	    	 if (!this.values.get(i).getClass().equals(DummyEnvelopeDataObject.class))
	    	 {
	    		 if (this.values.get(i).isDeleted()) 
	    			 this.values.get(i).insertOrReplaceIntoDb(db, false);
	    		 else {
	    			 this.values.get(i).setPosition(i);
	    			 this.values.get(i).setSpace_After((i<this.values.size()-1) && this.values.get(i+1).getClass().equals(DummyEnvelopeDataObject.class));
	    			 this.values.get(i).insertOrReplaceIntoDb(db, false);
	    		 }
	    	 }
	     }       	 
	     
	     db.setTransactionSuccessful();
	     db.endTransaction();
     }
  
	 public static class ListItemView extends RelativeLayout 
	 					implements MovableView, View.OnClickListener { 
		 private final EnvelopeDataObject _envelope;
		 protected final View _handle;
		 private OnDeleteEnvelopeListener _onDeleteEnvelopeListener;
		 private EnvelopeManagerListArrayAdapter _listArrayAdapter;
		 		 
		 public ListItemView(final EnvelopeManagerListArrayAdapter listArrayAdapter, EnvelopeDataObject envelope) {
			super(listArrayAdapter.context);
			_envelope = envelope;
			_listArrayAdapter = listArrayAdapter;
			LayoutInflater inflater = (LayoutInflater)listArrayAdapter.context.getSystemService
				      (Context.LAYOUT_INFLATER_SERVICE);
	        inflater.inflate(R.layout.list_item_manage_envelope, this, true);	
	        
	        final TextView _textView = (TextView) findViewById(R.id.label); 
	        _textView.setText(_envelope.getTitle());
			if (_envelope.isDeleted()) 
	        	_textView.setText("closed "+_envelope.getTitle());
			resetEnvelopeImage();
	        
	        _handle = findViewById(R.id.imagePull);	 
	        _handle.setOnHoverListener(TooltipHoverListener.getInstance());
	        	        
	        ImageView btn;
	        btn = (ImageView) findViewById(R.id.buttonEdit);
		    btn.setOnHoverListener(TooltipHoverListener.getInstance());
		    btn.setOnClickListener(this);  
		    
		    if (_envelope instanceof DummyEnvelopeDataObject) {
		    	btn.setVisibility(View.INVISIBLE);
		    }	 
	        
		    btn = (ImageView) findViewById(R.id.buttonDelete);
		    btn.setOnHoverListener(TooltipHoverListener.getInstance());
		    btn.setOnClickListener(this);

		 }
		 
		 public void resetEnvelopeImage() {			 
			 ImageView image = (ImageView) findViewById(R.id.imageTab);
			 if (_envelope  != null) {
				 image = (ImageView) findViewById(R.id.imageTab);
				 Drawable drawableTab = getResources().getDrawable(R.drawable.envelope).mutate();
				 if (_envelope.getTag() == TAG_IN_MOTION) 
					 drawableTab.setColorFilter( _envelope.getTabColor(), Mode.OVERLAY);
				 else
					 drawableTab.setColorFilter( _envelope.getTabColor(), Mode.MULTIPLY);
		    
				 image.setImageDrawable(drawableTab);
				 
				 image = (ImageView) findViewById(R.id.imageViewStamp);
				 if (image != null)
					 image.setImageDrawable(BudgetEnvelopes.getStampAsset(_envelope.getStamp()));		 
			 } else
				 image.setImageDrawable(null);	
		 }

		@Override
		public boolean onGrabAttempt(int x, int y) {
			return (x >= _handle.getLeft()) && (x <= _handle.getLeft() + _handle.getWidth()); 
		}

		@Override
		public void onRelease() {
			resetEnvelopeImage();
		}

		public EnvelopeDataObject getEnvelope() {
			return _envelope;
		}		
		
		public OnDeleteEnvelopeListener getOnDeleteEnvelopeListener() {
			return _onDeleteEnvelopeListener;
		}

		public void setOnDeleteEnvelopeListener(OnDeleteEnvelopeListener _onDeleteEnvelopeListener) {
			this._onDeleteEnvelopeListener = _onDeleteEnvelopeListener;
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.buttonEdit: 
				FragmentTransaction ft = _listArrayAdapter.fragmentManager.beginTransaction();
				Fragment prev = _listArrayAdapter.fragmentManager.findFragmentByTag("dialogEditEnvelope");
				if (prev != null) {
					ft.remove(prev);
				}
				//ft.addToBackStack(null);
	
				EnvelopeSettingsDialogFragment newFragment = EnvelopeSettingsDialogFragment
						.newInstance(_envelope);
				newFragment.setOnChangeListener(_listArrayAdapter.getOnEnvelopeSettingsChangedListener());
				newFragment.show(ft, "dialogEditEnvelope");
				break;
				
			case R.id.buttonDelete:
				Resources res = getResources();
				AlertDialog dialog = new AlertDialog.Builder(v.getContext()).create();
			    dialog.setTitle(res.getString(R.string.delete_envelope));
			    dialog.setMessage(res.getString(R.string.delete_envelope_question));
		
			    dialog.setButton(DialogInterface.BUTTON_POSITIVE, res.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int buttonId) {
			        	animate().alpha(0).setListener(new AnimatorListener() {						
							@Override
							public void onAnimationStart(Animator animation) {							
							}
							
							@Override
							public void onAnimationRepeat(Animator animation) {							
							}
							
							@Override
							public void onAnimationEnd(Animator animation) {
								_envelope.setDeleted(true);
								if (_onDeleteEnvelopeListener != null)
									_onDeleteEnvelopeListener.onEnvelopeDeleted(_envelope);
							}						
							@Override
							public void onAnimationCancel(Animator animation) {							
							}
						});
			        	return;
			        }
			    });
			  			        
			    dialog.setButton(DialogInterface.BUTTON_NEGATIVE, res.getString(android.R.string.no), new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int buttonId) {
			        	return;
			        }
			    });
                
			    dialog.setIcon(android.R.drawable.ic_dialog_alert);
			    dialog.show();					
			    break;
			}	
		}		
	 }
 
	 @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
		 ListItemView liv = new ListItemView(this, values.get(position)); 
		 liv.setOnDeleteEnvelopeListener(_onDeleteEnvelopeListener);
		 return liv; 
	  }
	 
	  public OnDeleteEnvelopeListener getOnDeleteEnvelopeListener() {
			return _onDeleteEnvelopeListener;
		}

	  public void setOnDeleteEnvelopeListener(OnDeleteEnvelopeListener _onDeleteEnvelopeListener) {
			this._onDeleteEnvelopeListener = _onDeleteEnvelopeListener;
		}

	public OnEnvelopeSettingsChangedListener getOnEnvelopeSettingsChangedListener() {
		return _onEnvelopeSettingsChangedListener;
	}

	public void setOnEnvelopeSettingsChangedListener(
			OnEnvelopeSettingsChangedListener _onEnvelopeSettingsChangedListener) {
		this._onEnvelopeSettingsChangedListener = _onEnvelopeSettingsChangedListener;
	}
}
