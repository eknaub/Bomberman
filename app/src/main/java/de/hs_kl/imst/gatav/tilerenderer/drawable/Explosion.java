package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Color;

import java.io.InputStream;

public class Explosion extends TileGraphics {
    private int explosionTime = 1; //how long until boom is gone

    public Explosion(int x, int y, InputStream is) {
        super(x, y, is);

        tilePaint.setColor(Color.parseColor("#149942"));
    }

    public int getExplosionTime() {return explosionTime; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPassable() {
        return true;
    }
}
