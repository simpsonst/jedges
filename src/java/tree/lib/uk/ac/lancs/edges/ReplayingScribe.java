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
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Records and scores a path, allowing it to be played back. It is
 * intended for use when several alternative tracings are possible, and
 * the best (i.e., with the lowest score) must be chosen without having
 * to compute the trace again.
 * 
 * @param <P> the position type
 *
 * @author simpsons
 */
public class ReplayingScribe<P> implements Scribe<P> {
    private final List<Consumer<Scribe<? super P>>> actions =
        new ArrayList<>();
    private int moves, draws;

    /**
     * Get the score at the current moment.
     * 
     * @return an immutable record of the number of moves and draws made
     * so far
     */
    public Score getScore() {
        final int moves = this.moves;
        final int draws = this.draws;

        return new Score() {
            @Override
            public int moves() {
                return moves;
            }

            @Override
            public int draws() {
                return draws;
            }
        };
    }

    /**
     * Replay to another scribe.
     * 
     * @param to the destination scribe
     * 
     * @return a process that will replay all steps to the given scribe
     */
    public Process replay(Scribe<? super P> to) {
        return new Process() {
            final Iterator<Consumer<Scribe<? super P>>> iter =
                actions.iterator();

            @Override
            public boolean process() {
                if (!iter.hasNext()) return false;
                iter.next().accept(to);
                return true;
            }
        };
    }

    @Override
    public void move(P to) {
        moves++;
        actions.add(new Consumer<Scribe<? super P>>() {
            @Override
            public void accept(Scribe<? super P> t) {
                t.move(to);
            }
        });
    }

    @Override
    public void draw(P to) {
        draws++;
        actions.add(new Consumer<Scribe<? super P>>() {
            @Override
            public void accept(Scribe<? super P> t) {
                t.draw(to);
            }
        });
    }

    @Override
    public void close() {
        actions.add(new Consumer<Scribe<? super P>>() {
            @Override
            public void accept(Scribe<? super P> t) {
                t.close();
            }
        });
    }
}
