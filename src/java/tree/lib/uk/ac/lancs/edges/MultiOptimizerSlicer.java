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

package uk.ac.lancs.edges;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Slices multicolour grids into layers using a multiple optimizers, and
 * choosing the best one on each layer.
 * 
 * @param <G> the grid type
 * 
 * @param <P> the position type
 *
 * @author simpsons
 */
public class MultiOptimizerSlicer<G, P> implements Slicer<G, P> {
    private final ColorSelector<? super G> selector;
    private final Collection<? extends ColorOptimizer<G>> optimizers;
    private final LayoutFactory<? super G, P> layerOut;
    private final ColorCollector<? super G> collector;
    private final Comparator<? super Score> scorer;

    /**
     * Create a slicer.
     * 
     * @param collector Determines the set of colours used in the grid.
     * 
     * @param selector Selects a colour to trace, given a set of those
     * yet to be traced.
     * 
     * @param optimizers a set of optimizers to be tried on each layer
     * to optimize a colour by temporarily including cells of other
     * colours not yet traced
     * 
     * @param layerOut Generates layouts from a monochrome grid.
     * 
     * @param scorer Compares scores of traces made by competing
     * optimizers.
     */
    public MultiOptimizerSlicer(ColorCollector<? super G> collector,
                                ColorSelector<? super G> selector,
                                Collection<? extends ColorOptimizer<G>> optimizers,
                                LayoutFactory<? super G, P> layerOut,
                                Comparator<? super Score> scorer) {
        if (optimizers.isEmpty())
            throw new IllegalArgumentException("no optimizers");
        this.collector = collector;
        this.selector = selector;
        this.optimizers = optimizers;
        this.layerOut = layerOut;
        this.scorer = scorer;
    }

    @Override
    public <S extends Scribe<? super P>> void
        slice(G grid, IntFunction<S> scribes,
              Collection<? super Process> processes,
              List<? super S> scribeOrder) {
        /* Get a set of available non-transparent colours. */
        BitSet colors = collector.collectColors(grid);
        colors.clear(0);

        /* Slice the grid into layers of different colours. */
        for (;;) {
            /* Select a colour to trace. */
            int col = selector.selectColor(grid, colors);
            if (col < 1) break;

            /* Exclude the chosen colour from the future set. */
            colors.clear(col);

            /* Optimize the selected colour by looking for cells of
             * future colours that could be temporarily included to
             * result in simpler paths. Try every optimizer, and get a
             * complete scored and recorded trace for each one. */
            List<ReplayingScribe<P>> optResults =
                new ArrayList<>(optimizers.size());
            Collection<Process> procs = new ArrayList<>();
            for (ColorOptimizer<G> optimizer : optimizers) {
                ColorOptimization<G> optProc =
                    optimizer.optimize(grid, col, colors);
                G monochromeGrid = optProc.getOptimizedGrid();
                Layout<P> layout = layerOut.makeLayout(monochromeGrid);
                ReplayingScribe<P> scribe = new ReplayingScribe<>();
                optResults.add(scribe);
                procs.add(new Tracer<>(layout, scribe));
            }
            procs.parallelStream().forEach(Process::processAll);

            /* Choose the scribe with the lowest score. */
            ReplayingScribe<P> bestScribe = null;
            Score bestScore = null;
            for (ReplayingScribe<P> scribe : optResults) {
                Score score = scribe.getScore();
                if (bestScore == null
                    || scorer.compare(score, bestScore) < 0) {
                    bestScore = score;
                    bestScribe = scribe;
                }
            }
            assert bestScribe != null;

            /* Create a layout for the grid, and prepare a tracer for it
             * with the appropriate scribe. */
            S scribe = scribes.apply(col);
            scribeOrder.add(scribe);
            processes.add(bestScribe.replay(scribe));
        }
    }
}
