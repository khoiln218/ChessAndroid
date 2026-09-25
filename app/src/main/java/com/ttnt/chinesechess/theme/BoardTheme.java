package com.ttnt.chinesechess.theme;

import android.content.Context;

import com.ttnt.chinesechess.R;

/**
 * A set the game can be played with: what {@link Graphics} paints the board with, and what
 * {@link PieceArt} draws the pieces in. One per element of the ngu hanh, in the traditional
 * Kim - Moc - Thuy - Hoa - Tho order, each cut from the material that element stands for:
 * metal, wood, ice, fire and stone.
 *
 * <p>A board carries four colours: two for the wood gradient, one for every line drawn on it,
 * and one for the river characters. A piece carries three per side - the rim, the face inside it
 * and the glyph - which is why the pieces are drawn rather than tinted: a real material changes
 * each of those to a different colour, not all three along one ramp.
 *
 * <p>A theme has to answer two questions at once: is the board light or dark, and does each side
 * of the pieces still read against it. That is why Hoa and Kim, whose boards are dark, both take
 * pale pieces, while the three light boards take darker ones.
 */
public enum BoardTheme {

    /** Kim - gunmetal board; chrome against copper. */
    KIM(R.string.theme_kim,
            0xFF5C646C, 0xFF262B30, 0xFFC9D4DC, 0xFFB8C4CC,
            0xFF96A3AF, 0xFFEDF3F8, 0xFF2E3841,
            0xFFB06A22, 0xFFF5D6A2, 0xFF6B3707),
    /** Moc - turned wood on a wooden board, in the vector set's own colours. */
    MOC(R.string.theme_moc,
            0xFFE8C99A, 0xFFCBA071, 0xFF59371B, 0xFF6B4526,
            0xFF8E5C32, 0xFFD8AD7D, 0xFF241206,
            0xFFAB3F2E, 0xFFD8AD7D, 0xFFB3241F),
    /** Thuy - water gone to ice; glacier blue against frosted rose. */
    THUY(R.string.theme_thuy,
            0xFFE8F4FB, 0xFFAFD2E8, 0xFF1F5273, 0xFF2B6488,
            0xFF1E5578, 0xFF8CC0DE, 0xFF0C2E45,
            0xFF8E2F48, 0xFFE9AFC0, 0xFF5E162C),
    /** Hoa - embers; ash against molten gold. */
    HOA(R.string.theme_hoa,
            0xFF6E2612, 0xFF2A0D06, 0xFFF0A868, 0xFFEFBE94,
            0xFF8A8078, 0xFFE9E2D9, 0xFF332C27,
            0xFFD2681E, 0xFFFFE0A4, 0xFF7E2A05),
    /** Tho - pale stone; slate against terracotta. */
    THO(R.string.theme_tho,
            0xFFF2F1EB, 0xFFCBCEC7, 0xFF4A524E, 0xFF5C6460,
            0xFF3C4F5C, 0xFFA5BAC6, 0xFF1B2831,
            0xFF96501F, 0xFFE5B683, 0xFF5E2A0C);

    /** The set the app has always had, and what it falls back to. */
    public final static BoardTheme DEFAULT = MOC;

    public final int labelRes;
    /** Wood at the top left corner of the board, and at the bottom right one. */
    final int woodLight;
    final int woodDark;
    /** Every line of the board: frame, grid, palaces and position marks. */
    final int line;
    final int river;
    /** The black side: the ring around the piece, the face inside it, and the character on it. */
    final int blackRim;
    final int blackFace;
    final int blackGlyph;
    final int redRim;
    final int redFace;
    final int redGlyph;

    BoardTheme(int labelRes, int woodLight, int woodDark, int line, int river,
               int blackRim, int blackFace, int blackGlyph,
               int redRim, int redFace, int redGlyph) {
        this.labelRes = labelRes;
        this.woodLight = woodLight;
        this.woodDark = woodDark;
        this.line = line;
        this.river = river;
        this.blackRim = blackRim;
        this.blackFace = blackFace;
        this.blackGlyph = blackGlyph;
        this.redRim = redRim;
        this.redFace = redFace;
        this.redGlyph = redGlyph;
    }

    /** The ring colour of one side's pieces, for anything outside the board that shows one. */
    public int rimOf(boolean red) {
        return red ? redRim : blackRim;
    }

    /** The five names, in declaration order, ready for a dialog's item list. */
    public static CharSequence[] labels(Context context) {
        BoardTheme[] all = values();
        CharSequence[] labels = new CharSequence[all.length];
        for (int i = 0; i < all.length; i++) {
            labels[i] = context.getString(all[i].labelRes);
        }
        return labels;
    }
}
