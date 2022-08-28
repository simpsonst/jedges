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

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Slices multicolour grids into layers using a single optimizer.
 * 
 * @param <G> the grid type
 * 
 * @param <P> the position type
 *
 * @author simpsons
 */
public class BasicSlicer<G, P> implements Slicer<G, P> {
    private final ColorSelector<? super G> selector;
    private final ColorOptimizer<G> optimizer;
    private final LayoutFactory<? super G, P> layerOut;
    private final ColorCollector<? super G> collector;

    /**
     * Create a slicer.
     * 
     * @param collector Determines the set of colours used in the grid.
     * 
     * @param selector Selects a colour to trace, given a set of those
     * yet to be traced.
     * 
     * @param optimizer Optimizes a colour by temporarily including
     * cells of other colours not yet traced.
     * 
     * @param layerOut Generates layouts from a monochrome grid.
     */
    public BasicSlicer(ColorCollector<? super G> collector,
                       ColorSelector<? super G> selector,
                       ColorOptimizer<G> optimizer,
                       LayoutFactory<? super G, P> layerOut) {
        this.collector = collector;
        this.selector = selector;
        this.optimizer = optimizer;
        this.layerOut = layerOut;
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
             * result in simpler paths. */
            ColorOptimization<G> optProc =
                optimizer.optimize(grid, col, colors);
            G monochromeGrid = optProc.getOptimizedGrid();

            /* Create a layout for the grid, and prepare a tracer for it
             * with the appropriate scribe. */
            Layout<P> layout = layerOut.makeLayout(monochromeGrid);
            S scribe = scribes.apply(col);
            scribeOrder.add(scribe);
            processes.add(new Tracer<>(layout, scribe));
        }
    }
}
