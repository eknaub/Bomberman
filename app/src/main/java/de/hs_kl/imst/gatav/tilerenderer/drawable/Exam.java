package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.InputStream;

public class Exam extends TileGraphics {
    private Paint floorPaint = new Paint();
    private double detonationTime = 3.0; //in Sec

    public Exam(int x, int y, InputStream is) {
        super(x, y, is);

        tilePaint.setColor(Color.parseColor("#149942"));
    }

    public double getDetonationTime() { return detonationTime; }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPassable() {
        return true;
    }
}
