package com.player.licenta.androidplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

public class EqualizerActivity extends Activity
{
    private static final String TAG = "AudioFxDemo";
    private static final float VISUALIZER_HEIGHT_DIP = 50f;
    private MediaPlayer mMediaPlayer;
    private Visualizer mVisualizer;
    private Equalizer mEqualizer;
    private LinearLayout mLinearLayout;
    private VisualizerView mVisualizerView;
    private TextView mStatusTextView;
    private MusicService musicSrv;
    private boolean musicBound = false;
    private ArrayList<Song> songList;
    private Intent playIntent;
    private int[] progressValues=new int[5];
    private int index=0;

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicSrv = binder.getService();
            //musicSrv.setList(songList);
            musicBound = true;
            setupVisualizerFxAndUI();
            setupEqualizerFxAndUI();
            mVisualizer.setEnabled(true);
            //setController();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            musicBound = false;
        }
    };

    @Override
    protected void onStart()
    {
        super.onStart();
        if (playIntent == null)
        {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        if(musicSrv != null){
            unbindService(musicConnection);
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(progressValues.length>=5){
            loadArray();
        }
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mStatusTextView = new TextView(this);
        mLinearLayout = new LinearLayout(this);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.addView(mStatusTextView);
        mLinearLayout.setBackgroundColor(Color.parseColor("#f06d00"));
        setContentView(mLinearLayout);
        // Create the MediaPlayer
        //mMediaPlayer = MediaPlayer.create(this, R.raw.test_cbr);
        //Log.d(TAG, "MediaPlayer audio session ID: " + musicSrv.getAudioSessionId());

        // Make sure the visualizer is enabled only when you actually want to receive data, and
        // when it makes sense to receive data.

        // When the stream ends, we don't need to collect any more data. We don't do this in
        // setupVisualizerFxAndUI because we likely want to have more, non-Visualizer related code
        // in this callback.
        /*
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            public void onCompletion(MediaPlayer mediaPlayer)
            {
                mVisualizer.setEnabled(false);
            }
        });
        mMediaPlayer.start();
        mStatusTextView.setText("Playing audio...");
        */
    }

    private void setupEqualizerFxAndUI()
    {
        // Create the Equalizer object (an AudioEffect subclass) and attach it to our media player,
        // with a default priority (0).
        mEqualizer = new Equalizer(0, musicSrv.getAudioId());
        mEqualizer.setEnabled(true);
        TextView eqTextView = new TextView(this);
        eqTextView.setText("Equalizer:");
        mLinearLayout.addView(eqTextView);
        short bands = mEqualizer.getNumberOfBands();
        final short minEQLevel = mEqualizer.getBandLevelRange()[0];
        final short maxEQLevel = mEqualizer.getBandLevelRange()[1];
        for (short i = 0; i < bands; i++)
        {
            final short band = i;
            index=i;
            TextView freqTextView = new TextView(this);
            freqTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + " Hz");
            mLinearLayout.addView(freqTextView);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            TextView minDbTextView = new TextView(this);
            minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            minDbTextView.setText((minEQLevel / 100) + " dB");
            TextView maxDbTextView = new TextView(this);
            maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            maxDbTextView.setText((maxEQLevel / 100) + " dB");
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.weight = 1;
            SeekBar bar = new SeekBar(this);
            bar.setLayoutParams(layoutParams);
            bar.setMax(maxEQLevel - minEQLevel);
            int bandLevel = mEqualizer.getBandLevel(band);
            int progressValue = bandLevel - minEQLevel;
            bar.setProgress(progressValue);
            progressValues[band] = progressValue;
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser)
                {
                    int bandValue = progress + minEQLevel;
                    mEqualizer.setBandLevel(band, (short) (bandValue));
                    progressValues[band] = progress;
                }

                public void onStartTrackingTouch(SeekBar seekBar)
                {
                }

                public void onStopTrackingTouch(SeekBar seekBar)
                {
                }
            });
            row.addView(minDbTextView);
            row.addView(bar);
            row.addView(maxDbTextView);
            mLinearLayout.addView(row);
        }
    }

    private void setupVisualizerFxAndUI()
    {
        // Create a VisualizerView (defined below), which will render the simplified audio
        // wave form to a Canvas.
        mVisualizerView = new VisualizerView(this);
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                (int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
        mLinearLayout.addView(mVisualizerView);
        // Create the Visualizer object and attach it to our media player.
        mVisualizer = new Visualizer(musicSrv.getAudioId());
        mVisualizer.setEnabled(false);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener()
        {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate)
            {
                mVisualizerView.updateVisualizer(bytes);
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate)
            {
            }
        }, Visualizer.getMaxCaptureRate() / 2, true, false);
    }


    @Override
    protected void onPause()
    {
        super.onPause();
        saveArray();

    }

    public void saveArray() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(progressValues +"_size", progressValues.length);
        for(int i=0;i<progressValues.length;i++)
            editor.putInt(progressValues + "_" + i, progressValues[i]);
    }

    public void loadArray() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("preferencename", 0);
        int size = prefs.getInt(progressValues + "_size", MODE_PRIVATE);
        for(int i=0;i<size;i++)
            progressValues[i] = prefs.getInt(progressValues + "_" + i, 0);
    }

}


    /**
     * A simple class that draws waveform data received from a
     * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
     */
    class VisualizerView extends View
    {
        private byte[] mBytes;
        private float[] mPoints;
        private Rect mRect = new Rect();
        private Paint mForePaint = new Paint();

        public VisualizerView(Context context)
        {
            super(context);
            init();
        }

        private void init()
        {
            mBytes = null;
            mForePaint.setStrokeWidth(1f);
            mForePaint.setAntiAlias(true);
            mForePaint.setColor(Color.rgb(0, 128, 255));
        }

        public void updateVisualizer(byte[] bytes)
        {
            mBytes = bytes;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            if (mBytes == null)
            {
                return;
            }

            if (mPoints == null || mPoints.length < mBytes.length * 4)
            {
                mPoints = new float[mBytes.length * 4];
            }
            mRect.set(0, 0, getWidth(), getHeight());
            for (int i = 0; i < mBytes.length - 1; i++)
            {
                int index = i * 4;
                mPoints[index] = mRect.width() * i / (mBytes.length - 1);
                mPoints[index + 1] = mRect.height() / 2
                        + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
                mPoints[index + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
                mPoints[index + 3] = mRect.height() / 2
                        + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
            }
            canvas.drawLines(mPoints, mForePaint);
        }
    }

