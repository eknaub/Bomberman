package de.hs_kl.imst.gatav.tilerenderer.screens;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import de.hs_kl.imst.gatav.tilerenderer.MainActivity;
import de.hs_kl.imst.gatav.tilerenderer.MainGameActivity;
import de.hs_kl.imst.gatav.tilerenderer.R;

public class EndScreen extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_screen);

        int gameMode = getIntent().getExtras().getInt("gameMode");

        TextView gameState = findViewById(R.id.gamestate_textView);
        if(gameMode == 2)
            gameState.setText("YOU WON!");
        if(gameMode == 3)
            gameState.setText("YOU LOST!");

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EndScreen.this, MainActivity.class);
                //Damit der Intent die neue Wurzel ist
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }
}