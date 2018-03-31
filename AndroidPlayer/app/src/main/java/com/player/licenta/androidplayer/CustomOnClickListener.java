package com.player.licenta.androidplayer;

import java.util.ArrayList;
import java.util.List;

import android.view.View;

public class CustomOnClickListener implements View.OnClickListener

{
    List<View.OnClickListener> listeners;

    public CustomOnClickListener()
    {
        listeners = new ArrayList<View.OnClickListener>();
    }

    public void addOnClickListener(View.OnClickListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void onClick(View v)
    {
       for(View.OnClickListener listener : listeners)
       {
          listener.onClick(v);
       }
    }
}