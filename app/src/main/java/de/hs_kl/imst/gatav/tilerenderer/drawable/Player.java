package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.io.InputStream;

public class Player extends MovableTileGraphics {
    private int explosionRadius = 1; //initial 1, can get upgraded with Upgrades

    public Player(int x, int y, InputStream is) {
        super(x, y, is);

        tilePaint.setColor(Color.parseColor("#F0CC00"));
    }

    public int getExplosionRadius() { return explosionRadius; }
    public void incrementExplosionRadius() { explosionRadius++; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPassable() {
        return false;
    }
}
