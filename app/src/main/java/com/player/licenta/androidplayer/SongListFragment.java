package com.player.licenta.androidplayer;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by razvan.milea on 3/29/2018.
 */

public class SongListFragment extends Fragment{

    private ListView listSongview;
    List<Song> songList = new ArrayList<Song>();
    private MusicService.OnSongChangedListener songChangedLister;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.songs_fragment_layout, container, false);
        listSongview = (ListView) view.findViewById(R.id.song_list);

        configureAdapter();
        setSongChangedListener();
        setListViewOnclickListener();

        return view;
    }

    private void configureAdapter() {
        listSongview.setAdapter(((MainActivity)getActivity()).getSongAdapter());
    }

    private void setSongChangedListener() {
        songChangedLister = new MusicService.OnSongChangedListener()
        {
            @Override
            public void onSongChanged(Song newSong)
            {
                ((MainActivity)getActivity()).getSongAdapter().setHighlightRow(((MainActivity)getActivity())
                        .getMusicService().getSongIndex());
                ((MainActivity)getActivity()).getMusicService().setOnSongFinishedListener(songChangedLister);
            }
        };
    }

    private void setListViewOnclickListener(){
        listSongview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((MainActivity)getActivity()).songPicked(position);
                ((MainActivity)getActivity()).setupViewPager(1);
            }
        });
    }


}
