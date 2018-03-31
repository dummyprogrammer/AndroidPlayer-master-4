package com.player.licenta.androidplayer;

import android.app.Service;

import java.io.IOException;
import java.util.ArrayList;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import java.util.List;
import java.util.Random;

import android.app.Notification;
import android.app.PendingIntent;
import android.widget.MediaController;

public class MusicService extends Service implements
MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
MediaPlayer.OnCompletionListener, MediaController.MediaPlayerControl
{
    public interface OnSongChangedListener
    {
        void onSongChanged(Song song);
    }

    OnSongChangedListener onSongChangedListener = null;

	//media player
	private MediaPlayer player;
	//song list
	private ArrayList<Song> songs;
	//current position
	private int songIndex;

	private final IBinder musicBind = new MusicBinder();

	private String songTitle = "";
	private static final int NOTIFY_ID = 1;

	private boolean shuffle = false;
	private Random rand;

	private String songPath;

	public void onCreate()
	{
		super.onCreate();
		songIndex = -1;
		player = new MediaPlayer();
		initMusicPlayer();

		rand = new Random();
	}

	public void initMusicPlayer()
	{
		player.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra)
	{
		mediaPlayer.reset();
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer)
	{
		//start playback
		mediaPlayer.start();

		Intent notIntent = new Intent(this, MainActivity.class);
		notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendInt = PendingIntent.getActivity(this, 0,
				notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder builder = new Notification.Builder(this);

		builder.setContentIntent(pendInt)
				.setSmallIcon(R.drawable.play)
				.setTicker(songTitle)
				.setOngoing(true)
				.setContentTitle("Playing")
				.setContentText(songTitle);
		Notification not = builder.build();

		startForeground(NOTIFY_ID, not);
	}

	public void setSong(int songIndex)
	{
		this.songIndex = songIndex;
	}


	@Override
	public IBinder onBind(Intent intent)
	{
		return musicBind;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		player.stop();
		player.release();
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer)
	{
		if (player.getCurrentPosition() > 0)
		{
			mediaPlayer.reset();
			playNext();
            if(onSongChangedListener != null)
            {
                onSongChangedListener.onSongChanged(getCurrentSong());
            }
		}
	}

	public void setList(ArrayList<Song> theSongs)
	{
		songs = theSongs;
	}

	@Override
	public void start()
	{
		player.start();
	}

	@Override
	public void pause()
	{
		player.pause();
	}

	@Override
	public int getDuration()
	{
		return player.getDuration();
	}

	@Override
	public int getCurrentPosition()
	{
		return player.getCurrentPosition();
	}

	@Override
	public void seekTo(int pos)
	{
		player.seekTo(pos);
	}

	@Override
	public boolean isPlaying()
	{
		return player.isPlaying();
	}

	@Override
	public int getBufferPercentage()
	{
		return 0;
	}

	@Override
	public boolean canPause()
	{
		return true;
	}

	@Override
	public boolean canSeekBackward()
	{
		return true;
	}

	@Override
	public boolean canSeekForward()
	{
		return true;
	}

	@Override
	public int getAudioSessionId()
	{
		return 0;
	}

	public void playSong(int index)
	{
		if(index >= 0 && index < songs.size())
		{
			songIndex = index;

			//play a song
			player.reset();

			//get song
			Song playSong = songs.get(songIndex);
			songTitle = playSong.getTitle();

			//get id
			long currSong = playSong.getID();
			//set uri
			Uri trackUri = ContentUris.withAppendedId(
					android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					currSong);

			Context context = getApplicationContext();
			songPath = getRealPathFromURI(context, trackUri);

			try
			{
				player.setDataSource(getApplicationContext(), trackUri);
			}
			catch (Exception e)
			{
				Log.e("MUSIC SERVICE", "Error setting data source", e);
			}

			try
			{
				player.prepare();
				player.start();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void playPrev()
	{
		int prevSongIndex = songIndex - 1;
		if (prevSongIndex < 0)
		{
			prevSongIndex = songs.size() - 1;
		}
		playSong(prevSongIndex);
	}

	//skip to next
	public void playNext()
	{
		int nextSongIndex = songIndex;
		if (shuffle)
		{
			while (nextSongIndex == songIndex)
			{
				nextSongIndex = rand.nextInt(songs.size());
			}
		}
		else
		{
			nextSongIndex++;
			if (nextSongIndex >= songs.size())
			{
				nextSongIndex = 0;
			}
		}
		playSong(nextSongIndex);
	}

	@Override
	public void onDestroy()
	{
		stopForeground(true);
	}

	public void setShuffle()
	{
		shuffle = !shuffle;
	}

	public String getRealPathFromURI(Context context, Uri contentUri)
	{
		Cursor cursor = null;
		try
		{
			String[] proj = {MediaStore.Images.Media.DATA};
			cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}

	public class MusicBinder extends Binder
	{
		MusicService getService()
		{
			return MusicService.this;
		}
	}

	public String getSongPath()
	{
		return songPath;
	}

	public int getSongIndex()
	{
		return songIndex;
	}

	public Song getCurrentSong()
	{
		if(songIndex >= 0 && songIndex < songs.size())
		{
			return songs.get(songIndex);
		}

		return null;
	}

    public void setOnSongFinishedListener(OnSongChangedListener listener)
    {
        onSongChangedListener = listener;
    }

	public void setVolume(int volume)
	{
		if(volume < 0)
		{
			volume = 0;
		}
		else if(volume > 100)
		{
			volume = 100;
		}

		float normalizedVolume = volume/100f;
		player.setVolume(normalizedVolume, normalizedVolume);
	}

    public int getAudioId()
    {
        return player.getAudioSessionId();
    }

}