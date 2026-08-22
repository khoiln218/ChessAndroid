package com.ttnt.chinesschess.chess;

import android.graphics.Point;

public class State {

    public Point prev, curr;
    public byte value1, value2;

    public State(Point p, Point c, byte val1, byte val2) {
        this(p, c.x, c.y, val1, val2);
    }

    /**
     * The target as plain coordinates. Move generation has them as numbers already, and this is
     * on the hottest path in the app - a point built only to be copied is one object per move.
     */
    public State(Point p, int x, int y, byte val1, byte val2) {
        this.prev = new Point(p);
        this.curr = new Point(x, y);
        this.value1 = val1;
        this.value2 = val2;
    }
}
