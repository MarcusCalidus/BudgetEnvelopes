package com.marcuscalidus.budgetenvelopes.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnHoverListener;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.marcuscalidus.budgetenvelopes.R;

public class TooltipHoverListener extends Object implements OnHoverListener {
	
	private static TooltipHoverListener instance = null;
	
	public static TooltipHoverListener getInstance() {
        if ( instance == null ){ 
                instance = new TooltipHoverListener(); 
            } 
 
            return instance; 
    } 
	
	private PopupWindow _toolTip = null;
	
	@Override
	public boolean onHover(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
			if ((_toolTip != null) && (_toolTip.isShowing()))
			  _toolTip.dismiss();
			
			if (v.getContentDescription() != "") {
				LayoutInflater inflater = (LayoutInflater) v.getContext().getSystemService
					      (Context.LAYOUT_INFLATER_SERVICE);
				_toolTip = new PopupWindow(
					       inflater.inflate(R.layout.tooltip_popup, null, false), 
					       300, 
					       130, 
					       false); 
		
				TextView txt = (TextView) _toolTip.getContentView().findViewById(R.id.tooltipText);
				txt.setText(v.getContentDescription());
				int[] viewLocation = {0,0};
				v.getLocationInWindow(viewLocation);
				_toolTip.showAtLocation(v, Gravity.NO_GRAVITY, viewLocation[0] +v.getWidth(), 12 + viewLocation[1] + v.getHeight() / 2);
			}
		  }
		else 
		if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
			if ((_toolTip != null) && (_toolTip.isShowing())) 
				_toolTip.dismiss();
		}
	    return true;
	}

}
