package com.ttnt.chinesechess.ai;

import java.util.List;

/**
 * A two-player, zero-sum, perfect-information game, seen from wherever it stands now - the one
 * thing the search algorithms in this package know about the game they play. Nothing here is
 * specific to xiangqi: the same algorithms play any game that can answer these five questions.
 *
 * <p>The state is mutable and searched in place: the algorithm plays a move, searches below it,
 * and takes it back, so {@link #undo} must restore exactly what {@link #play} changed.
 *
 * @param <M> how the game writes down one move
 */
public interface GameState<M> {

    /**
     * The score of a win on the spot. A game that scores a lost position {@code -(WIN - ply)},
     * with {@code ply} the moves played since the root, makes a search prefer the shortest win;
     * any score within 1000 of it is then a win at some distance.
     */
    int WIN = 900_000;

    /** Every legal move for the side to move. The order is the order the search tries them in. */
    List<M> moves();

    /** Makes {@code move} and hands the turn to the other side. */
    void play(M move);

    /** Takes back {@code move}, which must be the last move played. */
    void undo(M move);

    /** Whether the game is over here - the side to move has won, lost or drawn already. */
    boolean isTerminal();

    /**
     * How good the position is for the side to move: positive is good for it, negative good for
     * its opponent. Negamax depends on that point of view - one side's score is the other's
     * negated - so a game must score from the mover's side, never from a fixed colour.
     */
    int evaluate();
}
