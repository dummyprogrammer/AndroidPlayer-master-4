package com.player.licenta.androidplayer;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Song implements Serializable
{
	private long id;
	private String title;
	private String artist;
	
	public Song(long songID, String songTitle, String songArtist) 
	{
		id=songID;
		title=songTitle;
		artist=songArtist;
	}
	
	public long getID()
	{
		return id;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public String getArtist()
	{
		return artist;
	}

}
