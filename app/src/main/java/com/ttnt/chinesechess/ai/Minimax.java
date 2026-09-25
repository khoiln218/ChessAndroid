package com.ttnt.chinesechess.ai;

public final class Minimax {

    // ===== Minimax: hai ham MAX-VALUE / MIN-VALUE, duyet het cay, khong cat tia =====
    //   MAX: ben dang di o goc, MIN: doi thu. Moi gia tri deu tinh theo goc nhin MAX.
    //   max-value(n) = evaluate(n)                        neu n la nut la (MAX dang di)
    //                = max tren cac con c: min-value(c)   neu khong
    //   min-value(n) = -evaluate(n)                       neu n la nut la (MIN dang di, doi dau)
    //                = min tren cac con c: max-value(c)   neu khong
    // Negamax gop hai ham lam mot nho max(a, b) = -min(-a, -b); ket qua va so nut giong het Minimax.
    // Dung lai Problem/Node cua GameSearch, chi thay thuat toan search().
    private Minimax() {
    }

    // Duyet het cay nhu Negamax: khong bao gio cat
    public static <M> GameSearch.Problem<M> problem(GameState<M> state, int depth) {
        return new GameSearch.Problem<>(state, depth, Negamax.CUTOFF);
    }

    public static <M> GameSearch.Node<M> search(GameSearch.Problem<M> problem) {
        problem.visited = 0;
        GameSearch.Node<M> root = new GameSearch.Node<>(null, 0);
        maxValue(problem, root);
        return root;
    }

    // Nut MAX: chon con co gia tri lon nhat
    private static <M> int maxValue(GameSearch.Problem<M> problem, GameSearch.Node<M> node) {
        problem.visited++;
        if (problem.isLeaf(node)) {
            node.value = problem.eval();        // MAX dang di: dung goc nhin
            return node.value;
        }

        GameState<M> state = problem.initial;
        int best = -GameSearch.INF;
        for (M a : problem.actions()) {
            GameSearch.Node<M> child = new GameSearch.Node<>(a, node.depth + 1);
            state.play(a);
            int value = minValue(problem, child);
            state.undo(a);

            if (value > best) {                 // cung gia tri: giu nuoc gap truoc
                best = value;
                node.best = child;
            }
        }
        node.value = best;
        return best;
    }

    // Nut MIN: chon con co gia tri nho nhat
    private static <M> int minValue(GameSearch.Problem<M> problem, GameSearch.Node<M> node) {
        problem.visited++;
        if (problem.isLeaf(node)) {
            node.value = -problem.eval();       // MIN dang di: doi dau ve goc nhin MAX
            return node.value;
        }

        GameState<M> state = problem.initial;
        int best = GameSearch.INF;
        for (M a : problem.actions()) {
            GameSearch.Node<M> child = new GameSearch.Node<>(a, node.depth + 1);
            state.play(a);
            int value = maxValue(problem, child);
            state.undo(a);

            if (value < best) {
                best = value;
                node.best = child;
            }
        }
        node.value = best;
        return best;
    }
}
