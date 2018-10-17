package at.tugraz.student.aschmidhofer.tmdaudiorecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

public class AService extends Service {


	public static final String FILEPATH_INTENT_EXTRA = "filePathForTheRecording";
	public static final String SAMPLERATE_INTENT_EXTRA = "sampleRateToUse";
	public static final String TIMER_MS_INTENT_EXTRA = "useTimer";
	public static final String MANUALLY_INTENT_EXTRA = "manualUserEvent";
	
	public static final String ACTION_STOP = "stopAction";
	private static final int RECORDING_NOTIFICATION_ID = 0x101F101;

	private WavAudioRecorder rec;
	private boolean manuallyStarted = false;

	public AService() {
		super();
	}
	
	@Override
	public void onCreate() {
//		Log.i(RecordingActivity.TAG, "create");
		super.onCreate();
	}


	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if(ACTION_STOP.equals(intent.getAction())){
			Log.i(AnActivity.TAG, "started to stop");
			if((!manuallyStarted)|(intent.getBooleanExtra(MANUALLY_INTENT_EXTRA, false)))
				stopSelf();
			return;
		}

		if(rec!=null){
			// rec already started
			Toast.makeText(this, "still recording...", Toast.LENGTH_LONG).show();
			return; 
		}

		manuallyStarted = intent.getBooleanExtra(MANUALLY_INTENT_EXTRA, false);
		String filepath = intent.getStringExtra(FILEPATH_INTENT_EXTRA);
		if(filepath==null){
			Toast.makeText(this, "no file path...", Toast.LENGTH_LONG).show();
			return;
		}
		Log.i(AnActivity.TAG, "start:"+ startId);

		Log.d(AnActivity.TAG, "path: "+filepath);

		// SAMPLE RATE
		int sampleRate = intent.getIntExtra(SAMPLERATE_INTENT_EXTRA, 0);

		if (sampleRate == 0) {
			rec = WavAudioRecorder.getInstanse();
		} else{
			rec = WavAudioRecorder.getInstanse(sampleRate);
		}
		rec.setOutputFile(filepath);

		// TIMER
		int timerms = intent.getIntExtra(TIMER_MS_INTENT_EXTRA, 0);
		long endTime = 0;
		if(timerms>0){
			endTime = System.currentTimeMillis()+timerms;
			rec.setEndTime(endTime, this);
		}

		// START RECORDING
		rec.prepare();
		//Log.d(AnActivity.TAG, "rec state "+rec.getState());
		rec.start();
		Log.d(AnActivity.TAG, "rec state "+rec.getState());
		if (rec.getState()==WavAudioRecorder.State.RECORDING) {
			String toastText = "recording...";
			if(timerms>0){
				toastText = "recording for "+AnActivity.readableTime(timerms);
			}
			Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
			startForeground(endTime);
		} else {
			rec.release();
			rec = null;
		}



	}

	private void startForeground(long timerend){
		String msg = "recording...";
		if(timerend>0){
			//CharSequence hours = DateFormat.format("HH:MM ", timerend);
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			sdf.setTimeZone(TimeZone.getDefault());
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(timerend);
			String hours = sdf.format(c.getTime());
			msg = "recording until "+hours;
		}
		Log.d(AnActivity.TAG,msg);

		//Notification notification = new Notification(android.R.drawable.stat_notify_more, msg, System.currentTimeMillis());
		//AService.class).setAction(ACTION_STOP).putExtra(MANUALLY_INTENT_EXTRA, true);
		//PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,notificationIntent,0);//PendingIntent.getService(this, 0, notificationIntent, 0);
		//notification.setLatestEventInfo(this, "Andiorec", msg, contentIntent);

        Intent intent = new Intent(this, AnActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent,0);
        Notification.Builder builder = new Notification.Builder(this)
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(msg);
        Notification notification = builder.build();
        startForeground(RECORDING_NOTIFICATION_ID, notification);
	}

	@Override
	public void onDestroy() {
		Log.i(AnActivity.TAG, "destroy");
		if(rec!=null) {
			rec.release();
			rec = null;
			//Toast.makeText(this, "stopped...", Toast.LENGTH_LONG).show();
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
//		Log.e(RecordingActivity.TAG, "bind");
		// you shoudln't bind this service...
		return null;
	}

}
