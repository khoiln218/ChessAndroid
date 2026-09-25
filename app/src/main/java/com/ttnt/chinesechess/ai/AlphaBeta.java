package com.ttnt.chinesechess.ai;

public final class AlphaBeta {

    // ===== Alpha-Beta (dang Negamax): cat tia khi alpha >= beta =====
    //   alpha: gia tri ben dang di chac chan dat duoc (tu cac nuoc da xet)
    //   beta : gia tri doi thu chac chan dat duoc o nhanh khac cua cay
    //   Khi alpha >= beta doi thu se khong bao gio cho di vao nut nay -> bo cac nuoc con lai.
    //   Ket qua tai goc giong het Negamax, chi duyet it nut hon
    //   (thu tu nuoc tot: ~b^(d/2) nut; thu tu xau: van b^d).
    // Dung thuat toan chung GameSearch (lop GameSearch), truyen ham cat tia alpha >= beta.
    private AlphaBeta() {
    }

    // cut = alpha >= beta
    public static final GameSearch.Cutoff CUTOFF = (alpha, beta) -> alpha >= beta;

    public static <M> GameSearch.Problem<M> problem(GameState<M> state, int depth) {
        return new GameSearch.Problem<>(state, depth, CUTOFF);
    }
}
