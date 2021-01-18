package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.content.Context;
import android.media.SoundPool;
import android.media.AudioManager;

import de.hs_kl.imst.gatav.tilerenderer.R;

public class GameSound {
    private static SoundPool soundPool;
    private static int explosion;
    private static int upgrade;
    private static int mist;
    private static int ohno;



    public GameSound(Context context){
        soundPool = new SoundPool(2,AudioManager.STREAM_MUSIC,0);
        explosion = soundPool.load(context, R.raw.explosion,1);
        mist = soundPool.load(context, R.raw.berndsagtmist,1);
        ohno = soundPool.load(context, R.raw.ohno,1);
        upgrade = soundPool.load(context, R.raw.upgradsound,1);


    }

    public void playExplosionSound(){
        soundPool.play(explosion,1.0f,1.0f,1,0,1.0f);
    }
    public void playMistSound(){
        soundPool.play(mist,1.0f,1.0f,2,0,1.0f);
    }
    public void playOhNoSound(){
        soundPool.play(ohno,1.0f,1.0f,2,0,1.0f);
    }
    public void playUpgradeSound(){
        soundPool.play(upgrade,1.0f,1.0f,2,0,1.0f);
    }
}
