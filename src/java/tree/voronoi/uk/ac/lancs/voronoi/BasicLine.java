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

import java.awt.geom.Point2D;

/**
 * Defines a line by a centre point where the parameter is 0, and the
 * offset to another point where the parameter is 1.
 */
public class BasicLine implements Line {
    private final double cx, cy, sx, sy;
    private double min, max;

    /**
     * Create a basic line.
     * 
     * @param cx the X co-ordinate when the parameter is 0
     * 
     * @param cy the Y co-ordinate when the parameter is 0
     * 
     * @param sx the X offset from {@code cx} when the parameter is 1
     * 
     * @param sy the Y offset from {@code cy} when the parameter is 1
     * 
     * @param min the initial minimum parameter value
     * 
     * @param max the initial maximum parameter value
     */
    public BasicLine(double cx, double cy, double sx, double sy, double min,
                     double max) {
        this(cx, cy, sx, sy, min, max, null);
    }

    /**
     * Create a basic line.
     * 
     * @param cx the X co-ordinate when the parameter is 0
     * 
     * @param cy the Y co-ordinate when the parameter is 0
     * 
     * @param sx the X offset from {@code cx} when the parameter is 1
     * 
     * @param sy the Y offset from {@code cy} when the parameter is 1
     * 
     * @param min the initial minimum parameter value
     * 
     * @param max the initial maximum parameter value
     * 
     * @param reverse the reverse view of this line
     */
    private BasicLine(double cx, double cy, double sx, double sy, double min,
                      double max, BasicLine reverse) {
        this.cx = cx;
        this.cy = cy;
        this.sx = sx;
        this.sy = sy;
        this.min = min;
        this.max = max;
        this.reverse = reverse;
    }

    @Override
    public double xAxisAngle() {
        return Math.atan2(sy, sx);
    }

    /**
     * Create a basic line of infinite length.
     * 
     * @param cx the X co-ordinate when the parameter is 0
     * 
     * @param cy the Y co-ordinate when the parameter is 0
     * 
     * @param sx the X offset from {@code cx} when the parameter is 1
     * 
     * @param sy the Y offset from {@code cy} when the parameter is 1
     */
    public BasicLine(double cx, double cy, double sx, double sy) {
        this(cx, cy, sx, sy, Double.NEGATIVE_INFINITY,
             Double.POSITIVE_INFINITY);
    }

    @Override
    public BasicLine copy() {
        return new BasicLine(cx, cy, sx, sy, min, max);
    }

    @Override
    public double min() {
        return min;
    }

    @Override
    public double max() {
        return max;
    }

    @Override
    public double x(double t) {
        return cx + sx * t;
    }

    @Override
    public double y(double t) {
        return cy + sy * t;
    }

    @Override
    public void truncateMin(double t) {
        if (t > min) min = t;
    }

    @Override
    public void truncateMax(double t) {
        if (t < max) max = t;
    }

    private Line reverse;

    @Override
    public Line reverse() {
        if (reverse == null)
            reverse = new BasicLine(cx, cy, -sx, -sy, -max, -min, this);
        return reverse;
    }

    @Override
    public Intersection intersect(Line other) {
        if (other instanceof BasicLine) {
            final double osx, osy, ocx, ocy;
            BasicLine blo = (BasicLine) other;
            ocx = blo.cx;
            ocy = blo.cy;
            osx = blo.sx;
            osy = blo.sy;
            return Line.intersect(cx, cy, sx, sy, ocx, ocy, osx, osy);
        } else {
            return Line.super.intersect(other);
        }
    }

    /**
     * Create a line of infinite length passing half-way between two
     * points and perpendicular to the line passing through both. From
     * the first point, the line's parameter increases from right to
     * left (or anti-clockwise).
     * 
     * @param a the first point
     * 
     * @param b the second point
     * 
     * @return the required line
     */
    public static BasicLine between(Point2D a, Point2D b) {
        return new BasicLine((a.getX() + b.getX()) / 2.0,
                             (a.getY() + b.getY()) / 2.0, a.getY() - b.getY(),
                             b.getX() - a.getX());
    }

    /**
     * Create a line starting at one point and ending at another.
     * 
     * @param a the start point
     * 
     * @param b the end point
     * 
     * @return the require line
     */
    public static BasicLine through(Point2D a, Point2D b) {
        return new BasicLine(a.getX(), a.getY(), b.getX() - a.getX(),
                             b.getY() - a.getY(), 0.0, 1.0);
    }

    /**
     * Provide a string representation of this object. This includes the
     * centre position, the direction, and the parameter interval.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return toString(cx, cy, sx, sy, min, max);
    }

    private static String toString(double cx, double cy, double sx, double sy,
                                   double min, double max) {
        if (Double.isInfinite(min) || Double.isInfinite(max)) return String
            .format("(%g,%g)->(%g,%g) [%g,%g]", cx, cy, sx, sy, min, max);
        return String.format("(%g,%g)->(%g,%g)", cx + sx * min, cy + sy * min,
                             cx + sx * max, cy + sy * max);

    }
}
