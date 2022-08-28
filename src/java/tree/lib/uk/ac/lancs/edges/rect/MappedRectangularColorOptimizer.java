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

import uk.ac.lancs.edges.ColorOptimization;
import uk.ac.lancs.edges.ColorOptimizer;

/**
 * Performs the most na&iuml;ve optimization of a multicolour
 * rectangular grid, whereby future colours are simply mapped.
 *
 * @author simpsons
 */
public abstract class MappedRectangularColorOptimizer
    implements ColorOptimizer<RectangularGrid> {
    /**
     * Get the mapping scheme. Subclasses implement this method to
     * provide the mapping from colour to transpatent/opaque status.
     * 
     * @param currentColor the colour being optimized
     * 
     * @param futureColors colours that have not yet been traced
     * 
     * @return the mapping scheme appropriate for the inputs
     */
    protected abstract IntPredicate getReducer(int currentColor,
                                               BitSet futureColors);

    @Override
    public final ColorOptimization<RectangularGrid>
        optimize(RectangularGrid grid, int currentColor,
                 BitSet futureColors) {
        IntPredicate mapper = getReducer(currentColor, futureColors);
        RectangularGrid result = new RectangularGrid() {
            @Override
            public int width() {
                return grid.width();
            }

            @Override
            public int height() {
                return grid.height();
            }

            @Override
            public int color(int x, int y) {
                return mapper.test(grid.color(x, y)) ? 1 : 0;
            }
        };

        return new ColorOptimization<RectangularGrid>() {
            @Override
            public boolean process() {
                return false;
            }

            @Override
            public RectangularGrid getOptimizedGrid() {
                return result;
            }
        };
    }
}
