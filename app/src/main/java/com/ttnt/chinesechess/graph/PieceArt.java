package com.ttnt.chinesechess.graph;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.core.graphics.PathParser;

import com.ttnt.chinesechess.R;

/**
 * Draws the pieces from the vector set in res/values/piece_paths.xml.
 *
 * <p>The artwork is a 680x680 design: a rim disc under a lighter face, two hairlines cut into
 * the face, the character on top, and a highlight across the top left. All of that is redrawn
 * here in the theme's colours rather than loaded as a picture, so one set of outlines serves all
 * five materials. Only the character itself comes from the file - as an outline, not as text, so
 * it renders the same whether or not the device carries a CJK font.
 *
 * <p>A piece is a disc with thickness, seen from almost overhead: the same circle is drawn twice,
 * once {@link #DEPTH} lower in a dark gradient for the side of the cylinder, then the lit top
 * face over it, leaving the wall showing as a crescent along the bottom. The top face stays
 * centred on its intersection - that is the square the piece occupies - and the wall and the
 * contact shadow hang below it, into the margin the design leaves around the disc.
 *
 * <p>Each piece is rendered once per size and theme into its own bitmap, and from then on a
 * repaint is a single blit.
 */
public final class PieceArt {

    /** The artwork's design space, and where it puts the disc inside it. */
    private final static float DESIGN = 680f;
    private final static float CENTRE = 340f;
    private final static float DISC_R = 250f;
    private final static float FACE_R = 210f;
    private final static float INNER_R = 186f;
    /**
     * How thick a piece stands, in design units: how far the wall shows below the top face. The
     * design leaves 90 units under the disc for it, so much past 76 and the wall meets the bottom
     * of the bitmap and is cut off square instead of curving away.
     */
    private final static float DEPTH = 62f;
    /** The transform the artwork wraps its glyph in; the y flip is because outlines run bottom-up. */
    private final static float GLYPH_SCALE = 0.3f;
    private final static float GLYPH_X = 190f;
    private final static float GLYPH_Y = 455f;

    private PieceArt() {
    }

    /**
     * The fourteen pieces in the order the board numbers them: the black general down to its
     * pawn, then the red ones. {@code size} is the width of the square each is drawn in, of
     * which {@link Graphics#PIECE_ART} is the disc and the rest is room for its shadow.
     */
    public static Bitmap[] renderAll(Resources res, BoardTheme theme, int size) {
        String[] paths = res.getStringArray(R.array.piece_paths);
        Bitmap[] out = new Bitmap[paths.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = render(paths[i], size, theme, i >= out.length / 2);
        }
        return out;
    }

    /** The general of one side on its own, for the panels that put a face to each player. */
    public static Bitmap general(Resources res, BoardTheme theme, boolean red, int size) {
        String[] paths = res.getStringArray(R.array.piece_paths);
        return render(paths[red ? paths.length / 2 : 0], size, theme, red);
    }

    /**
     * Puts one side's general on a view, inside a ring struck in that side's own colour - the
     * seat marker the lobby and the two side panels both use.
     */
    public static void dressAvatar(android.widget.ImageView view, BoardTheme theme, boolean red) {
        Resources res = view.getResources();
        view.setImageBitmap(general(res, theme, red,
                res.getDimensionPixelSize(R.dimen.avatar_piece)));
        android.graphics.drawable.GradientDrawable ring =
                new android.graphics.drawable.GradientDrawable();
        ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        ring.setColor(androidx.core.content.res.ResourcesCompat.getColor(
                res, R.color.avatarFill, null));
        ring.setStroke(Math.round(res.getDisplayMetrics().density * 2), theme.rimOf(red));
        view.setBackground(ring);
    }

