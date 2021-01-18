package de.hs_kl.imst.gatav.tilerenderer;

import android.content.Intent;
import android.content.res.AssetManager;
import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer startscreensound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Sound
        startscreensound = MediaPlayer.create(MainActivity.this, R.raw.startscreensound);
        startscreensound.setLooping(true);
        startscreensound.start();

        AssetManager am = getResources().getAssets();
        ArrayList<String> levelList = new ArrayList<String>();  // alle Level-Namen ohne .txt

        try {
            String[] files = am.list("levels");
            for(String s : files) {
                if(!s.endsWith(".txt")) continue;
                s = s.substring(0, s.lastIndexOf("."));
                levelList.add(s);
            }
        }catch(IOException e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, levelList);

        ListView listView = findViewById(R.id.level_list);
        listView.setAdapter(itemsAdapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
                String level = (String) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, MainGameActivity.class);
                intent.putExtra("level", level);
                startActivity(intent);
			}
		});
    }

    @Override
    protected void onStop() {
        super.onStop();
        startscreensound.release();
        finish();
    }
}