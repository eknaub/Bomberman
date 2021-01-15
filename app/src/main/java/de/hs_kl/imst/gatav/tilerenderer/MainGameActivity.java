package de.hs_kl.imst.gatav.tilerenderer;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainGameActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String level=getIntent().getExtras().getString("level");

        gameView = new GameView(this, level);
        setContentView(gameView);
    }
}
