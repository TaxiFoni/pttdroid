package ro.ui.pttdroid;

import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements OnTouchListener {
	
	/*
	 * True if the activity is really starting for the first time.
	 * False if the activity starts after it was previously closed by a configuration change, like screen orientation. 
	 */
	private static boolean isStarting = true;	
	public static boolean isTestmode = false;
	
	private ImageView microphoneImage;	
	private TextView lossesText;
	
	public static final int MIC_STATE_NORMAL = 0;
	public static final int MIC_STATE_PRESSED = 1;
	public static final int MIC_STATE_DISABLED = 2;
	
	private static int microphoneState = MIC_STATE_NORMAL;
	
	/*
	 * Threads for recording and playing audio data.
	 * This threads are stopped only if isFinishing() returns true on onDestroy(), meaning the back button was pressed.
	 * With other words, recorder and player threads will still be running if an screen orientation event occurs.
	 */	
	private static Player player;	
	private static Recorder recorder;	
	private static PlayHello playhello;
	
	// Block recording when playing  something.
	private static Handler handler = new Handler();
	private static Runnable runnable;
	private static int storedProgress = 0;	
	private static final int PROGRESS_CHECK_PERIOD = 100;
	
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);        
                                      
        init();        
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	lossesText.setText("");
    	// Initialize codec 
    	Speex.open(AudioSettings.getSpeexQuality());
    	player.resumeAudio();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	
    	player.pauseAudio();
    	recorder.pauseAudio();    	

    	// Release codec resources
    	Speex.close();
    }
            
    @Override
    public void onDestroy() {
    	lossesText.setText("");
    	super.onDestroy();  
    	release();    	
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent i; 
    	
    	switch(item.getItemId()) {
    	case R.id.settings_comm:
    		i = new Intent(this, CommSettings.class);
    		startActivityForResult(i, 0);    		
    		return true;
    	case R.id.settings_audio:
    		i = new Intent(this, AudioSettings.class);
    		startActivityForResult(i, 0);    		
    		return true;    
    	case R.id.settings_reset_all:
    		return resetAllSettings();    		
    	case R.id.testmode:
    		return testModeFunc();
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    /**
     * Start test mode.
     * @return
     */
    public boolean testModeFunc() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	Editor editor = prefs.edit();
    	editor.clear();
    	editor.commit();   
    	
    	Toast toast;
    	
    	if(!isTestmode) {
    		isTestmode = true;
    		recorder.setTestmode(isTestmode);
    		playhello.setRunLoop(isTestmode);

    		toast = Toast.makeText(this, getString(R.string.testmode_on), Toast.LENGTH_SHORT);
    		toast.setGravity(Gravity.CENTER, 0, 0);
    		toast.show();
    		Log.d("TESTMODE:", "test mode is ON");
    	} else {
    		isTestmode = false;
    		recorder.setTestmode(isTestmode);
    		playhello.setRunLoop(isTestmode);
    		
    		toast = Toast.makeText(this, getString(R.string.testmode_off), Toast.LENGTH_SHORT);
    		toast.setGravity(Gravity.CENTER, 0, 0);
    		toast.show();
    		Log.d("TESTMODE:", "test mode is OFF");
    	}
    	return true;
    }

    /**
     * Reset all settings to their default value
     * @return
     */
    private boolean resetAllSettings() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	Editor editor = prefs.edit();
    	editor.clear();
    	editor.commit();   
    	
    	Toast toast = Toast.makeText(this, getString(R.string.setting_reset_all_confirm), Toast.LENGTH_SHORT);
    	toast.setGravity(Gravity.CENTER, 0, 0);
    	toast.show();

    	return true;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	CommSettings.getSettings(this);     	    	
    	AudioSettings.getSettings(this);    	
    }
    

    private void displayQuality() {
    	double losses = (double) player.getLosses();
    	double seqNum = (double) player.getSeqNum();
    	Log.i ("Player", "Losses " + losses);
    	Log.i ("Player", "seqNum " + seqNum);
    	
    	double quality = 100.0 - losses/seqNum * 100;
    	lossesText.setText("Quality: " + String.format("%1$.2f", quality) + "%");
    }
    
    private void clearLossesText() {
    	lossesText.setText("");
    }
    
    public boolean onTouch(View v, MotionEvent e) {
    	if(getMicrophoneState()!=MIC_STATE_DISABLED) {    		
    		switch(e.getAction()) {
    		case MotionEvent.ACTION_DOWN:    			
    			recorder.resumeAudio();
    			setMicrophoneState(MIC_STATE_PRESSED);
    			break;
    		case MotionEvent.ACTION_UP:
    			setMicrophoneState(MIC_STATE_NORMAL);
    			recorder.pauseAudio();    			
    			break;
    		}
    	}
    	return true;
    }
    
    public synchronized void setMicrophoneState(int state) {
    	switch(state) {
    	case MIC_STATE_NORMAL:
    		microphoneState = MIC_STATE_NORMAL;
    		microphoneImage.setImageResource(R.drawable.microphone_normal_image);
    		break;
    	case MIC_STATE_PRESSED:
    		microphoneState = MIC_STATE_PRESSED;
    		microphoneImage.setImageResource(R.drawable.microphone_pressed_image);
    		lossesText.setText("");
    		break;
    	case MIC_STATE_DISABLED:
    		microphoneState = MIC_STATE_DISABLED;
    		microphoneImage.setImageResource(R.drawable.microphone_disabled_image);
    		break;    		
    	}
    }
    
    public synchronized int getMicrophoneState() {
    	return microphoneState;
    }
    
    private void init() {
    	microphoneImage = (ImageView) findViewById(R.id.microphone_image);
    	microphoneImage.setOnTouchListener(this);    	    	    	    	    	
    	
    	lossesText = (TextView) findViewById(R.id.lossesText);
    	//lossesText.setText("Loses reported here");
    	
    	// When the volume keys will be pressed the audio stream volume will be changed. 
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	    	    	
    	/*
    	 * If the activity is first time created and not destroyed and created again like on an orientation screen change event.
    	 * This will be executed only once.
    	 */    	    	    	    	
    	if(isStarting) {    		
    		CommSettings.getSettings(this);
    		AudioSettings.getSettings(this);
    		    	    	    		
    		player = new Player();    		    		     		    	
    		recorder = new Recorder();
    		playhello = new PlayHello();
    		
    		// Disable microphone when receiving data.
    		runnable = new Runnable() {
				
				public void run() {					
					int currentProgress = player.getProgress();
					
					if(currentProgress!=storedProgress) {
						if(getMicrophoneState()!=MIC_STATE_DISABLED) {
							recorder.pauseAudio();
							clearLossesText();
							setMicrophoneState(MIC_STATE_DISABLED);
						}						 							
					}
					else {
						if(getMicrophoneState()==MIC_STATE_DISABLED) {
							setMicrophoneState(MIC_STATE_NORMAL);
							displayQuality();
				    		player.resetLosses();
						}
					}
					
					storedProgress = currentProgress;
					handler.postDelayed(this, PROGRESS_CHECK_PERIOD);
				}
			};
    		
    		handler.removeCallbacks(runnable);
    		handler.postDelayed(runnable, PROGRESS_CHECK_PERIOD);
    		
    		player.start();
    		recorder.start(); 
    		playhello.setDaemon(true);
    		playhello.start();
    		
    		isStarting = false;    		
    	}
    }
    
    private void release() {    	
    	// If the back key was pressed.
    	if(isFinishing()) {
    		handler.removeCallbacks(runnable);

    		// Force threads to finish.
    		player.finish();    		    		
    		recorder.finish();;
    		
    		try {
    			player.join();
    			recorder.join();
    		}
    		catch(InterruptedException e) {
    			Log.d("PTT", e.toString());
    		}
    		player = null;
    		recorder = null;
    	    		
    		// Resetting isStarting.
    		isStarting = true;     		
    	}
    }     
        
    public static void resumeAudiofunc() {
    	recorder.resumeAudio();
    }
    
    public static void pauseAudiofunc() {
    	recorder.pauseAudio();
    }
}
