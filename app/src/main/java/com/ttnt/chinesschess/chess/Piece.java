package com.ttnt.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

public abstract class Piece {

    Point CurrMove;
    Board board;
    String name;
    public boolean RED;
    ArrayList<State> allPossibleMove;

    public Piece(Board board, Point currMove) {
        this.board = board;
        this.CurrMove = currMove;
        byte val = board.cell[currMove.x][currMove.y];
        switch (val) {
            case 8:
            case 15:
                name = "KING";
                break;
            case 9:
            case 16:
                name = "BISHOP";
                break;
            case 10:
            case 17:
                name = "ELEPHANT";
                break;
            case 11:
            case 18:
                name = "KNIGHT";
                break;
            case 12:
            case 19:
                name = "ROOK";
                break;
            case 13:
            case 20:
                name = "CANNON";
                break;
            case 14:
            case 21:
                name = "PAWN";
                break;
        }
        this.RED = val > 14;
        allPossibleMove = null;
    }

    abstract ArrayList<State> findAllPossibleMoves();

    public boolean checkMove(int x, int y) {
        try {
            allPossibleMove = findAllPossibleMoves();
            int n = allPossibleMove.size();
            for (int i = 0; i < n; i++) {
                Point pos = allPossibleMove.get(i).curr;
                if (pos.x == x && pos.y == y) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR checkMove, Piece: " + e);
        }
        return false;
    }


    protected void doMove(int x, int y) {
        board.cell[x][y] = board.cell[CurrMove.x][CurrMove.y];
        board.cell[CurrMove.x][CurrMove.y] = 0;
    }

    protected void reMove(int x, int y, byte value) {
        board.cell[CurrMove.x][CurrMove.y] = board.cell[x][y];
        board.cell[x][y] = value;
    }
}