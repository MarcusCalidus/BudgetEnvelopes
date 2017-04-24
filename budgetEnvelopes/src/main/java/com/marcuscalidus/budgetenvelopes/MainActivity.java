package com.marcuscalidus.budgetenvelopes;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Vibrator;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.TransactionDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.envelopes.EnvelopeListArrayAdapter;
import com.marcuscalidus.budgetenvelopes.envelopes.EnvelopeListFragment;
import com.marcuscalidus.budgetenvelopes.envelopes.EnvelopeManagerDialogFragment;
import com.marcuscalidus.budgetenvelopes.envelopes.EnvelopeSettingsDialogFragment;
import com.marcuscalidus.budgetenvelopes.envelopes.EnvelopeSettingsDialogFragment.OnEnvelopeSettingsChangedListener;
import com.marcuscalidus.budgetenvelopes.network.SyncActivity;
import com.marcuscalidus.budgetenvelopes.transactions.DistributionDialogFragment;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionDialogFragment;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionDialogFragment.OnTransactionUpdateListener;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionListArrayAdapter;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionsMonthsAdapter;
import com.marcuscalidus.budgetenvelopes.widgets.TooltipHoverListener;

import java.util.List;

public class MainActivity extends Activity implements 
		OnItemClickListener, OnClickListener, OnTransactionUpdateListener, OnItemLongClickListener, OnEnvelopeSettingsChangedListener {

	private static final String ARGUMENT_ENVELOPE_UUID = "last_envelope_uuid";

	private int _xDelta;
	private int _maxLeft;
	private int _minLeft;
	private int _transactionListDefaultLeft;
	
	private ViewPager bookingsViewPager;

	private TransactionsMonthsAdapter bookingsMonthsAdapter;
	private EnvelopeDataObject _lastEnvelope = null;

	private View _transactionList;
	private ListView _transactionListView;

	@Override
	public void onBackPressed() {
		if (_lastEnvelope == null) {
			super.onBackPressed();
		} else {
			View view = findViewById(R.id.list_view_transactions);
			
			if (view != null) {
				
				int screenWidth;
				
  				screenWidth = getResources().getDisplayMetrics().widthPixels;
				
				view.animate().x(screenWidth).alpha(0).start();
				_lastEnvelope = null;
			}
		}
			
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		// getWindow().getDecorView().findViewById(android.R.id.content).setBackgroundColor(Color.argb(255,80,80,80));

		_transactionList = findViewById(R.id.list_view_transactions);
		_transactionListView = (ListView) findViewById(R.id.list_view_transactions_LV);
		
		ImageButton btn = (ImageButton) findViewById(R.id.btnDeposit);
		btn.setOnHoverListener(TooltipHoverListener.getInstance());
		btn.setOnClickListener(this);
		btn = (ImageButton) findViewById(R.id.btnWithdraw);
		btn.setOnHoverListener(TooltipHoverListener.getInstance());
		btn.setOnClickListener(this);
		btn = (ImageButton) findViewById(R.id.btnTransfer);
		btn.setOnHoverListener(TooltipHoverListener.getInstance());
		btn.setOnClickListener(this);
		btn = (ImageButton) findViewById(R.id.btnDistribute);
		btn.setOnHoverListener(TooltipHoverListener.getInstance()); 
		btn.setOnClickListener(this);
		
		ImageView img = (ImageView) findViewById(R.id.imageSync);
		img.setOnHoverListener(TooltipHoverListener.getInstance());
		img.setOnClickListener(this);		
		img = (ImageView) findViewById(R.id.imageSettings);
		img.setOnHoverListener(TooltipHoverListener.getInstance());
		img.setOnClickListener(this);		
		img = (ImageView) findViewById(R.id.imageOverflow);
		img.setOnHoverListener(TooltipHoverListener.getInstance());
		img.setOnClickListener(this);
		
	/*	img.setBackgroundResource(R.drawable.wait_animation);

		// Get the background, which has been compiled to an AnimationDrawable
		AnimationDrawable frameAnimation = (AnimationDrawable) img
				.getBackground();

		// Start the animation (looped playback by default).
		frameAnimation.start();
				*/
		EnvelopeListFragment envelopes = (EnvelopeListFragment) getFragmentManager().findFragmentById(R.id.list_view_envelopes);
		envelopes.getListView().setOnItemClickListener(this);
		envelopes.getListView().setOnItemLongClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onClick(View v) {
		final int id = v.getId();
		
		switch (id) {
		
		case R.id.btnDeposit:
			showTransactionDialog(_lastEnvelope, TransactionDialogFragment.TYPE_DEPOSIT);
			break;
		case R.id.btnWithdraw:
			showTransactionDialog(_lastEnvelope, TransactionDialogFragment.TYPE_WITHDRAWAL);
			break;
		case R.id.btnTransfer:
			showTransactionDialog(_lastEnvelope, TransactionDialogFragment.TYPE_TRANSFER);
			break;
		case R.id.btnDistribute:
			showDistributionDialog();
			break;
		case R.id.imageSync: 
			startSyncActivity();
			break; 
		case R.id.imageOverflow:
			this.openOptionsMenu();
			break;
		case R.id.imageSettings:
			startSettingsActivity();
			break;
		}
	}

    @Override
    public void openOptionsMenu() {

        Configuration config = getResources().getConfiguration();

        if((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                > Configuration.SCREENLAYOUT_SIZE_LARGE) {

            int originalScreenLayout = config.screenLayout;
            config.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE;
            super.openOptionsMenu();
            config.screenLayout = originalScreenLayout;

        } else {
            super.openOptionsMenu();
        }
    }

	private void startSyncActivity() {
		Intent sac = new
		Intent(this,SyncActivity.class);
		startActivity(sac);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		BudgetEnvelopes.setFragmentManager(getFragmentManager());
		BudgetEnvelopes.setOnTransactionUpdateListener(this);
		
		reinitGUI();		
	}

	@Override
	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		if (_lastEnvelope != null) {
			outState.putParcelable(ARGUMENT_ENVELOPE_UUID, new ParcelUuid(_lastEnvelope.getId()));
		}
	}
	
	@Override
	protected void onRestoreInstanceState (Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		if (savedInstanceState.containsKey(ARGUMENT_ENVELOPE_UUID)) {
			DBMain db = DBMain.getInstance();
			_lastEnvelope = EnvelopeDataObject.getById(this,
					db.getReadableDatabase(), ((ParcelUuid) savedInstanceState.getParcelable(ARGUMENT_ENVELOPE_UUID)).getUuid());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_manage_envelopes:
			manageEnvelopes();
			return true;
		case R.id.action_backup_sync:
			startSyncActivity();
			return true;
		case R.id.action_settings:
			startSettingsActivity();
			return true;
        case R.id.action_empty_envelopes:
            emptyEnvelopes();
            return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    private void emptyEnvelopes() {
        Resources res = getResources();
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(res.getString(R.string.empty_envelopes));
        dialog.setMessage(res.getString(R.string.empty_envelopes_question));
        final OnTransactionUpdateListener listener = this;


        dialog.setButton(DialogInterface.BUTTON_POSITIVE, res.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int buttonId) {
                EnvelopeDataObject.emptyAllEnvelopes(BudgetEnvelopes.getAppContext(), DBMain.getInstance().getWritableDatabase());
                listener.onTransactionUpdate(null);
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
    }
	
	private void startSettingsActivity() {
		Intent sac = new
		Intent(this,SettingsActivity.class);
		startActivity(sac);
	}

	private void manageEnvelopes() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog_envelopeManager");
		if (prev != null) {
			ft.remove(prev);
		}
		//ft.addToBackStack(null);

		EnvelopeManagerDialogFragment newFragment = EnvelopeManagerDialogFragment.newInstance();
		newFragment.setOnDismissListener(new OnDismissListener() {				
			@Override
			public void onDismiss(DialogInterface arg0) {
				EnvelopeListFragment envelopes = (EnvelopeListFragment) getFragmentManager().findFragmentById(R.id.list_view_envelopes);
				if (envelopes != null)
					envelopes.updateListView();				
			}
		});
		newFragment.show(ft, "dialog_envelopeManager");
	}
	
	private void showTransactionDialog(EnvelopeDataObject envelope, int type) {		
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("transaction_dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		//ft.addToBackStack(null);

		TransactionDialogFragment newFragment = TransactionDialogFragment
				.newInstance(envelope, type);
		newFragment.setOnTransactionUpdateListener(this);
		newFragment.show(ft, "transaction_dialog");
	}
	
	private void showDistributionDialog() {		
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("distribution_dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		//ft.addToBackStack(null);

		DistributionDialogFragment newFragment = DistributionDialogFragment
				.newInstance();
		newFragment.setOnTransactionUpdateListener(this);
		newFragment.show(ft, "distribution_dialog");
	}

	private void setLastEnvelope(EnvelopeDataObject envelope) {
		_lastEnvelope = envelope;
		initBookingListFromLastEnvelope();
	}
	
	private class InitPendingTransactionListAsync extends AsyncTask<Context, Void, TransactionListArrayAdapter> {
		private ListView listView;
		private OnTransactionUpdateListener onTransactionUpdateListener;

		public InitPendingTransactionListAsync(ListView v, OnTransactionUpdateListener onTransactionUpdateListener ) {		
			listView = v;
			this.onTransactionUpdateListener = onTransactionUpdateListener;
		}
		
		@Override
		protected TransactionListArrayAdapter doInBackground(Context... params) {
			DBMain db = DBMain.getInstance();
		
			List<TransactionDataObject> tl = TransactionDataObject.getPending(params[0], db.getReadableDatabase());
			
			TransactionListArrayAdapter tlaa = new TransactionListArrayAdapter(params[0], tl, null, R.layout.list_item_transaction_pending);
			tlaa.setOnTransactionUpdateListener(onTransactionUpdateListener);
			return tlaa;
		}
		
		@Override
		protected void onPostExecute(TransactionListArrayAdapter result) {
			listView.setAdapter(result);	
		}		
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
	    
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		
		if (currentapiVersion >= android.os.Build.VERSION_CODES.KITKAT)
		{		
			if (hasFocus) {
		        this.getWindow().getDecorView().setSystemUiVisibility(
		                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
		                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
		                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
		                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
		                | View.SYSTEM_UI_FLAG_FULLSCREEN
		                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		        }
		}
	}
	
	public void reinitGUI() {
		View _envelopeList;
		View _reviewBox;
		
		_envelopeList = findViewById(R.id.list_view_envelopes);
		_reviewBox = findViewById(R.id.reviewBox);
		
		//get minimum left side of movable bookings list -- if bookings are moved to left do not go more left than envelope List
		RelativeLayout.LayoutParams layoutParamsEnvelopeList = (RelativeLayout.LayoutParams) _envelopeList
				.getLayoutParams();
		RelativeLayout.LayoutParams layoutParamsBookingList = (RelativeLayout.LayoutParams) _transactionList
				.getLayoutParams();
		RelativeLayout.LayoutParams layoutParamsReviewBox = (RelativeLayout.LayoutParams) _reviewBox
				.getLayoutParams();	
		
		final int screenWidth;
		
		screenWidth = getResources().getDisplayMetrics().widthPixels;
		
		int typicalListWidth = layoutParamsEnvelopeList.width+layoutParamsEnvelopeList.leftMargin;
		
		_minLeft = layoutParamsEnvelopeList.leftMargin;
		_maxLeft = layoutParamsEnvelopeList.leftMargin + layoutParamsEnvelopeList.width;
		
		layoutParamsBookingList.leftMargin = _maxLeft;
		
		if (screenWidth >= 3 * typicalListWidth) {
			_envelopeList.animate().translationX(layoutParamsEnvelopeList.leftMargin).start();
			
			layoutParamsBookingList.width = screenWidth - (2 * typicalListWidth);
			_transactionList.setOnTouchListener(null);
			layoutParamsReviewBox.width = typicalListWidth;
			_reviewBox.setLayoutParams(layoutParamsReviewBox);
			_reviewBox.setVisibility(View.VISIBLE);
		}
		else
		if (screenWidth >= 2 * typicalListWidth) {
			_envelopeList.animate().translationX(layoutParamsEnvelopeList.leftMargin).start();
			
			layoutParamsBookingList.width = screenWidth - typicalListWidth;
			_transactionList.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					final int X = (int) event.getRawX();

					switch (event.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						_xDelta = (int) (X - view.getX());

						break;
					case MotionEvent.ACTION_UP:
						if (view.getX() < (_maxLeft - _minLeft) / 2) {
							view.animate().x(_minLeft).start();
						}
						else
						if (view.getX() < (_maxLeft + ((_maxLeft - _minLeft) / 2))) {
							view.animate().x(_maxLeft).start();
						}
						else {
							view.animate().x(screenWidth).alpha(0).start();
							_lastEnvelope = null;
						}
						
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						break;
					case MotionEvent.ACTION_POINTER_UP:
						break;
					case MotionEvent.ACTION_MOVE:
						view.setX(X - _xDelta);
						break;
					}
					return true;
				}
			});
			layoutParamsReviewBox.width = (int) Math.round(0.9 * (screenWidth - typicalListWidth));
			_reviewBox.setLayoutParams(layoutParamsReviewBox);
			_reviewBox.setVisibility(View.VISIBLE);
		}
		else {
			_envelopeList.animate().translationX((screenWidth - layoutParamsEnvelopeList.width) / 2).start();
			
			layoutParamsBookingList.width = screenWidth;
			layoutParamsBookingList.leftMargin = _minLeft;
			_transactionList.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(final View view, MotionEvent event) {
					final int X = (int) event.getRawX();

					switch (event.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						_xDelta = (int) (X - view.getX());

						break;
					case MotionEvent.ACTION_UP:
						if (view.getX() < (_maxLeft - _minLeft) / 2) {
							view.animate().x(_minLeft).start();
						}
						else {
							view.animate().x(screenWidth).alpha(0).start();
							_lastEnvelope = null;
						}
						
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						break;
					case MotionEvent.ACTION_POINTER_UP:
						break;
					case MotionEvent.ACTION_MOVE:
						view.setX(X - _xDelta);
						break;
					}
					return true;
				}
			});
			_reviewBox.setVisibility(View.INVISIBLE);
		}
			
		_transactionListDefaultLeft = layoutParamsBookingList.leftMargin;
		_transactionList.setLayoutParams(layoutParamsBookingList);
		initBookingListFromLastEnvelope();
		new InitPendingTransactionListAsync(_transactionListView, this).execute(_transactionList.getContext());
	}

	@SuppressWarnings("deprecation")
	public void initBookingListFromLastEnvelope() {
		EnvelopeDataObject envelope = getLastEnvelope();

		if (envelope == null) {
			_transactionList.setVisibility(View.INVISIBLE);
		} else {
			bookingsMonthsAdapter = new TransactionsMonthsAdapter(
					getFragmentManager(), TransactionDataObject.getMonthsList(), envelope);
			bookingsViewPager = (ViewPager) findViewById(R.id.transactionsViewPager);
			bookingsViewPager.setAdapter(bookingsMonthsAdapter);
			bookingsViewPager.setCurrentItem(bookingsMonthsAdapter.getCount()-1);
			bookingsViewPager.setVisibility(View.VISIBLE);
			

			ImageButton btn = (ImageButton) findViewById(R.id.btnDistribute);
			if (envelope.isBaseEnvelope())
				btn.setVisibility(View.VISIBLE);
		    else
		    	btn.setVisibility(View.GONE);

			Drawable drawableTab;
			TextView tv = (TextView) findViewById(R.id.textViewEnvelopeTitle);

			if (envelope.isBaseEnvelope()) {
				tv.setShadowLayer(0, 0, 0, 0);
				tv.setTextColor(0xFA000000);
				tv.setText("My Budget Envelopes");
				drawableTab = getResources().getDrawable(
						R.drawable.transactions_background_base);
			} else {
				tv.setShadowLayer(2, 1, 1, 0xFA000000);
				tv.setTextColor(0xFAFFFFFF); 
				tv.setText(envelope.getTitle());
				drawableTab = getResources().getDrawable(
						R.drawable.transactions_background).mutate();
				drawableTab.setColorFilter(envelope.getTabColor(), Mode.MULTIPLY);
			}
			_transactionList.setBackgroundDrawable(drawableTab);
			_transactionList.setVisibility(View.VISIBLE);
			_transactionList.animate().x(_transactionListDefaultLeft).alpha(1).start();
		}
	}

	private EnvelopeDataObject getLastEnvelope() {
		return _lastEnvelope;
	}
/*
	@Override
	public void onModeChanged(boolean arg0) {
		reinitGUI();	
	}

	@Override
	public void onSizeChanged(Rect arg0) {
		reinitGUI();		
	}

	@Override
	public void onZoneChanged(int arg0) {
		reinitGUI();			
	}
*/
	@Override
	public void onTransactionUpdate(TransactionDataObject transaction) {
		initBookingListFromLastEnvelope();
		EnvelopeListFragment lv = (EnvelopeListFragment) getFragmentManager().findFragmentById(R.id.list_view_envelopes);
		lv.updateListView();
		new InitPendingTransactionListAsync(_transactionListView, this).execute(_transactionList.getContext());
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		EnvelopeListArrayAdapter adapter = (EnvelopeListArrayAdapter) parent.getAdapter();
		setLastEnvelope(adapter.getItem(position));	
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		EnvelopeListArrayAdapter adapter = (EnvelopeListArrayAdapter) parent.getAdapter();
		final EnvelopeDataObject envelope = adapter.getItem(position);
		
		if (envelope != null) {			
			Vibrator vib = (Vibrator) view.getContext().getSystemService(
					Context.VIBRATOR_SERVICE);
			vib.vibrate(50);

			PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
			popupMenu.getMenuInflater().inflate(R.menu.popup_envelope_listitem,
					popupMenu.getMenu());

			popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							switch (item.getItemId()) {
							case R.id.action_settings : setupEnvelope(envelope); break;
							}
							return true;
						}
					});
			popupMenu.show();
			return true;
		}
		return false;	
	}
	
	private void setupEnvelope(EnvelopeDataObject envelope) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialogEditEnvelope");
		if (prev != null) {
			ft.remove(prev);
		}

		EnvelopeSettingsDialogFragment newFragment = EnvelopeSettingsDialogFragment
				.newInstance(envelope);
		newFragment.setOnChangeListener(this);
		newFragment.show(ft, "dialogEditEnvelope");
	}

	@Override
	public void onEnvelopeSettingsChanged(EnvelopeDataObject envelope) {
		EnvelopeListFragment envelopes = (EnvelopeListFragment) getFragmentManager().findFragmentById(R.id.list_view_envelopes);
		if (envelopes != null)
			envelopes.updateListView();			
	}

}
