package com.player.licenta.androidplayer;

import com.player.licenta.androidplayer.MusicService.MusicBinder;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

public class SongPickedActivity extends Activity
{
	private boolean musicBound=false;
	private boolean paused=false, playbackPaused=false;
	private MusicController controller;
	private ImageView coverArt;
	private MusicService musicSrv;
	private ArrayList<Song> songList;
	private Intent playIntent;
    private SongPickedActivity mInstance;


	private final static String TAG = "SongPickedActivity";

	private Context context;

	//connect to the service
	private ServiceConnection musicConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			MusicBinder binder = (MusicBinder)service;
			musicSrv = binder.getService();
			musicSrv.setList(songList);
			musicBound = true;
            setController();
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			musicBound = false;
		}
	};

	private void showControllerDelayed()
	{
		runOnUiThread(new Runnable() {

			@Override
			public void run()
			{
				final Handler handler = new Handler();
				handler.postDelayed(
                    new Runnable()
				{
					@Override
					public void run()
					{
                        controller.show();
                        controller.clearFocus();
					}
				}, 100);
			}
		});
	}

	@Override
	public ActionBar getActionBar() {
		return super.getActionBar();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
 		super.onCreate(savedInstanceState);

		setContentView(R.layout.song_picked);

        mInstance = this;

		// Get the message from the intent
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();

		songList = (ArrayList<Song>)intent.getSerializableExtra("songlist");

		String songFilePath = extras.getString("SONG_PATH");
		String songArtist = extras.getString("SONG_ARTIST");
		String songTitle = extras.getString("SONG_TITLE");

		coverArt = (ImageView)findViewById(R.id.coverArt);

        updateTitle(songArtist, songTitle);
		extractAlbumArt(songFilePath);

		Log.d(TAG, "onCreate() called");
	}

    private void updateTitle(String artist, String songName)
    {
        String title = artist + " - " + songName;
        setTitle(title);
    }

	@Override
	protected void onStart()
	{
		super.onStart();
		if(playIntent == null)
		{
			playIntent = new Intent(this, MusicService.class);
            startService(playIntent);
			bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);

		}
	}

	@Override
	protected void onPause()
	{
        if (controller != null)
        {
            controller.hide();
        }
		super.onPause();
		Log.d(TAG, "onPause() called");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.song_picked, menu);
		return true;
	}
	@Override
	protected void onStop()
	{
		super.onStop();
        stopService(playIntent);
		Log.d(TAG, "onStop() called");
	}

	@Override
	protected void onDestroy()
	{
		Log.d(TAG, "onDestroy called");

        if(controller != null)
        {
            controller.hide();
        }
        if(musicSrv != null){
            unbindService(musicConnection);
        }
		super.onDestroy();
	}


/*	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) 
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}*/

	public void extractAlbumArt(String songFilePath)
	{
		android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		mmr.setDataSource(songFilePath);

		byte[] data = mmr.getEmbeddedPicture();
		if (data != null)
		{
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			coverArt.setImageBitmap(bitmap); //associated cover art in bitmap
		}
		else
		{
			coverArt.setImageResource(R.drawable.fallback_cover); //any default cover resource folder
		}

		/*coverArt.setAdjustViewBounds(true);
		coverArt.setLayoutParams(new RelativeLayout.LayoutParams(1000, 500));
		coverArt.getLayoutParams().height = 1000;*/
		//coverArt.getLayoutParams().width =  ;

		coverArt.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext())
		{
			@Override
			public void onSwipeRight()
			{
//				Toast.makeText(getApplicationContext(), "Swipe right detected",
//						Toast.LENGTH_LONG).show();
				onBackPressed();
			}
		});
	}


	private void setController()
	{		//set the controller up
		controller = new MusicController(this);
		controller.setMediaPlayer(musicSrv);
		controller.setAnchorView(findViewById(R.id.coverArt));
		controller.setEnabled(true);
		controller.setPrevNextListeners(
				new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						musicSrv.playNext();
						updateArtwork();
					}
				},
				new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						musicSrv.playPrev();
						updateArtwork();
					}
				}
		);
        musicSrv.setOnSongFinishedListener(
                new MusicService.OnSongChangedListener()
                {
                    @Override
                    public void onSongChanged(Song newSong)
                    {
                        updateArtwork();
                    }
                }
        );
        showControllerDelayed();
	}

	private void updateArtwork()
	{
		Song currentSong = musicSrv.getCurrentSong();
		if(currentSong != null)
		{
            extractAlbumArt(musicSrv.getSongPath());

            String songTitle = currentSong.getTitle().toString();
			String songArtist = currentSong.getArtist().toString();
            updateTitle(songArtist, songTitle);
        }
	}


	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		//finish();
		Log.d(TAG, "onBackPressed called");
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if(controller != null)
		{
			controller.show();
		}
	}

    //Open Equalizer Activity
    public void openEqualizerActivity(MenuItem item)
    {
        Intent intent = new Intent(this, EqualizerActivity.class);
        startActivity(intent);
    }

}
