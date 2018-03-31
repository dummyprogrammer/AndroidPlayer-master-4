package com.player.licenta.androidplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by razvan.milea on 3/30/2018.
 */

public class SongCoverFragment extends Fragment {

    private ImageView coverArtView;
    List<Song> songList = new ArrayList<Song>();
    private MusicService.OnSongChangedListener songChangedLister;
    private static final String TAG = "SongCoverFragment";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.song_cover_fragment_layout, container, false);
        coverArtView = (ImageView) view.findViewById(R.id.coverArt);


        return view;
    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        if (visible) {
            extractAlbumArt(getSongPath());
        }

        super.setMenuVisibility(visible);
    }



    public void extractAlbumArt(String songFilePath)
    {
        if( songFilePath != null){

            android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(songFilePath);

            byte[] data = mmr.getEmbeddedPicture();
            if (data != null)
            {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                coverArtView.setImageBitmap(bitmap); //associated cover art in bitmap
            }
            else
            {
                coverArtView.setImageResource(R.drawable.fallback_cover); //any default cover resource folder
            }
            Log.d(TAG, "extract cover art called");
        }

    }

    public String getSongPath(){
        return ((MainActivity)getActivity()).getSongPath();
    }



}
