package com.cortxt.app.mmcui.Activities;

import java.text.DateFormat;
import java.util.Date;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cortxt.app.mmcui.R;
import com.cortxt.app.mmcutility.DataObjects.EventType;
import com.cortxt.app.mmcutility.Utils.CommonIntentActionsOld;
import com.cortxt.app.mmcutility.Utils.CommonIntentBundleKeysOld;
import com.cortxt.app.mmcutility.Utils.TaskHelper;
import com.cortxt.com.mmcextension.EventTriggers.SpeedTestTrigger;
import com.cortxt.com.mmcextension.EventTriggers.VideoTestTrigger;

public class VideoTest extends MMCActiveTest implements SurfaceHolder.Callback {

	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private TextView textBufferProg, textPlayProg, textStalls, textStallTime, textDuration;
	private ProgressBar mPlayProgress;
	
	private boolean bSurfaceSet = false, bSurfaceReady = false;
	private static final String TAG = VideoTest.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view  = inflater.inflate(R.layout.video_test, null, false);
		this.setContentView(view);
        init ();
		//getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY);
		
		MMCActivity.customizeTitleBar (this,view,R.string.eventtype_videoTest, R.string.eventtype_videoTest);

		mTechnologyIcon = (ImageView) view.findViewById(R.id.carrierGenerationImage);
		textBufferProg = (TextView)view.findViewById(R.id.textBufferProg);
		textPlayProg = (TextView)view.findViewById(R.id.textPlayProg);
		textStalls = (TextView)view.findViewById(R.id.textStalls);
		textDuration = (TextView)view.findViewById(R.id.textDuration);
		textStallTime = (TextView)view.findViewById(R.id.textStallTime);
		mBufferProgress = (ProgressBar)view.findViewById(R.id.buffer_progress);
		mPlayProgress = (ProgressBar)view.findViewById(R.id.play_progress);

        setShareText();

		surfaceView = (SurfaceView) view.findViewById(R.id.surfaceView1);
		
