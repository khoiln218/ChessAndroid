package com.ttnt.chinesechess.ai;

public final class Negamax {

    // ===== Negamax: duyet het cay, khong cat tia =====
    //   negamax(n) = evaluate(n)                       neu n la nut la
    //   negamax(n) = max tren cac con c: -negamax(c)   neu khong
    //   Mot ham phuc vu ca hai ben: max(a, b) = -min(-a, -b), gia tri luon theo ben dang di
    // Dung thuat toan chung GameSearch (lop GameSearch), truyen ham cat tia luon = false.
    private Negamax() {
    }

    // cut = false: khong bao gio cat, duyet du b^d nut
    public static final GameSearch.Cutoff CUTOFF = (alpha, beta) -> false;

    public static <M> GameSearch.Problem<M> problem(GameState<M> state, int depth) {
        return new GameSearch.Problem<>(state, depth, CUTOFF);
    }
}
