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

import java.util.BitSet;
import java.util.function.IntPredicate;

import uk.ac.lancs.edges.DifferenceScribe;
import uk.ac.lancs.edges.Score;
import uk.ac.lancs.edges.Tracer;

/**
 * Stores static optimizations for 3×3 grids.
 *
 * @author simpsons
 */
public final class NineSquareOptimizations {
    private NineSquareOptimizations() {}

    private static Score[] SAVINGS = new Score[512];

    /**
     * Get the saving for flipping the centre cell of a 3×3 grid
     * represented as a bit pattern. Bit 0 represents the top-left cell,
     * bit 1 the top-centre, etc.
     * 
     * @param pattern the bit pattern representing a 3×3 grid
     * 
     * @return the saving in the number of sets of co-ordinates that
     * must be expressed
     */
    public static Score saving(int pattern) {
        if (pattern < 0 || pattern >= 512)
            throw new IllegalArgumentException("not in range 0-511: "
                + pattern);
        return SAVINGS[pattern];
    }

    /**
     * Get the saving for flipping the centre cell of a 3×3 section of a
     * grid.
     * 
     * @param grid the source grid
     * 
     * @param x the X co-ordinate of the centre
     * 
     * @param y the Y co-ordinate of the centre
     * 
     * @param reducer the logic that determines which colours are
     * considered solid
     * 
     * @return the saving
     */
    public static Score saving(RectangularGrid grid, int x, int y,
                               IntPredicate reducer) {
        final int pattern = getPattern(grid, x, y, reducer);
        assert pattern >= 0;
        assert pattern < 512;
        return SAVINGS[pattern];
    }

    /**
     * Get the pattern for a 3×3 section of a grid.
     * 
     * @param grid the source grid
     * 
     * @param x the X co-ordinate of the centre
     * 
     * @param y the Y co-ordinate of the centre
     * 
     * @param reducer the logic that determines which colours are
     * considered solid
     * 
     * @return the pattern representing the grid section, in the range 0
     * to 511
     */
    public static int getPattern(RectangularGrid grid, int x, int y,
                                 IntPredicate reducer) {
        long[] words = BitSetRectangularGrid
            .getData(grid.subgrid(x - 1, y - 1, 3, 3), reducer).toLongArray();
        if (words.length == 0)
            return 0;
        else
            return (int) words[0];
    }

    /* For all 3x3 cell patterns with the centre clear, work out the
     * benefit of adding a cell in the centre. */
    static {
        /* Consider all 256 3x3 patterns around an empty cell. */
        long[] counter = new long[1];
        for (counter[0] = 0; counter[0] < SAVINGS.length; counter[0]++) {
            /* Create a grid for this pattern. */
            BitSet base = BitSet.valueOf(counter);
            RectangularGrid grid = new BitSetRectangularGrid(3, 3, base);

            /* Create a counter set. */
            DifferenceScribe scribe = new DifferenceScribe();

            /* Trace around the pattern to count the steps. */
            new Tracer<>(new RectangularLayout(grid), scribe).processAll();
            scribe.swap();

            /* Flip the centre cell, and trace again to count steps. */
            base.flip(4);
            new Tracer<>(new RectangularLayout(grid), scribe).processAll();

            /* Record the result for future use. */
            assert counter[0] >= 0;
            assert counter[0] < SAVINGS.length;
            SAVINGS[(int) counter[0]] = scribe.getScore();
        }
    }
}