		surfaceHolder = surfaceView.getHolder();
		  surfaceHolder.addCallback(this);
		  surfaceHolder.setKeepScreenOn(true);
		  //surfaceHolder.setFixedSize(176, 144);
		  surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		  
	}

    @Override
    protected void startTest ()
    {
        Intent intent = new Intent(CommonIntentActionsOld.ACTIVE_TEST);
        intent.putExtra(CommonIntentBundleKeysOld.EXTRA_SPEED_TRIGGER, 0);
        intent.putExtra(CommonIntentBundleKeysOld.EXTRA_TEST_TYPE, EventType.VIDEO_TEST.getIntValue());
        sendBroadcast(intent);
    }

	@Override
	public void shareClicked(View button) {
		temporarilyDisableButton(button);
		

			float density = getResources().getDisplayMetrics().density;
			int width = getResources().getDisplayMetrics().widthPixels;

			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					String message;
					int customSocialText = (getResources().getInteger(R.integer.CUSTOM_SOCIALTEXT));
					String subject = getString((customSocialText == 1)?R.string.sharecustomsubject_speedtest:R.string.sharemessagesubject_speedtest);
					
					int activeconn = ActiveConnection ();
					if(activeconn == 10)
					{
						message = getString((customSocialText == 1)?R.string.sharecustom_speedtest_wifi:R.string.sharemessage_speedtest_wifi);
						subject = getString((customSocialText == 1)?R.string.sharecustomsubject_speedtestwifi:R.string.sharemessagesubject_speedtestwifi);
					}
					else if (activeconn == 11)
					{
						message = getString((customSocialText == 1)?R.string.sharecustom_speedtest_wimax:R.string.sharemessage_speedtest_wimax);
						subject = getString((customSocialText == 1)?R.string.sharecustomsubject_speedtestwimax:R.string.sharemessagesubject_speedtestwimax);
					}
					else
						message = getString((customSocialText == 1)?R.string.sharecustom_speedtest_poorspeed:R.string.sharemessage_speedtest_poorspeed);
					TaskHelper.execute(
							new ShareTask(VideoTest.this, message, subject, findViewById(R.id.webtest_container))); // .execute((Void[])null);
				}
			}, 1);
			
		}
	//}

	private void setShareText() {
		String carrier = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName();
		int activeconn = ActiveConnection ();
		if (activeconn == 10)
			carrier = "WiFi";
		else if (activeconn == 11)
			carrier = "WiMAX";
		else if (activeconn == 12)
			carrier = "Ethernet";
		String phone = "";
		if(android.os.Build.BRAND.length() > 0) {
			String brand = android.os.Build.BRAND.substring(0, 1).toUpperCase() + android.os.Build.BRAND.substring(1);
			phone = " - " + brand + " " + android.os.Build.MODEL;
		}
		long timeStamp = System.currentTimeMillis();
		DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
		String dateTime = dateTimeFormat.format(new Date(timeStamp));
		
		String shareText = carrier + phone + "\n" + dateTime;
		//mShareText.setText(shareText);
	}
	
	/**
	 * Set download speed on gauge and rotate needle
	 * @param downSpeed download speed in bits/second
	 */
	private void setGaugeDownloadSpeed(int downSpeed) {
	}

    protected void setProgress (int _bufferProgress, int _playProgress, int stallCount, int _stallTime, int _accessDelay, int _playDelay, int _duration, int _downloadTime, int _rxbytes)
	{

		float bufferProgress, playProgress, accessDelay, playDelay, stallTime, duration, downloadTime;
		bufferProgress = (int)(_bufferProgress / 100) / 10f;
		playProgress = (int)(_playProgress / 100) / 10f;
		accessDelay = (int)(_accessDelay / 100) / 10f;
		playDelay = (int)(_playDelay / 100) / 10f;
		stallTime = (int)(_stallTime / 100) / 10f;
		duration = (int)(_duration / 100) / 10f;
		downloadTime = (int)(_downloadTime / 100) / 10f;

		if (_bufferProgress < 50)
			textBufferProg.setText(""+bufferProgress + "%");
		else
			textBufferProg.setText(""+bufferProgress);
		textPlayProg.setText(""+playProgress);

		String durationLine = getString(R.string.activetest_buffertime) + ": " + downloadTime;
//		if (bufferProgress >= duration) {
//			float score = (int)(bufferProgress * 100 / downloadTime) / 100f;
//			durationLine += "   " + getString(R.string.activetest_score) + ": " + bufferProgress + "/" + downloadTime + " = " + score;
//		}
		if ((_bufferProgress > 0 || _rxbytes > 100) && _downloadTime > 0) {

			int bitRate = (int) (_rxbytes * 8 / _downloadTime);
			double mbps = (bitRate / 10) / 100.0;
			durationLine += "   " + getString(R.string.activetest_rate) + ": " + mbps + " Mb/s";
		}
		textStalls.setText(getString(R.string.activetest_accessdelay) + ": " + accessDelay + "   " + getString(R.string.activetest_playdelay) + ": " + playDelay);
		textDuration.setText(durationLine);
		textStallTime.setText(getString(R.string.activetest_stalls) + ": " + stallCount + "   " + getString(R.string.activetest_stalltime) + ": " + stallTime);
		
		if (duration > 0)
		{
			int percentBuffer = _bufferProgress * 100 / _duration;
			int percentPlay = _playProgress * 100 / _duration;

			mBufferProgress.setProgress(percentBuffer);
			mPlayProgress.setProgress(percentPlay);

        }
		eventType = EventType.VIDEO_TEST.getIntValue();
	}

    @Override
    protected void handleTestProgress (Intent intent)
    {
        if(intent.hasExtra(CommonIntentBundleKeysOld.EXTRA_BUFFER_PROGRESS)) {
            int bufferProgress = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_BUFFER_PROGRESS, 0);
            int playProgress = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_PLAY_PROGRESS, 0);
            int stallCount = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_STALLS, 0);
            int stallTime = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_STALL_TIME, 0);
            int duration = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_DURATION, 0);
            int accessDelay = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_ACCESS_DELAY, 0);
            int playDelay = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_PLAY_DELAY, 0);
			int downloadTime = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_DOWNLOAD_TIME, 0);
            int eventtype = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_EVENTTYPE, 0);
			int rxbytes = intent.getIntExtra(CommonIntentBundleKeysOld.EXTRA_RXBYTES, 0);
            if (eventtype != EventType.VIDEO_TEST.getIntValue())
                return;
            setProgress(bufferProgress, playProgress, stallCount, stallTime, accessDelay, playDelay, duration, downloadTime, rxbytes);

        }
        else {

            if (VideoTestTrigger.getMediaPlayer() != null && bSurfaceReady == true && bSurfaceSet == false)
            {
                try
                {
                    VideoTestTrigger.getMediaPlayer().setDisplay(surfaceHolder);
                    bSurfaceSet = true;
                }
                catch (Exception e) {
                    bSurfaceSet = false;
                }

            }
        }
    }

    @Override
    protected void handleTestComplete ()
    {
        mPlayProgress.setProgress(100);
        textPlayProg.setText(R.string.GenericText_Done);
		if (VideoTestTrigger.getMediaPlayer() != null) {
			surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
			surfaceHolder.setFormat(PixelFormat.OPAQUE);
			VideoTestTrigger.getMediaPlayer().reset();
			VideoTestTrigger.getMediaPlayer().release();
			VideoTestTrigger.setMediaPlayer (null);
		}
		bSurfaceSet = false;
    }

    @Override
    protected void handleTestError ()
    {
        bSurfaceSet = false;
    }

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        bSurfaceReady = true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
	
}
 