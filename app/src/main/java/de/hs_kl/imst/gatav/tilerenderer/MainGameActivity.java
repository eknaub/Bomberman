package de.hs_kl.imst.gatav.tilerenderer;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;

public class MainGameActivity extends AppCompatActivity {

    private GameView gameView;
    private MediaPlayer hintergrund;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hintergrund = MediaPlayer.create(MainGameActivity.this, R.raw.background);
        hintergrund.setLooping(true);
        hintergrund.start();
        String level = getIntent().getExtras().getString("level");

        gameView = new GameView(this, level);
        setContentView(gameView);
    }
    @Override
    protected void onStop() {
        super.onStop();
        hintergrund.release();
        finish();
    }
}
