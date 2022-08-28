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

/**
 * Initiates optimization of a colour grid based on current and future
 * colours.
 * 
 * @param <G> the grid type
 *
 * @author simpsons
 */
@FunctionalInterface
public interface ColorOptimizer<G> {
    /**
     * Optimize a grid. The colour to be optimized is specified, along
     * with a set of other ('future') colours that have not yet been
     * traced. The goal is to selectively include some cells with future
     * colours as if they are part of the current colour, so that
     * tracing yields fewer paths or path elements. This is valid so
     * long as those cells will indeed be painted over with a different
     * colour.
     * 
     * @param grid the input grid
     * 
     * @param currentColor the code for the current colour, the one
     * whose shape is to be optimized before tracing
     * 
     * @param futureColors a set of colour indices that have not yet
     * been traced
     * 
     * @return the optimizer for the grid
     * 
     * @throws IllegalArgumentException if the current or future colours
     * include zero (which should always be treated as a 'past' or
     * transparent colour), or if the future colours include the current
     * colour.
     */
    ColorOptimization<G> optimize(G grid, int currentColor,
                                  BitSet futureColors);
}
