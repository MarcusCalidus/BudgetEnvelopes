package com.marcuscalidus.budgetenvelopes.widgets;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;
import android.widget.TextView;

public class FilteredCursorAdapter extends CursorAdapter implements Filterable {

	private int fColumnDisplayIdx;
	
	public FilteredCursorAdapter(Context context, Cursor c, boolean autoRequery, int columnDisplayIdx) {
		super(context, c, autoRequery);
		fColumnDisplayIdx = columnDisplayIdx;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		((TextView) view).setText(cursor.getString(fColumnDisplayIdx));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
        final TextView view = (TextView) inflater.inflate(
                android.R.layout.simple_dropdown_item_1line, parent, false);
        bindView(view, context, cursor);
        return view;
	}
	
	@Override
    public String convertToString(Cursor cursor) {
        return cursor.getString(fColumnDisplayIdx);
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        FilterQueryProvider filter = getFilterQueryProvider();
     //   if (filter != null) {
            return filter.runQuery(constraint);
   //     }
    }

}
