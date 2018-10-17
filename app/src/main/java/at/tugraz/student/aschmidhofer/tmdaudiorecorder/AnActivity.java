package at.tugraz.student.aschmidhofer.tmdaudiorecorder;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;

public class AnActivity extends Activity {

	public static final String REC_DIRECTORY = "TMDrecordings";
	public static final String TAG = "TMDAudioRecorder";
	public static final String PREFS_NAME = "TMDRecPersistantStorage";
	public static final String PREF_NOTES = "pref_notes";
	public static final String PREF_CATEGORY = "pref_cat";
	public static final String PREF_CITY = "pref_city";
	public static final String PREF_PHONE = "pref_phoner";
    public static final String PREF_TIMER = "pref_timer";
    public static final String PREF_PATH = "pref_recpath";
	private ComponentName name;


	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final Context context = this;

        // restore previous state
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String notes = settings.getString(PREF_NOTES, "");
		EditText notestxt = (EditText)findViewById(R.id.txt_recname);
		notestxt.setText(notes);
		String city = settings.getString(PREF_CITY, "Graz");
		EditText citytxt = (EditText)findViewById(R.id.txt_city);
		citytxt.setText(city);
		int catIndex = settings.getInt(PREF_CATEGORY, 0);
		Spinner categorySpinner = (Spinner)findViewById(R.id.spin_category);
		categorySpinner.setSelection(catIndex);
		int phoneIndex = settings.getInt(PREF_PHONE, 0);
		Spinner phoneSpinner = (Spinner)findViewById(R.id.spin_phone);
		phoneSpinner.setSelection(phoneIndex);
		int timerMS = settings.getInt(PREF_TIMER, 0);
		setTimerDisplay(timerMS);


