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
import java.util.Comparator;

/**
 * Defines a straight line parametrically with adjustable minimum and
 * maximum parameter values.
 *
 * @author simpsons
 */
public interface Line {
    /**
     * Get the minimum parameter value. This identifies the start of the
     * line.
     * 
     * @return the minimum parameter value
     */
    double min();

    /**
     * Get the maximum parameter value. This identifies the end of the
     * line.
     * 
     * @return the maximum parameter value
     */
    double max();

    /**
     * Increase the minimum parameter value.
     * 
     * @param t the new minimum parameter value if it is greater than
     * the current
     */
    void truncateMin(double t);

    /**
     * Decrease the maximum parameter value.
     * 
     * @param t the new maximum parameter value if it is less than the
     * current
     */
    void truncateMax(double t);

    /**
     * Get the X co-ordinate for a given parameter.
     * 
     * @param t the input parameter
     * 
     * @return the X co-ordinate for the given parameter
     */
    double x(double t);

    /**
     * Get the Y co-ordinate for a given parameter.
     * 
     * @param t the input parameter
     * 
     * @return the Y co-ordinate for the given parameter
     */
    double y(double t);

    /**
     * Get the co-ordinates for a given parameter.
     * 
     * @param t the input parameter
     * 
     * @return the co-ordinate pair for the given parameter
     */
    default Point2D get(double t) {
        return new Point2D.Double(x(t), y(t));
    }

    /**
     * Set a co-ordinate pair to that of the line for a given parameter.
     * 
     * @param into the destination pair
     * 
     * @param t the input parameter
     */
    default void set(Point2D into, double t) {
        into.setLocation(x(t), y(t));
    }

    /**
     * Get the co-ordinates of the start position of the line.
     * 
     * @return the line's start position
     */
    default Point2D getStart() {
        return get(min());
    }

    /**
     * Get the co-ordinates of the end position of the line.
     * 
     * @return the line's end position
     */
    default Point2D getEnd() {
        return get(max());
    }

    /**
     * Get the length of this line. This is the distance between its two
     * points.
     * 
     * @return the length of the line
     */
    default double length() {
        return getStart().distance(getEnd());
    }

    /**
     * Get a line going in the opposite direction. The start and end
     * positions are the same, but the direction is opposite.
     * 
     * <p>
     * Calling this method twice yields the same object. Calling this
     * method on the result yields this object.
     * 
     * <pre>
     * assert line.reverse() == line.reverse();
     * assert line.reverse().reverse() == line;
     * </pre>
     * 
     * @return the reverse view of this line
     */
    Line reverse();

    /**
     * Intersect this line with another. Two parameters are returned if
     * they intersect. The first parameter yields the point of
     * intersection for this line, and the second for the other.
     * 
     * @param other the line to intersect with
     * 
     * @return a description of the intersection
     */
    default Intersection intersect(Line other) {
        final double cx, cy, sx, sy;
        cx = x(0.0);
        cy = y(0.0);
        sx = x(1.0) - cx;
        sy = y(1.0) - cy;
        final double ocx, ocy, osx, osy;
        ocx = other.x(0.0);
        ocy = other.y(0.0);
        osx = other.x(1.0) - ocx;
        osy = other.y(1.0) - ocy;
        return Line.intersect(cx, cy, sx, sy, ocx, ocy, osx, osy);
    }

    /**
     * Compute the intersection point of two parametric lines. Two
     * parameters are returned if they intersect. The first parameter
     * yields the point of intersection for this line, and the second
     * for the other.
     * 
     * @param cx0 the X co-ordinate of the first line when the parameter
     * is 0
     * 
     * @param cy0 the Y co-ordinate of the first line when the parameter
     * is 0
     * 
     * @param sx0 the X offset from {@code cx0} of the first line when
     * the parameter is 1
     * 
     * @param sy0 the Y offset from {@code cy0} of the first line when
     * the parameter is 1
     * 
     * @param cx1 the X co-ordinate of the second line when the
     * parameter is 0
     * 
     * @param cy1 the Y co-ordinate of the second line when the
     * parameter is 0
     * 
     * @param sx1 the X offset from {@code cx1} of the second line when
     * the parameter is 1
     * 
     * @param sy1 the Y offset from {@code cy1} of the second line when
     * the parameter is 1
     * 
     * @return a description of the intersection
     */
    public static Intersection intersect(double cx0, double cy0, double sx0,
                                         double sy0, double cx1, double cy1,
                                         double sx1, double sy1) {
        final double denom = sy0 * sx1 - sy1 * sx0;
        if (denom == 0.0) {
            if (sx0 * sx1 + sy0 * sy1 < 0.0)
                return Intersection.ANTIPARALLEL;
            else
                return Intersection.PARALLEL;
        }
        double t0 = (sx1 * (cy0 - cy1) + sy1 * (cx1 - cx0)) / -denom;
        double t1 = (sx0 * (cy1 - cy0) + sy0 * (cx0 - cx1)) / denom;
        return new Intersection(t0, t1);
    }

    /**
     * Compute the angle of this line relative to the positive X axis.
     * 
     * @return an angle in the range -&#960; to +&#960;
     */
    default double xAxisAngle() {
        final double cx, cy, sx, sy;
        cx = x(0.0);
        cy = y(0.0);
        sx = x(1.0) - cx;
        sy = y(1.0) - cy;
        return Math.atan2(sy, sx);
    }

    /**
     * Orders lines by the angle they make with the positive X axis.
     */
    public static final Comparator<Line> BY_INCREASING_POSITIVE_X_AXIS_ANGLE =
        new Comparator<Line>() {
            @Override
            public int compare(Line o1, Line o2) {
                return Double.compare(o1.xAxisAngle(), o2.xAxisAngle());
            }
        };

    /**
     * Compute how much a turn to another line would be. Positive means
     * anti-clockwise.
     * 
     * @param other the new angle to turn to
     * 
     * @return the turn in -&#960; to +&#960;
     */
    default double turn(Line other) {
        double f = other.xAxisAngle() - this.xAxisAngle();
        if (f > +Math.PI) return f - 2 * Math.PI;
        if (f < -Math.PI) return f + 2 * Math.PI;
        return f;
    }

    /**
     * Determine whether the parameter is within the set range.
     * 
     * @param t the parameter to test
     * 
     * @return {@code true} if it is within range
     */
    default boolean contains(double t) {
        return t >= min() && t <= max();
    }

    /**
     * Determine whether the parameter is within the set range.
     * 
     * @param t the parameter to test
     * 
     * @return {@code true} if it is within range
     */
    default boolean containsExclusive(double t) {
        return t > min() && t < max();
    }

    /**
     * Get the length of the line in parameter units from its minimum
     * position to its maximum.
     * 
     * @return the parameter length of the line
     */
    default double parameterLength() {
        return max() - min();
    }

    /**
     * Copy this line.
     * 
     * @return a fresh line
     */
    Line copy();
}
