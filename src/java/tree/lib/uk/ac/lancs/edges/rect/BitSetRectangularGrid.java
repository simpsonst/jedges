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

/**
 * Cell state is arranged row-by-row. Set bits correspond to cells of
 * colour 1, and clear bits to colour 0. In accordance with the general
 * contract of {@link RectangularGrid}, cells outside the defined area
 * are of colour 0.
 * 
 * @resume A rectangular grid based on a bit set
 *
 * @author simpsons
 */
public class BitSetRectangularGrid implements RectangularGrid {
    private final BitSet data;
    private final int width, height;
    private final int falseColor, trueColor;

    /**
     * Create a grid from a bit set with zero as the false colour and
     * one as the true colour. The raw data is not copied.
     * 
     * @param width the width of the grid in cells
     * 
     * @param height the height of the grid in cells
     * 
     * @param data the state of the grid
     */
    public BitSetRectangularGrid(int width, int height, BitSet data) {
        this(width, height, data, 1);
    }

    /**
     * Create a grid from a bit set with zero as the false colour. The
     * raw data is not copied.
     * 
     * @param width the width of the grid in cells
     * 
     * @param height the height of the grid in cells
     * 
     * @param data the state of the grid
     * 
     * @param trueColor the colour number for cells that appear in the
     * bit set
     */
    public BitSetRectangularGrid(int width, int height, BitSet data,
                                 int trueColor) {
        this(width, height, data, trueColor, 0);
    }

    /**
     * Create a grid from a bit set. The raw data is not copied. Note
     * that cells outside the grid always return 0 as the colour.
     * 
     * @param width the width of the grid in cells
     * 
     * @param height the height of the grid in cells
     * 
     * @param data the state of the grid
     * 
     * @param trueColor the colour number for cells that appear in the
     * bit set
     * 
     * @param falseColor the colour number for cells that do not appear
     * in the bit set
     */
    public BitSetRectangularGrid(int width, int height, BitSet data,
                                 int trueColor, int falseColor) {
        if (width < 0)
            throw new IllegalArgumentException("negative width: " + width);
        if (height < 0)
            throw new IllegalArgumentException("negative height: " + height);
        this.width = width;
        this.height = height;
        this.data = data;
        this.trueColor = trueColor;
        this.falseColor = falseColor;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public int color(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= width) return 0;
        return data.get(x + y * width) ? trueColor : falseColor;
    }

    /**
     * Get a bit set that could be used to represent a grid.
     * 
     * @param grid the input grid
     * 
     * @param reducer a test to determine whether a colour is to be
     * included in the result
     * 
     * @return a bit set that could be used with this class to build an
     * equivalent grid
     */
    public static BitSet getData(RectangularGrid grid, IntPredicate reducer) {
        final int width = grid.width();
        final int height = grid.height();
        BitSet result = new BitSet(width * height);
        for (int y = 0; y < height; y++) {
            final int base = y * width;
            for (int x = 0; x < width; x++)
                result.set(base + x, reducer.test(grid.color(x, y)));
        }
        return result;
    }
}
