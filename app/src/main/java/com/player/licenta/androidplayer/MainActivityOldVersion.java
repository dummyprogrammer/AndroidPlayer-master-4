package com.player.licenta.androidplayer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.player.licenta.androidplayer.MusicService.MusicBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivityOldVersion extends Activity
{
    private ArrayList<Song> songList;
    private ListView songView;
    //private SeekBar volumeControl;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;


    private MusicController controller;

    private SongAdapter songAdapter;

    private boolean paused=false, playbackPaused=false;

    private MusicService.OnSongChangedListener songChangedLister;

    public final static String EXTRA_MESSAGE = "com.mycompany.myfirstapp.MESSAGE";
    private final static String TAG = "MainActivity";

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MusicBinder binder = (MusicBinder)service;
            musicSrv = binder.getService();
            musicSrv.setList(songList);
            musicSrv.setOnSongFinishedListener(songChangedLister);
            musicBound = true;
            setController();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            musicBound = false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        if (checkAndRequestPermissions()){
            getSongList();
        }

        Collections.sort(songList, new Comparator<Song>()
        {
            public int compare(Song a, Song b)
            {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        songAdapter = new SongAdapter(this, songList);
        songView.setAdapter(songAdapter);

        songChangedLister = new MusicService.OnSongChangedListener()
        {
            @Override
            public void onSongChanged(Song newSong)
            {
                songAdapter.setHighlightRow(musicSrv.getSongIndex());
            }
        };

        Log.d(TAG, "onCreate called");
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if(playIntent==null)
        {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (controller != null)
        {
            controller.show();
            songAdapter.setHighlightRow(musicSrv.getSongIndex());
            musicSrv.setOnSongFinishedListener(songChangedLister);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    getSongList();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private  boolean checkAndRequestPermissions() {

        int permissionSendMessage = ContextCompat.checkSelfPermission(this,  Manifest.permission.RECORD_AUDIO);
        int readExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (readExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    public void getSongList()
    {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst())
        {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do
            {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    public void songPicked(View view)
    {
        try
        {
            int songIndex = Integer.parseInt(view.getTag().toString());
            if(songIndex >= 0)
            {
                if (songIndex != musicSrv.getSongIndex())
                {
                    musicSrv.playSong(songIndex);
                    songAdapter.setHighlightRow(musicSrv.getSongIndex());
                }
                controller.show();
                showCoverArtActivity(view);
            }
        }
        catch(NumberFormatException ex)
        {
            ex.printStackTrace();
        }
    }

    private void showCoverArtActivity(View view)
    {
        Intent intent = new Intent(this, SongPickedActivity.class);
        Integer index = Integer.parseInt(view.getTag().toString());

        Song currentSong  = (Song)songList.get(index);
        String songTitle = currentSong.getTitle().toString();
        String songArtist = currentSong.getArtist().toString();

        String songPath = musicSrv.getSongPath();

        Bundle extras = new Bundle();
        extras.putString("SONG_PATH", songPath);
        extras.putString("SONG_ARTIST", songArtist);
        extras.putString("SONG_TITLE", songTitle);

        intent.putExtras(extras);

        intent.putExtra("songlist", songList);

        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //menu item selected
        switch (item.getItemId())
        {
            case R.id.action_end:
                finish();
                break;

            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy called");

        controller.hide();
        stopService(playIntent);
        if (musicBound)
        {
            unbindService(musicConnection);
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {

        super.onBackPressed();
        Log.d(TAG, "onBackPressed called");
    }

    private void setController()
    {		//set the controller up
        controller = new MusicController(this);

        controller.setPrevNextListeners(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        musicSrv.playNext();
                        songAdapter.setHighlightRow(musicSrv.getSongIndex());
                    }
                },
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        musicSrv.playPrev();
                        songAdapter.setHighlightRow(musicSrv.getSongIndex());
                    }
                }
        );
        MusicController.LayoutParams layoutParams = new MusicController.LayoutParams(controller.getLayoutParams());
        controller.setLayoutParams(layoutParams);
        controller.setMediaPlayer(musicSrv);
        controller.setAnchorView(findViewById(R.id.rootView));
        controller.setEnabled(true);
        controller.show();
    }
}
