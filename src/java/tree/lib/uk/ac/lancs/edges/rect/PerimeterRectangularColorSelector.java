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
import java.util.HashMap;
import java.util.Map;

import uk.ac.lancs.edges.ColorSelector;

/**
 * Identifies the colour most frequently next to already traced cells.
 *
 * @author simpsons
 */
public class PerimeterRectangularColorSelector
    implements ColorSelector<RectangularGrid> {
    /**
     * @resume The score awarded to a colour if it touches a
     * transparent cell at their sides
     */
    public final double diagonalScore;

    /**
     * @resume The score awarded to a colour if it touches a
     * transparent cell at their corners
     */
    public final double orthogonalScore;

    /**
     * @resume The default diagonal score of {@value}
     */
    public static final double DEFAULT_DIAGONAL_SCORE = 0.7;

    /**
     * @resume The default orthogonal score of {@value}
     */
    public static final double DEFAULT_ORTHOGONAL_SCORE = 1.0;

    /**
     * Create a colour selector with default scores
     * {@value #DEFAULT_ORTHOGONAL_SCORE} for orthogonals and
     * {@value #DEFAULT_DIAGONAL_SCORE} for diagonals.
     */
    public PerimeterRectangularColorSelector() {
        this(DEFAULT_ORTHOGONAL_SCORE, DEFAULT_DIAGONAL_SCORE);
    }

    /**
     * Create a colour selector with a specific score for orthogonals,
     * and zero for diagonals.
     * 
     * @param orthogonalScore the score awarded to a colour if it
     * touches a transparent cell at their corners
     */
    public PerimeterRectangularColorSelector(double orthogonalScore) {
        this(orthogonalScore, 0.0);
    }

    /**
     * Create a colour selector with specific scores for diagonals and
     * orthogonals.
     * 
     * @param orthogonalScore the score awarded to a colour if it
     * touches a transparent cell at their corners
     * 
     * @param diagonalScore the score awarded to a colour if it touches
     * a transparent cell at their sides
     */
    public PerimeterRectangularColorSelector(double orthogonalScore,
                                             double diagonalScore) {
        this.orthogonalScore = orthogonalScore;
        this.diagonalScore = diagonalScore;
    }

    @Override
    public int selectColor(RectangularGrid grid, BitSet colors) {
        Map<Integer, Double> counters = new HashMap<>();
        final int width = grid.width();
        final int height = grid.height();
        for (int y = 0; y <= height; y++)
            for (int x = 0; x <= width; x++) {
                final int c0 = grid.color(x - 1, y - 1);
                final int c1 = grid.color(x - 1, y);
                final int c2 = grid.color(x, y - 1);
                final int c3 = grid.color(x, y);
                if (colors.get(c3)) {
                    /* The colour of the current cell is yet to be
                     * traced. It should be counted against each of its
                     * transparent neighbours. */
                    final double inc = (colors.get(c0) ? 0.0 : diagonalScore)
                        + (colors.get(c1) ? 0.0 : orthogonalScore)
                        + (colors.get(c2) ? 0.0 : orthogonalScore);
                    counters.put(c3, counters.getOrDefault(c3, 0.0) + inc);
                } else {
                    /* The cell has already been traced. Each of its
                     * non-transparent neighbours should be counted. */
                    if (colors.get(c0)) counters
                        .put(c0,
                             counters.getOrDefault(c0, 0.0) + diagonalScore);
                    if (colors.get(c1))
                        counters.put(c1, counters.getOrDefault(c1, 0.0)
                            + orthogonalScore);
                    if (colors.get(c2))
                        counters.put(c2, counters.getOrDefault(c2, 0.0)
                            + orthogonalScore);
                }
            }

        int bestColor = -1;
        double bestCount = 0;
        for (Map.Entry<Integer, Double> entry : counters.entrySet()) {
            double value = entry.getValue();
            if (value > bestCount) {
                bestCount = value;
                bestColor = entry.getKey();
            }
        }
        return bestColor;
    }
}
