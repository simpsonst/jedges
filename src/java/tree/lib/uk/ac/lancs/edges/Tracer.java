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

/**
 * Traces an efficient outline of a 2D polygon exploiting an even-odd
 * fill rule.
 * 
 * @param <P> the position type
 *
 * @author simpsons
 */
public final class Tracer<P> implements Process {
    private final Layout<P> layout;
    private final Scribe<? super P> scribe;

    /**
     * Create a tracer for a polygon. The polygon is described as a
     * collection of steps, each being a straight line of the polygon,
     * though not necessarily a complete edge. Each step knows its start
     * and end position, and has an inverse whose start and end
     * positions are swapped. Each step also knows what steps lead out
     * from its start position.
     * 
     * <p>
     * The collection of steps is modified while walking.
     * {@link Layout#consume(int)} removes a step and its inverse from
     * the collection. The collection's iterator should favour steps
     * that start at corners.
     * 
     * <p>
     * The caller provides a {@link Scribe} to receive drawing
     * instructions for the polygon.
     * 
     * @param layout a description of the polygon to trace
     * 
     * @param scribe a receiver of drawing commands
     */
    public Tracer(Layout<P> layout, Scribe<? super P> scribe) {
        this.layout = layout;
        this.scribe = scribe;
    }

    private int step = -1;
    private boolean foundCorner;

    @Override
    public boolean process() {
        if (step == -1) {
            step = layout.getAnyStep();
            if (step == -1) return false;
            foundCorner = false;
        }

        /* Consider the next step. */
        assert step != -1;
        if (foundCorner) {
            layout.consume(step);
        }
        int[] alts = layout.getOptions(step);
        int chosen = -1;
        int secondary = -1;
        boolean turn = true;
        for (int i1 = 0; i1 < alts.length; i1++) {
            final int cand = alts[i1];
            if (cand == -1) continue;

            /* If this candidate carries on in the same direction,
             * choose it at once. */
            if (layout.areParallel(cand, step)) {
                turn = false;
                chosen = cand;
                break;
            }

            /* Check among the other candidates for ones that are
             * anti-parallel. */
            for (int i2 = i1 + 1; i2 < alts.length; i2++) {
                final int cand2 = alts[i2];
                if (cand2 == -1) continue;
                assert cand2 != cand;
                if (layout.areAntiparallel(cand, cand2)) secondary = cand;
            }
            if (secondary == cand) continue;

            /* This candidate is not anti-parallel to any other options,
             * so choose it for now. */
            chosen = cand;
        }

        /* Choose our next step. */
        final int next;
        if (chosen != -1) {
            /* This one is either straight ahead, or not parallel to any
             * other options. */
            next = chosen;
        } else if (secondary != -1) {
            /* This is one we prefer not to take, as we miss an
             * opportunity to traverse our current position in a
             * straight line. */
            next = secondary;
        } else {
            /* We've reached the end of the path. */
            next = -1;
        }

        /* Draw the previous line if we're turning, or have reached the
         * end. */
        if (foundCorner) {
            if (turn || next == -1) scribe.draw(layout.getEnd(step));
            if (next == -1) scribe.close();
        } else {
            if (turn) {
                foundCorner = true;
                scribe.move(layout.getEnd(step));
            }
        }
        step = next;
        return true;
    }
}
