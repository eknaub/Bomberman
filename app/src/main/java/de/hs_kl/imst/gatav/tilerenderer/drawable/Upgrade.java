package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Color;
import java.io.InputStream;

public class Upgrade extends TileGraphics {
    static private double dropChance = 0.25;

    public Upgrade(int x, int y, InputStream is) {
        super(x, y, is);

        tilePaint.setColor(Color.parseColor("#149942"));
    }

    public static double getDropChance() { return dropChance; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPassable() {
        return true;
    }
}
