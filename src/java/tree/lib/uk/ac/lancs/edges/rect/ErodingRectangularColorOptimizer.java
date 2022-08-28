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
import java.util.function.ToIntFunction;

import uk.ac.lancs.edges.ColorOptimization;
import uk.ac.lancs.edges.ColorOptimizer;
import uk.ac.lancs.edges.Score;

/**
 * Assumes that all future cells should be included, then gradually
 * erodes undesirable cells away.
 * 
 * <p>
 * First, a bitmap is created to include all cells of current or future
 * colours. Each cell is also given a flag indicating that it has to be
 * (re-)processed. Repeatedly, an unprocessed cell is picked and
 * evaluated, and then marked processed. According to the evaluation,
 * the cell may be removed. Obviously, a cell already removed does not
 * get removed again, and a cell of the current colour cannot be
 * removed.
 * 
 * <p>
 * Evaluation of a cell involves looking at the 3×3 subgrid around it.
 * {@link NineSquareOptimizations} provides a score expressing what
 * benefit there is to removing the cell. If this is non-negative, it is
 * removied.
 *
 * @author simpsons
 */
public class ErodingRectangularColorOptimizer
    implements ColorOptimizer<RectangularGrid> {
    private class Job implements ColorOptimization<RectangularGrid> {
        private final int width, height;
        private final RectangularGrid grid;
        private final BitSet future;

        private final IntPredicate reducer;

        private final BitSet result;
        private final RectangularGrid resultGrid;
        private final BitSet remaining;

        public Job(RectangularGrid grid, int current, BitSet future) {
            this.grid = grid;
            this.future = future;
            this.width = grid.width();
            this.height = grid.height();

            final int count = width * height;

            /* Express how to pick non-past colours. */
            this.reducer = in -> in == current || future.get(in);

            /* Create a grid of current and future cells. */
            result = new BitSet(count);
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    result.set(x + y * width,
                               reducer.test(grid.color(x, y)));
                }
            resultGrid = new BitSetRectangularGrid(width, height, result);

            /* Mark all cells as needing to be processed. */
            remaining = new BitSet(count);
            remaining.set(0, count);
        }

        private boolean evaluate(int x, int y) {
            return tester.test(scorer.applyAsInt(NineSquareOptimizations
                .saving(resultGrid, x, y, reducer)));
        }

        @Override
        public boolean process() {
            /* Choose co-ordinates of a cell to be processed. */
            int idx = remaining.nextSetBit(0);
            if (idx < 0) return false;
            final int x = idx % width;
            final int y = idx / width;

            /* Make sure we always mark this cell as processed. */
            try {
                /* Skip cells that are not future colours. */
                if (!future.get(grid.color(x, y))) return true;

                /* Determine whether we should remove this cell. */
                if (evaluate(x, y)) {
                    /* Remove the cell. */
                    result.clear(idx);

                    /* Mark surrounding cells as busy, if they are set. */
                    for (int dy = -1; dy <= +1; dy++) {
                        final int ny = y + dy;
                        if (ny < 0 || ny >= height) continue;
                        for (int dx = -1; dx <= +1; dx++) {
                            final int nx = x + dx;
                            if (nx < 0 || nx >= width) continue;
                            final int nidx = nx + ny * width;
                            if (result.get(nidx)) remaining.set(nidx);
                        }
                    }
                }

                return true;
            } finally {
                remaining.clear(idx);
            }
        }

        @Override
        public RectangularGrid getOptimizedGrid() {
            while (process())
                ;
            return resultGrid;
        }
    }

    private final IntPredicate tester;
    private final ToIntFunction<? super Score> scorer;

    /**
     * Create a colour optimizer.
     * 
     * @param scorer a predicate to determine whether a cell should be
     * removed based on a score for the state of a 3×3 matrix around it;
     * it should return positive if changing the cell would improve
     * tracing, negative if not, and zero if it would not change
     * anything
     * 
     * @param eager whether cell changes that incur no change to the
     * score should be applied
     */
    public ErodingRectangularColorOptimizer(ToIntFunction<? super Score> scorer,
                                            boolean eager) {
        this.scorer = scorer;
        this.tester = eager ? s -> s >= 0 : s -> s > 0;
    }

    @Override
    public ColorOptimization<RectangularGrid> optimize(RectangularGrid grid,
                                                       int currentColor,
                                                       BitSet futureColors) {
        return new Job(grid, currentColor, futureColors);
    }
}
