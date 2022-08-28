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

import java.awt.Point;

/**
 * The grid is assumed to have (0, 0) in the top left, with increments
 * meaning right or down.
 * 
 * @resume A direction in a rectangular grid, in divisions of 45°
 *
 * @author simpsons
 */
public enum Direction {
    /**
     * @resume The positive X direction
     */
    RIGHT(+1, 0),

    /**
     * @resume The positive X and Y direction
     */
    DOWN_RIGHT(+1, +1),

    /**
     * @resume The positive Y direction
     */
    DOWN(0, +1),

    /**
     * @resume The negative X and positive Y direction
     */
    DOWN_LEFT(-1, +1),

    /**
     * @resume The negative X direction
     */
    LEFT(-1, 0),

    /**
     * @resume The negative X and Y direction
     */
    UP_LEFT(-1, -1),

    /**
     * @resume The negative Y direction
     */
    UP(0, -1),

    /**
     * @resume The positive X and negative Y direction
     */
    UP_RIGHT(+1, -1);

    private final int dx, dy;

    private Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Get the change in X co-ordinate representing this direction.
     * 
     * @return the change in X co-ordinate
     */
    public int dx() {
        return dx;
    }

    /**
     * Get the change in Y co-ordinate representing this direction.
     * 
     * @return the change in Y co-ordinate
     */
    public int dy() {
        return dy;
    }

    private static final Direction[] VALUES = values();
    private static final int COUNT = VALUES.length;

    /**
     * Turn clockwise 45° a specific number of times.
     * 
     * @param clockwise the number of times to turn; negatives permitted
     * 
     * @return the new direction after turning
     */
    public Direction turn(int clockwise) {
        clockwise -= (clockwise - (COUNT - 1)) / COUNT * COUNT - COUNT;
        clockwise += this.ordinal();
        clockwise %= COUNT;
        return VALUES[clockwise];
    }

    /**
     * @resume A turn of 90° clockwise
     */
    public static final int RIGHT_TURN_90 = +2;

    /**
     * @resume A turn of 90° anti-clockwise
     */
    public static final int LEFT_TURN_90 = -2;

    /**
     * @resume A turn of 180°
     */
    public static final int ABOUT_TURN = +4;

    /**
     * @resume A turn of 0°
     */
    public static final int NO_TURN = 0;

    /**
     * Move co-ordinates in this direction.
     * 
     * @param p the co-ordinates to modify in-place
     * 
     * @return {@code p}
     */
    public Point move(Point p) {
        p.translate(dx(), dy());
        return p;
    }

    /**
     * Move co-ordinates in this direction by several units at once.
     * 
     * @param p the co-ordinates to modify in-place
     * 
     * @param amount the number of units to translate by
     * 
     * @return {@code p}
     */
    public Point move(Point p, int amount) {
        p.translate(dx() * amount, dy() * amount);
        return p;
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        Direction dir = Direction.valueOf(args[0]);
        int turn = Integer.valueOf(args[1]);
        System.out.println(dir.turn(turn));
    }
}
