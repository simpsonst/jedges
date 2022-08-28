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
import java.io.PrintStream;
import java.util.BitSet;
import java.util.Random;

import uk.ac.lancs.edges.ColorOptimization;
import uk.ac.lancs.edges.ColorSelector;
import uk.ac.lancs.edges.Scribe;
import uk.ac.lancs.edges.Tracer;

final class RectangularColorOptimizerTester {
    private static void print(PrintStream out, RectangularGrid grid) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                switch (grid.color(x, y)) {
                case 0:
                    out.print('-');
                    break;
                case 1:
                    out.print('#');
                    break;
                case 2:
                    out.print('O');
                    break;
                default:
                    out.print('?');
                    break;
                }
            }
            out.println();
        }
    }

    private static class StepCounter implements Scribe<Point> {
        private int moves, draws;

        @Override
        public void move(Point to) {
            moves++;
        }

        @Override
        public void draw(Point to) {
            draws++;
        }

        @Override
        public void close() {}

        public int moves() {
            return moves;
        }

        public int draws() {
            return draws;
        }

        public int steps() {
            return moves + draws;
        }
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        long seed;
        if (args.length > 0) {
            seed = Long.parseLong(args[0]);
        } else {
            seed = new Random().nextLong();
        }
        Random rng = new Random(seed);

        /* 2 is always a future colour. */
        BitSet future = new BitSet();
        future.set(2);

        /* Create a random grid. */
        int width = 10, height = 10;
        byte[] data = new byte[width * height];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte) rng.nextInt(3);
        RectangularGrid grid =
            new ByteArrayRectangularGrid(width, height, data);
        System.out.println();
        System.out.println("Remaining colours:");
        print(System.out, grid);

        {
            ColorSelector<RectangularGrid> selector =
                new PerimeterRectangularColorSelector();
            BitSet remainingColors = new BitSet(3);
            remainingColors.set(1, 3);

            while (remainingColors.nextSetBit(0) >= 0) {
                int chosen = selector.selectColor(grid, remainingColors);
                System.out.printf("Paint colour %d%n", chosen);
                remainingColors.clear(chosen);
            }
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                MinimalRectangularColorOptimizer.INSTANCE.optimize(grid, 1,
                                                                   future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Minimal:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new AccretingRectangularColorOptimizer(s -> s.movesAndDraws(),
                                                       true).optimize(grid, 1,
                                                                      future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Eager accreting:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new AccretingRectangularColorOptimizer(s -> s.movesAndDraws(),
                                                       false)
                                                           .optimize(grid, 1,
                                                                     future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Reluctant accreting:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new CleverRectangularColorOptimizer(s -> s.movesAndDraws(),
                                                    true).optimize(grid, 1,
                                                                   future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Eager clever:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new CleverRectangularColorOptimizer(s -> s.movesAndDraws(),
                                                    false).optimize(grid, 1,
                                                                    future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Reluctant clever:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new CleverRectangularColorOptimizer(s -> s.movesAndDraws(),
                                                    s -> s.draws(), true)
                                                        .optimize(grid, 1,
                                                                  future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Eager clever, draws only on erosion:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new CleverRectangularColorOptimizer(s -> s.movesAndDraws(),
                                                    s -> s.draws(), false)
                                                        .optimize(grid, 1,
                                                                  future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Reluctant clever, draws only on erosion:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new ErodingRectangularColorOptimizer(s -> s.movesAndDraws(),
                                                     true).optimize(grid, 1,
                                                                    future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Eager eroding:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new ErodingRectangularColorOptimizer(s -> s.movesAndDraws(),
                                                     false).optimize(grid, 1,
                                                                     future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Reluctant eroding:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new AccretingRectangularColorOptimizer(s -> s.draws(), true)
                    .optimize(grid, 1, future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Eager accreting, draws only:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new AccretingRectangularColorOptimizer(s -> s.draws(), false)
                    .optimize(grid, 1, future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Reluctant accreting, draws only:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new CleverRectangularColorOptimizer(s -> s.draws(),
                                                    s -> s
                                                        .movesAndDraws(),
                                                    true).optimize(grid, 1,
                                                                   future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Eager clever, draws only on accretion:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new CleverRectangularColorOptimizer(s -> s.draws(),
                                                    s -> s
                                                        .movesAndDraws(),
                                                    false).optimize(grid, 1,
                                                                    future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Reluctant clever, draws only on accretion:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new CleverRectangularColorOptimizer(s -> s.draws(), true)
                    .optimize(grid, 1, future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Eager clever, draws only:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new CleverRectangularColorOptimizer(s -> s.draws(), false)
                    .optimize(grid, 1, future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Reluctant clever, draws only:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new ErodingRectangularColorOptimizer(s -> s.draws(), true)
                    .optimize(grid, 1, future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Eager eroding, draws only:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        {
            /* Perform the optimization and show the result. */
            ColorOptimization<RectangularGrid> opt =
                new ErodingRectangularColorOptimizer(s -> s.draws(), false)
                    .optimize(grid, 1, future);
            RectangularGrid result = opt.getOptimizedGrid();
            System.out.println();
            System.out.println("Reluctant eroding, draws only:");
            print(System.out, result);
            StepCounter scribe = new StepCounter();
            new Tracer<>(new RectangularLayout(result), scribe).processAll();
            System.out.printf("Steps: %d; lines: %d; moves: %d%n",
                              scribe.steps(), scribe.draws(), scribe.moves());
        }

        System.out.printf("Seed: %d%n", seed);
    }
}
