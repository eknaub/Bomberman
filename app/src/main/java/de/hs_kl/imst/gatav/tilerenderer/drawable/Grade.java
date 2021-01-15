package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Color;
import java.io.InputStream;

public class Grade extends TileGraphics {
    private double removeTime = 1; // in sec

    public Grade(int x, int y, InputStream is) {
        super(x, y, is);

        tilePaint.setColor(Color.parseColor("#149942"));
    }

    public double getRemoveTime() { return removeTime; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPassable() {
        return true;
    }
}