    private static Bitmap render(String glyphPath, int size, BoardTheme theme, boolean red) {
        int rim = theme.rimOf(red);
        int face = red ? theme.redFace : theme.blackFace;
        int ink = red ? theme.redGlyph : theme.blackGlyph;

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(size / DESIGN, size / DESIGN);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Where the piece meets the board: flattened, because the light is high and to the left.
        paint.setColor(0x4A000000);
        paint.setMaskFilter(new BlurMaskFilter(26f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawOval(CENTRE - DISC_R * 1.02f, CENTRE + DEPTH + 6f - DISC_R * 0.32f,
                CENTRE + DISC_R * 1.02f, CENTRE + DEPTH + 6f + DISC_R * 0.32f, paint);
        paint.setMaskFilter(null);
        // setColor just set the paint's alpha, and a shader is modulated by it - reset it, or
        // everything below would come out translucent.
        paint.setAlpha(255);

        // The side of the cylinder: the same disc, sunk by DEPTH, in shadow all the way down.
        paint.setShader(new LinearGradient(0, CENTRE, 0, CENTRE + DEPTH + DISC_R,
                darken(rim, 0.34f), darken(rim, 0.70f), Shader.TileMode.CLAMP));
        canvas.drawCircle(CENTRE, CENTRE + DEPTH, DISC_R, paint);

        paint.setShader(new LinearGradient(0, CENTRE - DISC_R, 0, CENTRE + DISC_R,
                lighten(rim, 0.20f), darken(rim, 0.20f), Shader.TileMode.CLAMP));
        canvas.drawCircle(CENTRE, CENTRE, DISC_R, paint);

        // Lit from up and to the left, as the artwork has it.
        paint.setShader(new RadialGradient(DESIGN * 0.42f, DESIGN * 0.36f, DESIGN * 0.72f,
                new int[]{lighten(face, 0.22f), face, darken(face, 0.18f)},
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(CENTRE, CENTRE, FACE_R, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(withAlpha(darken(rim, 0.15f), 128));
        canvas.drawCircle(CENTRE, CENTRE, FACE_R, paint);
        paint.setStrokeWidth(3.5f);
        paint.setColor(withAlpha(lighten(rim, 0.22f), 166));
        canvas.drawCircle(CENTRE, CENTRE, INNER_R, paint);
        paint.setStyle(Paint.Style.FILL);

        Path glyph = PathParser.createPathFromPathData(glyphPath);
        Matrix place = new Matrix();
        place.setScale(GLYPH_SCALE, -GLYPH_SCALE);
        place.postTranslate(GLYPH_X, GLYPH_Y);
        glyph.transform(place);
        paint.setColor(ink);
        canvas.drawPath(glyph, paint);

        paint.setColor(0x1FFFFFFF);
        canvas.drawOval(new RectF(286f - 118f, 248f - 66f, 286f + 118f, 248f + 66f), paint);

        // Catch the light on the top left edge, and let the far edge fall away, so the top face
        // reads as the rounded end of a cylinder rather than a flat sticker.
        RectF edge = new RectF(CENTRE - DISC_R, CENTRE - DISC_R, CENTRE + DISC_R, CENTRE + DISC_R);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(11f);
        paint.setColor(0x59FFFFFF);
        canvas.drawArc(edge, 163f, 118f, false, paint);
        paint.setStrokeWidth(13f);
        paint.setColor(0x3D000000);
        canvas.drawArc(edge, -12f, 116f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        return bitmap;
    }

    private static int lighten(int color, float amount) {
        return mix(color, Color.WHITE, amount);
    }

    private static int darken(int color, float amount) {
        return mix(color, Color.BLACK, amount);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /** {@code amount} of the way from {@code from} to {@code towards}, alpha left alone. */
    private static int mix(int from, int towards, float amount) {
        return Color.argb(Color.alpha(from),
                Math.round(Color.red(from) + (Color.red(towards) - Color.red(from)) * amount),
                Math.round(Color.green(from) + (Color.green(towards) - Color.green(from)) * amount),
                Math.round(Color.blue(from) + (Color.blue(towards) - Color.blue(from)) * amount));
    }
}
