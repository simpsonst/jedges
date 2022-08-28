/*
 * Copyright (c) 2016, Lancaster University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 *  * Neither the name of the copyright holder nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contributors:
 *    Steven Simpson <https://github.com/simpsonst>
 */
package uk.ac.lancs.voronoi;

/**
 * Describes the intersection of two parametric lines. This captures the
 * possibility of the two lines not intersecting. In that case, their
 * being strictly parallel or anti-parallel can be distinguished.
 *
 * @author simpsons
 */
public final class Intersection {
    private final int mode;
    private final double t0, t1;

    /**
     * Describe an intersection between two parametric straight lines.
     * Do not use this constructor if the lines do not intersect. Use
     * {@link #PARALLEL} or {@link #ANTIPARALLEL} instead.
     * 
     * @param t0 the parameter of the first line identifying the
     * intersection
     * 
     * @param t1 the parameter of the second line identifying the
     * intersection
     */
    public Intersection(double t0, double t1) {
        this.mode = 0;
        this.t0 = t0;
        this.t1 = t1;
    }

    private Intersection(int mode) {
        this.mode = mode;
        this.t0 = this.t1 = 0.0;
    }

    /**
     * @resume The non-intersection of parallel lines
     */
    public static final Intersection PARALLEL = new Intersection(+1);

    /**
     * @resume The non-intersection of anti-parallel lines
     */
    public static final Intersection ANTIPARALLEL = new Intersection(-1);

    /**
     * Determine if there was no intersection because the lines are
     * strictly parallel.
     * 
     * @return {@code true} if the lines are pointing in the same
     * direction
     */
    public boolean parallel() {
        return mode > 0;
    }

    /**
     * Determine if there was no intersection because the lines are
     * anti-parallel.
     * 
     * @return {@code true} if the lines are pointing in exactly
     * opposite directions
     */
    public boolean antiparallel() {
        return mode < 0;
    }

    /**
     * Determine if there was an intersection.
     * 
     * @return {@code true} if the lines intersected
     */
    public boolean exists() {
        return mode == 0;
    }

    private void checkExists() {
        if (!exists()) throw new IllegalStateException();
    }

    /**
     * Get the parameter of the first line that yields the intersection.
     * 
     * @return the first line's parameter yielding the intersection
     * 
     * @throws IllegalStateException if the lines do not intersect
     */
    public double t0() {
        checkExists();
        return t0;
    }

    /**
     * Get the parameter of the second line that yields the
     * intersection.
     * 
     * @return the second line's parameter yielding the intersection
     * 
     * @throws IllegalStateException if the lines do not intersect
     */
    public double t1() {
        checkExists();
        return t1;
    }
}
