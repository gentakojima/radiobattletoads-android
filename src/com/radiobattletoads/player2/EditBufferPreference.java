package com.radiobattletoads.player2;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;
 
public class EditBufferPreference extends android.preference.EditTextPreference {
	
	private CharSequence originalSummary = null;
 
    public EditBufferPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
     
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
         
        setSummary(getSummary());
    }
    
    @Override
    public void setText(String t){
    	try{
    		Integer val = Integer.parseInt(t);
    		if(val<500){
    			// Minimum value is 500ms
    			val=500;
    			Toast.makeText((RBTPlayerApplication) this.getContext().getApplicationContext(),R.string.preferences_buffering_warning_minimum, Toast.LENGTH_SHORT).show();
    		}
    		if(val>20000){
    			// Maximum value is 20000ms
    			val=20000;
    			Toast.makeText((RBTPlayerApplication) this.getContext().getApplicationContext(),R.string.preferences_buffering_warning_maximum, Toast.LENGTH_SHORT).show();
    		}
    		super.setText(val.toString());
    	}
    	catch(NumberFormatException e){
    		super.setText("1500");
    	}
    }
 
    @Override
    public CharSequence getSummary() {
    	if(originalSummary == null){
    		originalSummary = super.getSummary();
    	}
        return ((String)originalSummary).replace("%", this.getText());
    }
}