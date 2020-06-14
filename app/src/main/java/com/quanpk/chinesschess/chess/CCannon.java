package com.quanpk.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

public class CCannon extends Piece {

    static int[][] CannonTable = {
            {50, 50, 50, 50, 50, 50, 50, 50, 50}, /* CANNON */
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 51, 51, 51, 51, 51, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 53, 53, 55, 53, 53, 51, 50},
            {50, 50, 50, 50, 50, 50, 50, 50, 50},
            {50, 50, 50, 50, 50, 50, 50, 50, 50}
    };

    public CCannon(Board board, Point currMove) {
        super(board, currMove);
    }

    @Override
    public ArrayList<State> findAllPossibleMoves() {
        allPossibleMove = new ArrayList<State>();
        int x = CurrMove.x;
        int y = CurrMove.y;
        for (int i = y + 1; i <= 8; i++) {
            if (getCannonMove1(x, i)) {
                break;
            }
        }
        for (int i = y - 1; i >= 0; i--) {
            if (getCannonMove1(x, i)) {
                break;
            }
        }
        for (int i = x + 1; i <= 9; i++) {
            if (getCannonMove1(i, y)) {
                break;
            }
        }
        for (int i = x - 1; i >= 0; i--) {
            if (getCannonMove1(i, y)) {
                break;
            }
        }
        return allPossibleMove;
    }

    boolean getCannonMove1(int x, int y) {
        byte val1 = board.cell[CurrMove.x][CurrMove.y];
        byte val2 = board.cell[x][y];
        if (val2 == 0) {
            getCannonMove3(x, y, val1, val2);
        } else {
            if (CurrMove.x == x) {
                if (y > CurrMove.y) {
                    for (y = y + 1; y <= 8; y++) {
                        if (getCannonMove2(x, y)) {
                            break;
                        }
                    }
                } else {
                    for (y = y - 1; y >= 0; y--) {
                        if (getCannonMove2(x, y)) {
                            break;
                        }
                    }
                }
            } else {
                if (x > CurrMove.x) {
                    for (x = x + 1; x <= 9; x++) {
                        if (getCannonMove2(x, y)) {
                            break;
                        }
                    }
                } else {
                    for (x = x - 1; x >= 0; x--) {
                        if (getCannonMove2(x, y)) {
                            break;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    boolean getCannonMove2(int x, int y) {
        byte val1 = board.cell[CurrMove.x][CurrMove.y];
        byte val2 = board.cell[x][y];
        if (val2 != 0) {
            if ((RED && val2 <= 14) || (!RED && val2 > 14)) {
                getCannonMove3(x, y, val1, val2);
            }
            return true;
        }
        return false;
    }

    void getCannonMove3(int x, int y, byte val1, byte val2) {
        doMove(x, y);
        ArrayList<Point> arr = board.findPieces(RED);
        if (!checkProject(board, arr)) {
            allPossibleMove.add(new State(CurrMove, new Point(x, y), val1, val2));
        }
        reMove(x, y, val2);
    }

    @Override
    boolean checkProject(Point King) {
        if (CurrMove.x == King.x || CurrMove.y == King.y) {
            int count = 0;
            if (CurrMove.x == King.x) {
                for (int i = Math.min(CurrMove.y, King.y) + 1; i < Math.max(CurrMove.y, King.y); i++) {
                    if (board.cell[CurrMove.x][i] != 0) {
                        count++;
                    }
                }
            } else {
                for (int i = Math.min(CurrMove.x, King.x) + 1; i < Math.max(CurrMove.x, King.x); i++) {
                    if (board.cell[i][CurrMove.y] != 0) {
                        count++;
                    }
                }
            }
            if (count == 1) {
                return true;
            }
        }
        return false;
    }

    public static int getPositionValue(Point pos, boolean RED) {
        if (RED) {
            return CannonTable[9 - pos.x][8 - pos.y];
        }
        return CannonTable[pos.x][pos.y];
    }
}
