package com.marcuscalidus.budgetenvelopes.transactions;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;

import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;

public class TransactionsMonthsAdapter extends FragmentStatePagerAdapter {

	private TransactionsMonth[] _months;
	private EnvelopeDataObject _envelope;
	
	public TransactionsMonthsAdapter(FragmentManager fm, TransactionsMonth[] months, EnvelopeDataObject envelope) {
		super(fm);
		_months = months;
		_envelope = envelope;
	}

	@Override
	public Fragment getItem(int position) {
		return TransactionsFragment.newInstance(_months[position], _envelope);
	}

	@Override
	public int getCount() {
		return _months.length;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return _months[position].getDisplayName();
	}
}
