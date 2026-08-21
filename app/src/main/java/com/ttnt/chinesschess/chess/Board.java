package com.ttnt.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

public final class Board {

    public final static int ROW = 10;
    public final static int COL = 9;

    public final static byte[][] cellStartup = {
            {19, 18, 17, 16, 15, 16, 17, 18, 19},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 20, 0, 0, 0, 0, 0, 20, 0},
            {21, 0, 21, 0, 21, 0, 21, 0, 21},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {14, 0, 14, 0, 14, 0, 14, 0, 14},
            {0, 13, 0, 0, 0, 0, 0, 13, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {12, 11, 10, 9, 8, 9, 10, 11, 12}};

    public byte[][] cell;
    public Point currMove;
    public Point prevMove;
    public boolean select;
    public boolean move;
    public boolean RED;
    public ArrayList<State> listUndo;

    public Board(boolean side) {
        listUndo = new ArrayList<>();
        setBoard(cellStartup);
        currMove = new Point(-1, -1);
        prevMove = new Point(-1, -1);
        this.select = false;
        this.move = false;
        this.RED = side;
    }

    public Board(Board b) {
        listUndo = new ArrayList<>();
        setBoard(b.cell);
        this.currMove = new Point(b.currMove);
        this.prevMove = new Point(b.prevMove);
        this.select = b.select;
        this.move = b.move;
        this.RED = b.RED;
    }

    public void setBoard(byte[][] board) {
        cell = new byte[ROW][COL];
        for (int i = 0; i < ROW; i++) {
            System.arraycopy(board[i], 0, cell[i], 0, COL);
        }
    }

    public byte getValue(int x, int y) {
        if (x < 0 || x >= ROW || y < 0 || y >= COL)
            return 0;
        return cell[x][y];
    }

    public boolean isCheckSelect(int x, int y) {
        byte value = getValue(x, y);
        if (RED) return value > 14;
        else return value >= 8 && value <= 14;
    }

    public boolean isCheckMove(int x, int y) {
        if (!select) return false;
        Piece piece = getPiece(prevMove.x, prevMove.y);
        return piece != null && piece.checkMove(x, y);
    }

    public Piece getPiece(int x, int y) {
        Piece piece = null;
        byte value = getValue(x, y);
        piece = switch (value) {
            case 8, 15 -> new CKing(this, new Point(x, y));
            case 9, 16 -> new CBishop(this, new Point(x, y));
            case 10, 17 -> new CElephant(this, new Point(x, y));
            case 11, 18 -> new CKnight(this, new Point(x, y));
            case 12, 19 -> new CRook(this, new Point(x, y));
            case 13, 20 -> new CCannon(this, new Point(x, y));
            case 14, 21 -> new CPawn(this, new Point(x, y));
            default -> piece;
        };
        return piece;
    }

    public ArrayList<Point> findPieces(boolean _RED) {
        ArrayList<Point> allPiece = new ArrayList<>();
        allPiece.add(0, new Point(-1, -1));
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                byte val = cell[i][j];
                if (val >= 8 && val <= 21) {
                    if ((_RED && val == 15) || (!_RED && val == 8)) {
                        allPiece.remove(0);
                        allPiece.add(0, new Point(i, j));
                    }
                    if ((_RED && val <= 14) || (!_RED && val > 14)) {
                        allPiece.add(new Point(i, j));
                    }
                }
            }
        }
        return allPiece;
    }

    public ArrayList<State> allMove(Point pos) {
        ArrayList<State> arrMoves = new ArrayList<>();
        byte val = getValue(pos.x, pos.y);
        switch (val) {
            case 8:
            case 15:
                CKing king = new CKing(this, pos);
                arrMoves.addAll(king.findAllPossibleMoves());
                break;
            case 9:
            case 16:
                CBishop bishop = new CBishop(this, pos);
                arrMoves.addAll(bishop.findAllPossibleMoves());
                break;
            case 10:
            case 17:
                CElephant elephant = new CElephant(this, pos);
                arrMoves.addAll(elephant.findAllPossibleMoves());
                break;
            case 11:
            case 18:
                CKnight knight = new CKnight(this, pos);
                arrMoves.addAll(knight.findAllPossibleMoves());
                break;
            case 12:
            case 19:
                CRook rook = new CRook(this, pos);
                arrMoves.addAll(rook.findAllPossibleMoves());
                break;
            case 13:
            case 20:
                CCannon cannon = new CCannon(this, pos);
                arrMoves.addAll(cannon.findAllPossibleMoves());
                break;
            case 14:
            case 21:
                CPawn pawn = new CPawn(this, pos);
                arrMoves.addAll(pawn.findAllPossibleMoves());
                break;
        }
        return arrMoves;
    }

    public ArrayList<State> allMoves(boolean _RED) {
        ArrayList<Point> allPiece = findPieces(_RED);
        ArrayList<State> arrMoves = new ArrayList<>();
        for (int i = 1; i < allPiece.size(); i++) {
            arrMoves.addAll(allMove(allPiece.get(i)));
        }
        return arrMoves;
    }

    public boolean isGameOver(boolean _RED) {
        ArrayList<Point> allPiece = findPieces(_RED);
        for (int i = 1; i < allPiece.size(); i++) {
            Point pos = allPiece.get(i);
            ArrayList<State> arrMoves = null;
            byte val = cell[pos.x][pos.y];
            arrMoves = switch (val) {
                case 8, 15 -> {
                    CKing king = new CKing(this, pos);
                    yield king.findAllPossibleMoves();
                }
                case 9, 16 -> {
                    CBishop bishop = new CBishop(this, pos);
                    yield bishop.findAllPossibleMoves();
                }
                case 10, 17 -> {
                    CElephant elephant = new CElephant(this, pos);
                    yield elephant.findAllPossibleMoves();
                }
                case 11, 18 -> {
                    CKnight knight = new CKnight(this, pos);
                    yield knight.findAllPossibleMoves();
                }
                case 12, 19 -> {
                    CRook rook = new CRook(this, pos);
                    yield rook.findAllPossibleMoves();
                }
                case 13, 20 -> {
                    CCannon cannon = new CCannon(this, pos);
                    yield cannon.findAllPossibleMoves();
                }
                case 14, 21 -> {
                    CPawn pawn = new CPawn(this, pos);
                    yield pawn.findAllPossibleMoves();
                }
                default -> arrMoves;
            };
            if (arrMoves != null && !arrMoves.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void select(int x, int y) {
        prevMove = new Point(x, y);
        select = true;
        move = false;
    }

    public void moveTo(int x, int y) {
        currMove = new Point(x, y);
        byte val1 = getValue(prevMove.x, prevMove.y);
        byte val2 = getValue(currMove.x, currMove.y);
        listUndo.add(new State(prevMove, currMove, val1, val2));
        cell[x][y] = val1;
        cell[prevMove.x][prevMove.y] = 0;
        select = false;
        move = true;
        switchPlayer();
    }

    public void switchPlayer() {
        RED = !RED;
    }
}
