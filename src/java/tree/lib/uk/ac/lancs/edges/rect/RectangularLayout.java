/* Copyright (c) 2016, Lancaster University All
 * rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution.
 *
 * * Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contributors: Steven Simpson <https://github.com/simpsonst> */

package uk.ac.lancs.edges.rect;

import java.awt.Point;
import java.io.Console;
import java.util.BitSet;

import uk.ac.lancs.edges.Layout;
import uk.ac.lancs.edges.Scribe;
import uk.ac.lancs.edges.Tracer;

/**
 * <p>
 * The step ids are lain out assuming that row 0 is the top, and that
 * column 0 is the left. Steps along the top edge to the right are
 * numbered 0, 2, 4, 6, etc. Their inverses are obtained by adding 1.
 * The total number of horizontal steps is <var>v</var> =
 * 2<var>w</var>(<var>h</var> + 1). The step from the top left downwards
 * is numbered <var>v</var>, and to its right is <var>v</var> + 2,
 * <var>v</var> + 4, <var>v</var> + 6, etc. Their inverses are also
 * obtained by adding 1.
 * 
 * @resume A layout based on a rectangular grid
 *
 * @author simpsons
 */
public final class RectangularLayout implements Layout<Point> {
    private final int width, height;
    private final int halfHorizontals, halfVerticals;
    private final int horizontals, verticals;
    private final int maxSteps;
    private final BitSet steps;

    private boolean isAvailable(int id) {
        return steps.get(id >> 1);
    }

    /**
     * Create a layout from a rectangular grid.
     * 
     * @param grid the source grid
     */
    public RectangularLayout(RectangularGrid grid) {
        this.width = grid.width();
        this.height = grid.height();

        halfHorizontals = (height + 1) * width;
        horizontals = 2 * halfHorizontals;
        halfVerticals = (width + 1) * height;
        verticals = 2 * halfVerticals;
        maxSteps = horizontals + verticals;
        steps = new BitSet(halfHorizontals + halfVerticals);

        for (int y = 0; y <= height; y++)
            for (int x = 0; x <= width; x++) {
                boolean here = grid.color(x, y) != 0;
                boolean left = grid.color(x - 1, y) != 0;
                boolean up = grid.color(x, y - 1) != 0;
                if (here != up) steps.set(width * y + x);
                if (here != left)
                    steps.set(halfHorizontals + (width + 1) * y + x);
            }
    }

    private void validate(int id) {
        if (id < 0)
            throw new IllegalArgumentException("invalid step id: " + id);
        if (id >= maxSteps)
            throw new IllegalArgumentException("invalid step id: " + id
                + " in rectangle " + width + "x" + height);
    }

    @Override
    public int getAnyStep() {
        int halfId = steps.nextSetBit(0);
        if (halfId < 0) return -1;
        return (halfId << 1) + 1;
    }

    @Override
    public boolean areParallel(int id1, int id2) {
        validate(id1);
        validate(id2);
        if ((id1 < horizontals) != (id2 < horizontals)) return false;
        return ((id1 ^ id2) & 1) == 0;
    }

    @Override
    public boolean areAntiparallel(int id1, int id2) {
        validate(id1);
        validate(id2);
        if ((id1 < horizontals) != (id2 < horizontals)) return false;
        return ((id1 ^ id2) & 1) != 0;
    }

    @Override
    public int[] getOptions(int id) {
        /* Find the end position. The result will only contain
         * candidates that start from here. */
        final int x, y;
        {
            Point end = getEnd(id);
            x = end.x;
            y = end.y;
        }
        final int inv = invert(id);

        /* Prepare a result with upto 3 entries. */
        int[] result = { -1, -1, -1 };
        int idx = 0;

        final int right = 2 * (width * y + x);
        final int left = right - 1;
        final int down = horizontals + 2 * ((width + 1) * y + x);
        final int up = down - (width + 1) * 2 + 1;

        /* Add horizontal entries. */
        if (x > 0) {
            /* Add the left one. */
            if (isAvailable(left) && inv != left) result[idx++] = left;
        }
        if (x < width) {
            /* Add the right one. */
            if (isAvailable(right) && inv != right) result[idx++] = right;
        }

        /* Add vertical entries. */
        if (y > 0) {
            /* Add the up one. */
            if (isAvailable(up) && inv != up) result[idx++] = up;
        }
        if (y < height) {
            /* Add the down one. */
            if (isAvailable(down) && inv != down) result[idx++] = down;
        }
        return result;
    }

    @Override
    public void consume(int id) {
        steps.clear(id >> 1);
    }

    /**
     * Get the start position of a step. This simply calls
     * <code>{@linkplain #getEnd(int)}({@linkplain #invert(int)}(id))</code>
     * really.
     * 
     * @param id the step id
     * 
     * @return the start position of the step
     */
    public Point getStart(int id) {
        return getEnd(invert(id));
    }

    @Override
    public Point getEnd(int id) {
        validate(id);
        final int dx, dy;
        final int scale;
        if (id < horizontals) {
            scale = width;
            dy = 0;
            dx = 1 - (id & 1);
        } else {
            id -= horizontals;
            scale = width + 1;
            dx = 0;
            dy = 1 - (id & 1);
        }
        final int halfId = id >>> 1;
        final int y = halfId / scale + dy;
        final int x = halfId % scale + dx;
        return new Point(x, y);
    }

    /**
     * Get the id of the inverse of a step.
     * 
     * @param id the input step
     * 
     * @return the inverse of the input step
     */
    public int invert(int id) {
        return id ^ 1;
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        int width = 0;
        int height = 0;
        Console console = System.console();
        String line;
        BitSet data = new BitSet();
        while ((line = console.readLine()) != null) {
            int length = line.length();
            if (length > width) {
                BitSet alt = new BitSet();
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        if (data.get(x + y * width)) alt.set(x + y * length);
                data = alt;
                width = length;
            }
            for (int i = 0; i < length; i++)
                if (line.charAt(i) == '#') data.set(i + height * width);
            height++;
        }
        RectangularGrid grid = new BitSetRectangularGrid(width, height, data);
        RectangularLayout layout = new RectangularLayout(grid);
        Scribe<Point> scribe = new Scribe<Point>() {
            @Override
            public void move(Point to) {
                System.out.printf("(%d, %d)", to.x, to.y);
            }

            @Override
            public void draw(Point to) {
                System.out.printf(" (%d, %d)", to.x, to.y);
            }

            @Override
            public void close() {
                System.out.println(" DONE");
            }
        };
        new Tracer<>(layout, scribe).processAll();
        ;
    }
}
