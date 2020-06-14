package com.quanpk.chinesschess.chess;

import android.graphics.Point;

public class State {

    public Point prev, curr;
    public byte value1, value2;

    public State(Point p, Point c, byte val1, byte val2) {
        this.prev = new Point(p);
        this.curr = new Point(c);
        this.value1 = val1;
        this.value2 = val2;
    }
}
