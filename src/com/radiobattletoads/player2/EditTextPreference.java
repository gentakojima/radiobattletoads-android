package com.radiobattletoads.player2;

import android.content.Context;
import android.util.AttributeSet;
 
public class EditTextPreference extends android.preference.EditTextPreference {
	
	private CharSequence originalSummary = null;
 
    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
     
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
         
        setSummary(getSummary());
    }
 
    @Override
    public CharSequence getSummary() {
    	if(originalSummary == null){
    		originalSummary = super.getSummary();
    	}
        return ((String)originalSummary).replace("%", this.getText());
    }
}