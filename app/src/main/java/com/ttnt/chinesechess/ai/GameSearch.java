package com.ttnt.chinesechess.ai;

import java.util.List;

public final class GameSearch {

    // ===== Thuat toan tim kiem doi khang dung chung (dang Negamax) =====
    // Negamax (Negamax.java) va Alpha-Beta (AlphaBeta.java) deu goi GameSearch.search(),
    // chi khac nhau o ham cat tia cut(alpha, beta) duoc truyen vao Problem:
    //   Negamax   : cut = false            (duyet het cay)
    //   Alpha-Beta: cut = alpha >= beta    (bo cac nuoc con lai khi doi thu khong cho di vao nut nay)
    // Minimax (Minimax.java) dung lai Problem va Node nhung co search() rieng voi hai ham MAX/MIN.

    /** Duong vo cung: lon hon moi gia tri ham danh gia tra ve. */
    public static final int INF = 1_000_000_000;

    private GameSearch() {
    }

    // Ham cat tia, nhan alpha va beta hien tai cua nut
    public interface Cutoff { boolean cut(int alpha, int beta); }

    public static class Problem<M> {
        protected final GameState<M> initial;   // trang thai goc (ben can di)
        protected final int depth;              // do sau toi da (so nuoc nhin truoc)
        protected final Cutoff cutoff;          // ham cat tia cua tung thuat toan
        long visited;                           // so nut da duyet, goc tinh ca vao

        public Problem(GameState<M> initial, int depth, Cutoff cutoff) {
            this.initial = initial;
            this.depth = depth;
            this.cutoff = cutoff;
        }

        public long visited() { return visited; }

        // Nut la: het do sau hoac het van
        boolean isLeaf(Node<M> node) {
            return node.depth >= depth || initial.isTerminal();
        }

        List<M> actions() { return initial.moves(); }

        // Ham danh gia, theo goc nhin ben dang di tai nut
        int eval() { return initial.evaluate(); }
    }

    // ===== Node: (ACTION, depth, value, best) =====
    public static class Node<M> {
        public final M action;          // nuoc di tu nut cha den nut nay (goc: null)
        public final int depth;         // so nuoc tu goc
        public int value;               // Negamax: theo goc nhin ben dang di tai nut; Minimax: theo goc nhin MAX
        public Node<M> best;            // nut con tot nhat (null o nut la)

        public Node(M action, int depth) {
            this.action = action; this.depth = depth;
        }

        // Nuoc tot nhat tai nut (null neu khong con nuoc di)
        public M bestAction() { return best == null ? null : best.action; }
    }

    // Thuat toan chung: Negamax voi cua so [alpha, beta] va ham cat tia cua Problem
    public static <M> Node<M> search(Problem<M> problem) {
        problem.visited = 0;
        Node<M> root = new Node<>(null, 0);
        negamax(problem, root, -INF, INF);
        return root;
    }

    // negamax(n) = max tren cac con c cua n: -negamax(c)
    // Cua so cua con la [-beta, -alpha]: dao dau va doi cho vi con nhin tu phia doi thu
    private static <M> int negamax(Problem<M> problem, Node<M> node, int alpha, int beta) {
        problem.visited++;
        GameState<M> state = problem.initial;

        if (problem.isLeaf(node)) {
            node.value = problem.eval();
            return node.value;
        }

        int best = -INF;
        for (M a : problem.actions()) {
            Node<M> child = new Node<>(a, node.depth + 1);
            state.play(a);
            int value = -negamax(problem, child, -beta, -alpha);
            state.undo(a);

            if (value > best) {                 // cung gia tri: giu nuoc gap truoc
                best = value;
                node.best = child;
            }
            if (best > alpha) alpha = best;
            if (problem.cutoff.cut(alpha, beta)) break;   // cat tia: bo cac nuoc con lai
        }
        node.value = best;
        return best;
    }
}
