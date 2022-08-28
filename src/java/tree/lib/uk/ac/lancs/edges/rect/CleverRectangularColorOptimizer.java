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
import java.util.BitSet;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

import uk.ac.lancs.edges.ColorOptimization;
import uk.ac.lancs.edges.ColorOptimizer;
import uk.ac.lancs.edges.Score;

/**
 * Assumes that no future cells should be included, then gradually
 * accretes desirable cells, fills in corners, extends projections, and
 * then reluctantly erodes.
 * 
 * <p>
 * First, a bitmap is created to include all cells of the current
 * colour. Each cell is also given a flag indicating that it has to be
 * (re-)processed. Repeatedly, an unprocessed cell is picked and
 * evaluated, and then marked processed. According to the evaluation,
 * the cell may be added. Obviously, a cell already added does not get
 * added again. If the evaluation does not indicate addition, the cell
 * is tested to see if it is in a corner that can be partially filled.
 * 
 * <p>
 * Evaluation of a cell involves looking at the 3×3 subgrid around it.
 * {@link NineSquareOptimizations} provides a score expressing what
 * benefit there is to including the cell. If this is non-negative, it
 * is included.
 * 
 * <p>
 * When all accretion is complete, the optimizer switches into a
 * reluctant erosion mode, using {@link NineSquareOptimizations} again.
 * It looks for cells that can be removed only if they simplify; if they
 * have no over-all effect, they must remain.
 *
 * @author simpsons
 */
