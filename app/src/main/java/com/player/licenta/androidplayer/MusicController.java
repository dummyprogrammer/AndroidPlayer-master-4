package com.player.licenta.androidplayer;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.MediaController;

public class MusicController extends MediaController 
{
	
	public MusicController(Context context)
	{
		super(context);
	}

    public MusicController(Context context, AttributeSet attrs) {

        super(context, attrs); // This should be first line of constructor
    }

	@Override
	public void show()
	{
		super.show(0);
	}

    @Override
    public void show(int timeout)
    {
        super.show(0);
    }

    @Override
	public void hide()
	{
		//super.hide();
	}

	@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK ) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                ((Activity) getContext()).onBackPressed();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

}