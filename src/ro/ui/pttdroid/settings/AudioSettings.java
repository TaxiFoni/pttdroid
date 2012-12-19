package ro.ui.pttdroid.settings;

import ro.ui.pttdroid.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class AudioSettings extends PreferenceActivity {
	
	private static boolean useSpeex;
	private static int speexQuality;
	private static boolean echoState;

	public static final boolean USE_SPEEX = true;
	public static final boolean DONT_USE_SPEEX = false;	
	public static final boolean ECHO_ON = true;
	public static final boolean ECHO_OFF = false;	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_audio);		
	}	
	
	/**
	 * Update cache settings
	 * @param context
	 */
	public static void getSettings(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Resources res = context.getResources();
		
    	useSpeex = prefs.getBoolean(
    			"use_speex",
    			USE_SPEEX);    		    		
    	speexQuality = Integer.parseInt(prefs.getString(
    			"speex_quality", 
    			res.getStringArray(R.array.speex_quality_values)[0]));
    	echoState = prefs.getBoolean(
    			"echo",
    			ECHO_OFF);    		
	}
	
	public static boolean useSpeex() {
		return useSpeex;
	}	

	public static int getSpeexQuality() {
		return speexQuality;
	}
	
	public static boolean getEchoState() {
		return echoState;
	}		

}