public class CleverRectangularColorOptimizer
    implements ColorOptimizer<RectangularGrid> {
    private class Job implements ColorOptimization<RectangularGrid> {
        private final int width, height;
        private final RectangularGrid colorGrid;
        private final BitSet future;

        private final IntPredicate reducer;

        private final BitSet result;
        private final RectangularGrid resultGrid;
        private final BitSet remaining;

        private boolean eroding;

        public Job(RectangularGrid grid, int current, BitSet future) {
            this.colorGrid = grid;
            this.future = future;
            this.width = grid.width();
            this.height = grid.height();

            final int count = width * height;

            /* Express how to pick non-past colours. */
            this.reducer = in -> in == current || future.get(in);

            /* Create a grid of current cells. We gradually add future
             * cells to these if they improve the result. */
            result = new BitSet(count);
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    result.set(x + y * width, grid.color(x, y) == current);
                }
            resultGrid = new BitSetRectangularGrid(width, height, result);

            /* Mark all cells as needing to be processed. */
            remaining = new BitSet(count);
            remaining.set(0, count);
        }

        @Override
        public boolean process() {
            /* Choose co-ordinates of a cell to be processed. */
            int idx = remaining.nextSetBit(0);
            if (idx < 0) {
                if (eroding) return false;

                /* Start reluctantly eroding. Mark solid cells as
                 * unresolved. */
                eroding = true;
                remaining.or(result);
                return true;
            }
            final int x = idx % width;
            final int y = idx / width;

            /* Make sure we always mark this cell as processed. */
            try {
                /* Skip cells that are not future colours. */
                if (!future.get(colorGrid.color(x, y))) return true;

                /* Determine whether we should change this cell. */
                final int pattern = NineSquareOptimizations
                    .getPattern(resultGrid, x, y, reducer);
                final Score saving = NineSquareOptimizations.saving(pattern);
                if (eroding) {
                    /* We are reluctantly eroding. The cell should be
                     * removed only if it definitely makes the grid
                     * better. */
                    if (erosionScorer.applyAsInt(saving) > 0) {
                        /* Remove the cell. */
                        result.clear(idx);

                        /* Mark surrounding cells as unresolved, if they
                         * are set. */
                        unresolvedAround(x, y, true);
                    }
                } else {
                    if (tester.test(accretionScorer.applyAsInt(saving))) {
                        /* Include the cell. */
                        result.set(idx);

                        /* Mark surrounding cells as unresolved, if they
                         * are clear. */
                        unresolvedAround(x, y, false);
                    } else if (findCorner(x, y, pattern)) {}
                }
                return true;
            } finally {
                remaining.clear(idx);
            }
        }

        /**
         * Selectively mark cells around a point as unresolved.
         * 
         * @param x the X co-ordinate of the centre of the 3×3 area to
         * mark
         * 
         * @param y the Y co-ordinate of the centre of the 3×3 area to
         * mark
         * 
         * @param on the result state of the cell if it is to be marked
         */
        private void unresolvedAround(int x, int y, boolean on) {
            for (int dy = -1; dy <= +1; dy++) {
                final int ny = y + dy;
                if (ny < 0 || ny >= height) continue;
                for (int dx = -1; dx <= +1; dx++) {
                    final int nx = x + dx;
                    if (nx < 0 || nx >= width) continue;
                    final int nidx = nx + ny * width;
                    if (result.get(nidx) == on) remaining.set(nidx);
                }
            }
        }

        /**
         * Mark a cell as unresolved, if it is clear.
         * 
         * @param x the X position
         * 
         * @param y the Y position
         */
        private void unresolved(int x, int y) {
            if (y < 0 || y >= height) return;
            if (x < 0 || x >= width) return;
            final int nidx = x + y * width;
            if (!result.get(nidx)) remaining.set(nidx);
        }

        private void unresolved(Point p) {
            unresolved(p.x, p.y);
        }

        /**
         * Mark a cell as resolved.
         * 
         * @param x the X position
         * 
         * @param y the Y position
         */
        private void resolved(int x, int y) {
            if (y < 0 || y >= height) return;
            if (x < 0 || x >= width) return;
            final int nidx = x + y * width;
            remaining.clear(nidx);
        }

        private void resolved(Point p) {
            resolved(p.x, p.y);
        }

        @SuppressWarnings("unused")
        private void displayCorner(int x, int y, Direction dir, int turn,
                                   int length) {
            /* Display the area eligible to be filled in. */

            /* Find one corner, inclusive. */
            Point p0 = new Point(x, y);
            dir.turn(Direction.ABOUT_TURN).move(p0);
            dir.turn(turn).move(p0);

            /* Find the opposite corner, exclusive. */
            Point p1 = new Point(p0);
            dir.move(p1, length + 2);
            dir.turn(-turn).move(p1, 3);

            /* Normalize the points, so p0 is the top-left corner, and
             * p1 the bottom right. */
            if (p1.x < p0.x) {
                int tmp = p0.x + 1;
                p0.x = p1.x + 1;
                p1.x = tmp;
            }
            if (p1.y < p0.y) {
                int tmp = p0.y + 1;
                p0.y = p1.y + 1;
                p1.y = tmp;
            }

            /* Draw the area. */
            System.err.printf("(%d, %d) %s %d:%n", x, y, dir, turn);
            colorGrid.subgrid(p0.x, p0.y, p1.x - p0.x, p1.y - p0.y)
                .print(System.err, "-#O?");
            System.err.println();
            resultGrid.subgrid(p0.x, p0.y, p1.x - p0.x, p1.y - p0.y)
                .print(System.err, "-#?");
            System.err.println();
        }

        private boolean checkCorner(int x, int y, Direction dir, int turn) {
            /* First find out how far this corner goes and how it ends.
             * If the side stays solid, but the line includes a cell of
             * a past colour, we fail. Otherwise, we stop when we reach
             * a solid, or the side reaches a non-solid. */
            int length = 0;
            {
                Point p = new Point(x, y);
                Point side = new Point(p);
                dir.turn(turn).move(side);
                for (;;) {
                    /* Move the co-ordinates on one cell. */
                    length++;
                    dir.move(p);
                    dir.move(side);

                    if (result.get(p.x + p.y * width)) {
                        /* We encountered a solid, so stop. */
                        break;
                    }
                    if (!result.get(side.x + side.y * width)) {
                        /* We encountered a blank to our side, so stop. */
                        break;
                    }
                    if (!reducer.test(colorGrid.color(p.x, p.y))) {
                        /* We encountered a past colour, so we fail. */
                        return false;
                    }
                }
            }
            /* We've found a corner that can be filled in. The
             * conditions that led to the invocation of this method mean
             * that we must have found a corner of at least two cells,
             * or no suitable corner at all. */
            assert length > 1;
            // displayCorner(x, y, dir, turn, length);

            /* We can set all the cells we walked over, mark them as
             * resolved, and mark adjacent cells as unresolved. Start
             * one line earlier, so we can mark these cells as
             * unresolved. */
            Point p = new Point(x, y);
            dir.turn(Direction.ABOUT_TURN).move(p);
            Point side1 = new Point(p);
            dir.turn(turn).move(side1);
            Point side2 = new Point(p);
            dir.turn(-turn).move(side2);
            unresolved(p);
            unresolved(side1);
            unresolved(side2);

            /* Move along to the correct start position. */
            dir.move(p);
            dir.move(side1);
            dir.move(side2);

            /* Walk the line, setting the centre cell, marking it as
             * resolved, and marking cells either side as unresolved. */
            for (int i = 0; i < length; i++) {
                /* Include the cell, mark it as resolved, and mark cells
                 * either side as unresolved. */
                result.set(p.x + p.y * width);
                resolved(p);
                unresolved(side1);
                unresolved(side2);

                /* Move along. */
                dir.move(p);
                dir.move(side1);
                dir.move(side2);
            }

            /* Mark the final line as unresolved. */
            unresolved(p);
            unresolved(side1);
            unresolved(side2);

            /* Indicate that we made changes, so we don't immediately
             * look for something else. */
            return true;
        }

        /**
         * Look for corner patterns, and then check how far the corner
         * goes.
         * 
         * @param x the X co-ordinate of the potential corner
         * 
         * @param y the Y co-ordinate of the potential corner
         * 
         * @param pattern the pattern of cells around the potential
         * corner
         * 
         * @return true if a corner was found and filled in
         */
        private boolean findCorner(int x, int y, int pattern) {
            /* Check for projections. */

            if (match(pattern, 8 + 64, 1 + 16 + 128)) {
                // -??
                // #-?
                // #-?
                return checkCorner(x, y, Direction.DOWN,
                                   Direction.RIGHT_TURN_90);
            }
            if (match(pattern, 32 + 256, 4 + 16 + 128)) {
                // ??-
                // ?-#
                // ?-#
                return checkCorner(x, y, Direction.DOWN,
                                   Direction.LEFT_TURN_90);
            }

            if (match(pattern, 1 + 2, 4 + 8 + 16)) {
                // ##-
                // --?
                // ???
                return checkCorner(x, y, Direction.LEFT,
                                   Direction.RIGHT_TURN_90);
            }
            if (match(pattern, 64 + 128, 8 + 16 + 256)) {
                // ???
                // --?
                // ##-
                return checkCorner(x, y, Direction.LEFT,
                                   Direction.LEFT_TURN_90);
            }

            if (match(pattern, 1 + 8, 2 + 16 + 64)) {
                // #-?
                // #-?
                // -??
                return checkCorner(x, y, Direction.UP,
                                   Direction.LEFT_TURN_90);
            }
            if (match(pattern, 4 + 32, 2 + 16 + 256)) {
                // ?-#
                // ?-#
                // ??-
                return checkCorner(x, y, Direction.UP,
                                   Direction.RIGHT_TURN_90);
            }

            if (match(pattern, 2 + 4, 1 + 16 + 32)) {
                // -##
                // ?--
                // ???
                return checkCorner(x, y, Direction.RIGHT,
                                   Direction.LEFT_TURN_90);
            }
            if (match(pattern, 128 + 256, 16 + 32 + 64)) {
                // ???
                // ?--
                // -##
                return checkCorner(x, y, Direction.RIGHT,
                                   Direction.RIGHT_TURN_90);
            }

            /* Check for corners. */

            if (match(pattern, 1 + 2 + 8 + 64, 4 + 128)) {
                // ##-
                // #??
                // #-?
                return checkCorner(x, y, Direction.DOWN,
                                   Direction.RIGHT_TURN_90);
            }
            if (match(pattern, 1 + 2 + 4 + 8, 32 + 64)) {
                // ###
                // #?-
                // -??
                return checkCorner(x, y, Direction.RIGHT,
                                   Direction.LEFT_TURN_90);
            }
            if (match(pattern, 1 + 2 + 4 + 8 + 64, 32 + 128)) {
                // ###
                // #?-
                // #-?
                return checkCorner(x, y, Direction.DOWN,
                                   Direction.RIGHT_TURN_90)
                    || checkCorner(x, y, Direction.RIGHT,
                                   Direction.LEFT_TURN_90);
            }

            if (match(pattern, 2 + 4 + 32 + 256, 1 + 128)) {
                // -##
                // ??#
                // ?-#
                return checkCorner(x, y, Direction.DOWN,
                                   Direction.LEFT_TURN_90);
            }
            if (match(pattern, 1 + 2 + 4 + 32, 8 + 256)) {
                // ###
                // -?#
                // ??-
                return checkCorner(x, y, Direction.LEFT,
                                   Direction.RIGHT_TURN_90);
            }
            if (match(pattern, 1 + 2 + 4 + 32 + 256, 8 + 128)) {
                // ###
                // -?#
                // ?-#
                return checkCorner(x, y, Direction.LEFT,
                                   Direction.RIGHT_TURN_90)
                    || checkCorner(x, y, Direction.DOWN,
                                   Direction.LEFT_TURN_90);
            }

            if (match(pattern, 4 + 32 + 128 + 256, 2 + 64)) {
                // ?-#
                // ??#
                // -##
                return checkCorner(x, y, Direction.UP,
                                   Direction.RIGHT_TURN_90);
            }
            if (match(pattern, 32 + 64 + 128 + 256, 4 + 8)) {
                // ??-
                // -?#
                // ###
                return checkCorner(x, y, Direction.LEFT,
                                   Direction.LEFT_TURN_90);
            }
            if (match(pattern, 4 + 32 + 64 + 128 + 256, 2 + 8)) {
                // ?-#
                // -?#
                // ###
                return checkCorner(x, y, Direction.LEFT,
                                   Direction.LEFT_TURN_90)
                    || checkCorner(x, y, Direction.UP,
                                   Direction.RIGHT_TURN_90);
            }

            if (match(pattern, 1 + 8 + 64 + 128, 2 + 256)) {
                // #-?
                // #??
                // ##-
                return checkCorner(x, y, Direction.UP,
                                   Direction.LEFT_TURN_90);
            }
            if (match(pattern, 8 + 64 + 128 + 256, 1 + 32)) {
                // -??
                // #?-
                // ###
                return checkCorner(x, y, Direction.RIGHT,
                                   Direction.RIGHT_TURN_90);
            }
            if (match(pattern, 1 + 8 + 64 + 128 + 256, 2 + 32)) {
                // #-?
                // #?-
                // ###
                return checkCorner(x, y, Direction.RIGHT,
                                   Direction.RIGHT_TURN_90)
                    || checkCorner(x, y, Direction.UP,
                                   Direction.LEFT_TURN_90);
            }

            return false;
        }

        @Override
        public RectangularGrid getOptimizedGrid() {
            while (process())
                ;
            return resultGrid;
        }
    }

    private static boolean match(int pattern, int included, int excluded) {
        assert (included & excluded) == 0;
        return (pattern & included) == included && (pattern & excluded) == 0;
    }

    private final IntPredicate tester;
    private final ToIntFunction<? super Score> accretionScorer, erosionScorer;

    /**
     * Create a colour optimizer with a scorer for both accretion and
     * erosion.
     * 
     * @param scorer a predicate to determine whether a cell should be
     * changed based on a score for the state of a 3×3 matrix around it;
     * it should return positive if changing the cell would improve
     * tracing, negative if not, and zero if it would not change
     * anything
     * 
     * @param eager whether cell additions that incur no change to the
     * score should be applied
     */
    public CleverRectangularColorOptimizer(ToIntFunction<? super Score> scorer,
                                           boolean eager) {
        this(scorer, scorer, eager);
    }

    /**
     * Create a colour optimizer with separate scorers for accretion and
     * erosion.
     * 
     * @param accretionScorer a predicate to determine whether a cell
     * should be added based on a score for the state of a 3×3 matrix
     * around it; it should return positive if changing the cell would
     * improve tracing, negative if not, and zero if it would not change
     * anything
     * 
     * @param erosionScorer a predicate to determine whether a cell
     * should be removed based on a score for the state of a 3×3 matrix
     * around it; it should return positive if changing the cell would
     * improve tracing, negative if not, and zero if it would not change
     * anything
     * 
     * @param eager whether cell additions that incur no change to the
     * score should be applied
     */
    public CleverRectangularColorOptimizer(ToIntFunction<? super Score> accretionScorer,
                                           ToIntFunction<? super Score> erosionScorer,
                                           boolean eager) {
        this.accretionScorer = accretionScorer;
        this.erosionScorer = erosionScorer;
        this.tester = eager ? s -> s >= 0 : s -> s > 0;
    }

    /**
     * @resume An instance with the best parameters
     */
    public static final CleverRectangularColorOptimizer BEST_INSTANCE =
        new CleverRectangularColorOptimizer(s -> s.draws(),
                                            s -> s.movesAndDraws(), true);

    @Override
    public ColorOptimization<RectangularGrid> optimize(RectangularGrid grid,
                                                       int currentColor,
                                                       BitSet futureColors) {
        return new Job(grid, currentColor, futureColors);
    }
}
