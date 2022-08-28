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
 * Models a polygon made up of linear steps.
 * 
 * @param <P> the co-ordinate type for the starts and ends of steps
 *
 * @author simpsons
 */
public interface Layout<P> {
    /**
     * Get an unconsumed step, preferably at a corner.
     * 
     * @return an unconsumed step, or {@code -1} if none left
     */
    int getAnyStep();

    /**
     * Determine whether two steps point in the same direction.
     * 
     * @param id1 the first step
     * 
     * @param id2 the second step
     * 
     * @return true if the two steps point in the same direction
     */
    boolean areParallel(int id1, int id2);

    /**
     * Determine whether two steps point in opposite directions.
     * 
     * @param id1 the first step
     * 
     * @param id2 the second step
     * 
     * @return true if the two steps point in opposite directions
     */
    boolean areAntiparallel(int id1, int id2);

    /**
     * Get potential steps that follow from a given one, i.e., their
     * start positions are the same as the end position of the given
     * one.
     * 
     * @return an array of ids of following steps
     */
    int[] getOptions(int id);

    /**
     * Consume a step. Mark it as traced.
     * 
     * @param id the step to consume
     */
    void consume(int id);

    /**
     * Get the end co-ordinates of a step.
     * 
     * @param id the step whose end co-ordinates are sought
     * 
     * @return the end co-ordinates
     */
    P getEnd(int id);
}