		// setup buttons
        final Button startBtn = (Button)findViewById(R.id.btn_start);
        startBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String currentRecPath = getAbsoluteFilePathForRecording();
				Spinner spinsamplerate = (Spinner)findViewById(R.id.spin_samplerate);
				String samplerateString = spinsamplerate.getSelectedItem().toString();
				int sampleRate = Integer.parseInt(samplerateString);
				Log.d(TAG, "Using sample rate "+sampleRate);
				//Spinner spintimer = (Spinner)findViewById(R.id.spin_timer);
				//int timerms = getResources().getIntArray(R.array.timer_ms_array)[spintimer.getSelectedItemPosition()];
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				int timerms = settings.getInt(PREF_TIMER, 0);
				Log.d(TAG, "Using timer "+timerms+"ms");
				name = startService(new Intent(context, AService.class)
						.putExtra(AService.FILEPATH_INTENT_EXTRA, currentRecPath)
						.putExtra(AService.SAMPLERATE_INTENT_EXTRA, sampleRate)
						.putExtra(AService.TIMER_MS_INTENT_EXTRA, timerms)
						.putExtra(AService.MANUALLY_INTENT_EXTRA, true));
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_PATH, currentRecPath);
                editor.commit();
				updateButtons(true);
			}
		});
        Button stopBtn = (Button)findViewById(R.id.btn_stop);
        stopBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if(name!=null){
						stopService(new Intent(context, Class.forName(name.getClassName())));
					} else {
						stopService(new Intent(context, AService.class));
					}
				} catch (ClassNotFoundException e) {
					Log.e(TAG, "class not found...",e);
				}

				// RENAME FILE
				CheckBox overwriteCheckbox = (CheckBox)findViewById(R.id.chk_overwrite);
				boolean rename = overwriteCheckbox.isChecked();
				String msg = "stopped";
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                String currentRecPath = settings.getString(PREF_PATH, null);
				if(rename) {
					if (currentRecPath != null) {
						String newFileName = getAbsoluteFilePathForRecording();
						Log.d(TAG, "renaming " + currentRecPath + " to " + newFileName);
						File currentFile = new File(currentRecPath);
						File newFile = new File(newFileName);
						if (currentFile.exists()) {
							currentFile.renameTo(newFile);
							msg = "saved " + newFile.getName();
						}
					}
				}
				Toast.makeText(AnActivity.this, msg, Toast.LENGTH_LONG).show();

				currentRecPath=null;
				SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_PATH, currentRecPath);
                editor.commit();

				updateButtons(false);
			}
		});

        Button timerBtn = (Button)findViewById(R.id.btn_timer);
		timerBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerDialog mTimePicker;
				mTimePicker = new TimePickerDialog(AnActivity.this, new TimePickerDialog.OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker timePicker, int hour, int minute) {
						int timerms = ((hour*60)+minute)*60*1000;
						SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putInt(PREF_TIMER, timerms);
						editor.commit();
						setTimerDisplay(timerms);
					}
				}, 0,0,true);
				mTimePicker.setTitle("Select Timer Duration");
				mTimePicker.show();
			}
		});

		AdapterView.OnItemSelectedListener spinListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner me = (Spinner)adapterView;
                String text = me.getSelectedItem().toString();
                EditText input = (EditText)findViewById(R.id.txt_recname);
                if(text.length()>0) {
                    input.append(text);
                    input.append(" ");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // we're good...
            }
		};

		//categorySpinner.setOnItemSelectedListener(spinListener);
		//phoneSpinner.setOnItemSelectedListener(spinListener);
		//Spinner citySpinner = (Spinner)findViewById(R.id.spin_city);
		//citySpinner.setOnItemSelectedListener(spinListener);

    }
    
	@Override
	protected void onResume() {
		super.onResume();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String currentRecPath = settings.getString(PREF_PATH, null);
		updateButtons(currentRecPath!=null);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		EditText notestxt = (EditText)findViewById(R.id.txt_recname);
		editor.putString(PREF_NOTES, notestxt.getText().toString());

		EditText citytxt = (EditText)findViewById(R.id.txt_city);
		editor.putString(PREF_CITY, citytxt.getText().toString());

		Spinner categorySpinner = (Spinner)findViewById(R.id.spin_category);
		editor.putInt(PREF_CATEGORY, categorySpinner.getSelectedItemPosition());

		Spinner phoneSpinner = (Spinner)findViewById(R.id.spin_phone);
		editor.putInt(PREF_PHONE, phoneSpinner.getSelectedItemPosition());


		editor.commit();
	}

	private void updateButtons(boolean recording){
        Button startBtn = (Button)findViewById(R.id.btn_start);
        Button stopBtn = (Button)findViewById(R.id.btn_stop);
        if(recording){
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
        } else {
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        }
    }

	private String getAbsoluteFilePathForRecording(){
    	File dir = prepareDirectory();
    	String name = getFileNameFromInput();
		String fileName = System.currentTimeMillis()+"_"+name+".wav";
		File dst = new File(dir, fileName);
		return dst.getAbsolutePath();
	}

	private File prepareDirectory(){

		String directory = Environment.getExternalStorageDirectory().getPath();
		File sdcardDirectory = new File(directory);
		File dir = new File(sdcardDirectory, REC_DIRECTORY);
		if(!dir.exists()){
			Log.d(AnActivity.TAG, "creating directory: "+dir.getPath());
			dir.mkdirs();
		}
		return dir;
	}

	private String getFileNameFromInput(){
		EditText input = (EditText)findViewById(R.id.txt_recname);
		String notes = input.getText().toString().trim().replace(' ', '_');
		EditText citytxt = (EditText)findViewById(R.id.txt_city);
		String city = citytxt.getText().toString().trim();
        Spinner categorySpinner = (Spinner)findViewById(R.id.spin_category);
        String category = categorySpinner.getSelectedItem().toString();
        Spinner phoneSpinner = (Spinner)findViewById(R.id.spin_phone);
        String phoneLocation = phoneSpinner.getSelectedItem().toString();
        StringBuilder fn = new StringBuilder();
        if(category.length()>0){
            fn.append("Rv6_");
            fn.append(category);
            fn.append("_");
        }
        if(phoneLocation.length()>0){
            fn.append(phoneLocation);
            fn.append("_");
        }
        if(city.length()>0){
            fn.append(city);
            fn.append("_");
        }
        fn.append(notes);
        return fn.toString();
	}

	private void setTimerDisplay(int timerms){
		String timerName = getResources().getString(R.string.timer_desc);
		String timeString = timerms==0?"no timer":readableTime(timerms);
		String newText = timerName+": "+ timeString;
		TextView timerTextLbl = (TextView)findViewById(R.id.lbl_timer);
		timerTextLbl.setText(newText);
	}

	public static String readableTime(int ms){
		int sec = ms/1000;
		String rest = "";
		if(sec<60){
			return sec+"s";
		}
		int r = sec%60;
		if(r!=0){
			rest = " "+r+"s"+rest;
		}
		int min = sec/60;
		if(min<60){
			return min+"min"+rest;
		}
		r = min%60;
		if(r!=0){
			rest = " "+r+"min"+rest;
		}
		int h = min/60;
		return h+"h"+rest;
	}
}