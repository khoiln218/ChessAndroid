package com.quanpk.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.Random;

public class _AI {

    Board board;
    Board clone;
    private int maxDepth = 2 * 2;
    private State bestMove;

    public _AI(Board b) {
        this.board = b;
    }

    public _AI(Board b, int depth) {
        this.board = b;
        maxDepth = depth * 2;
    }

    int bonous(boolean side) {

        int materialNumber[][] = {{5, 2, 2, 2, 2, 2, 1},
                {5, 2, 2, 2, 2, 2, 1}};
        int s, bn[][] = {{-2, -3, -3, -4, -4, -5, 0},
                {-2, -3, -3, -4, -4, -5, 0}};

        for (int i = 0; i < 2; i++) {
            if (materialNumber[i][1] < 2) {
                bn[1 - i][5] += 4;
                bn[1 - i][3] += 2;
                bn[1 - i][0] += 1;
            }

            if (materialNumber[i][2] < 2) {
                bn[1 - i][5] += 2;
                bn[1 - i][4] += 2;
                bn[1 - i][0] += 1;
            }
        }

        if (clone.cell[0][0] == 19 && clone.cell[0][1] == 18) {
            bn[0][6] -= 10;
        }
        if (clone.cell[0][8] == 19 && clone.cell[0][7] == 18) {
            bn[0][6] -= 10;
        }
        if (clone.cell[9][0] == 13 && clone.cell[9][1] == 12) {
            bn[1][6] -= 10;
        }
        if (clone.cell[9][8] == 13 && clone.cell[9][7] == 12) {
            bn[1][6] -= 10;
        }

        int d1, d2;
        if (side) {
            d1 = 0;
            d2 = 1;
        } else {
            d1 = 1;
            d2 = 0;
        }

        s = bn[d1][6] - bn[d2][6];

        for (int i = 0; i < 6; i++) {
            s += materialNumber[d1][i] * bn[d1][i]
                    - materialNumber[d2][i] * bn[d2][i];
        }
        return s;
    }

    int eval(boolean side) {
        int s = 0;
        for (int x = 0; x <= 9; x++) {
            for (int y = 0; y <= 8; y++) {
                byte value = clone.getValue(x, y);
                switch (value) {
                    case 8:
                        s -= CKing.getPositionValue(new Point(x, y), false);
                        break;
                    case 15:
                        s += CKing.getPositionValue(new Point(x, y), true);
                        break;
                    case 9:
                        s -= CBishop.getPositionValue(new Point(x, y), false);
                        break;
                    case 16:
                        s += CBishop.getPositionValue(new Point(x, y), true);
                        break;
                    case 10:
                        s -= CElephant.getPositionValue(new Point(x, y), false);
                        break;
                    case 17:
                        s += CElephant.getPositionValue(new Point(x, y), true);
                        break;
                    case 11:
                        s -= CKnight.getPositionValue(new Point(x, y), false);
                        break;
                    case 18:
                        s += CKnight.getPositionValue(new Point(x, y), true);
                        break;
                    case 12:
                        s -= CRook.getPositionValue(new Point(x, y), false);
                        break;
                    case 19:
                        s += CRook.getPositionValue(new Point(x, y), true);
                        break;
                    case 13:
                        s -= CCannon.getPositionValue(new Point(x, y), false);
                        break;
                    case 20:
                        s += CCannon.getPositionValue(new Point(x, y), true);
                        break;
                    case 14:
                        s -= CPawn.getPositionValue(new Point(x, y), false);
                        break;
                    case 21:
                        s += CPawn.getPositionValue(new Point(x, y), true);
                        break;
                }
            }
        }
        if (!side) {
            s = -s;
        }
        return s + bonous(side);
    }

    void doMove(State state) {
        clone.cell[state.curr.x][state.curr.y] = state.value1;
        clone.cell[state.prev.x][state.prev.y] = 0;
    }

    void reMove(State state) {
        clone.cell[state.curr.x][state.curr.y] = state.value2;
        clone.cell[state.prev.x][state.prev.y] = state.value1;
    }

    public int alphaBeta(int depth, boolean side, int alpha, int beta) {
        if (depth == 0) {
            return eval(side);
        }

        int best = -100000;
        ArrayList<State> bestMove = new ArrayList<>();
        ArrayList<State> arrMoves = clone.allMoves(!side);

        if (arrMoves.isEmpty()) {
            return -100000 - depth;
        }

        int i = 0;
        while (i < arrMoves.size() && best < beta) {
            State m = arrMoves.get(i);

            if (best > alpha) {
                alpha = best;
            }

            doMove(m);
            side = !side;

            int value = -alphaBeta(depth - 1, side, -beta, -alpha);

            reMove(m);
            side = !side;

            if (value > best) {
                best = value;
                bestMove.clear();
                bestMove.add(m);
            } else if (value == best) {
                bestMove.add(m);
            }
            i++;
        }
        if (depth == maxDepth) {
//            int index = new Random().nextInt(bestMove.size());
            this.bestMove = bestMove.get(0);
        }
        return best;
    }

    public State generateMove(boolean RED) {
        int alpha = -100000;
        int beta = 100000;

        clone = new Board(board);

        alphaBeta(maxDepth, RED, -beta, -alpha);

        return bestMove;
    }
}
