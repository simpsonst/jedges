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

import java.io.IOException;

/**
 * Represents a rectangular grid of squares from which a rectangular
 * layout can be derived.
 * 
 * @see RectangularLayout
 *
 * @author simpsons
 */
public interface RectangularGrid {
    /**
     * Get the width of the grid.
     * 
     * @return the grid width in cells
     */
    int width();

    /**
     * Get the height of the grid.
     * 
     * @return the grid height in cells
     */
    int height();

    /**
     * Get the colour of a cell in the grid.
     * 
     * <p>
     * Zero is always considered transparent. The basic
     * {@link RectangularLayout} treats all non-zero values as solid,
     * and therefore part of the polygon to be traced.
     * 
     * <p>
     * X co-ordinates within the grid are in the interval [0, width),
     * and Y in [0, height). Requested co-ordinates may lie outside of
     * the defined grid, in which case, the returned value must be zero.
     * 
     * @param x the X co-ordinate
     * 
     * @param y the Y co-ordinate
     * 
     * @return the cell colour
     */
    int color(int x, int y);

    /**
     * Obtain a subgrid view of this grid.
     * 
     * @param left the X co-ordinate of the top-left cell
     * 
     * @param top the Y co-ordinate of the top-left cell
     * 
     * @param width the width of the view
     * 
     * @param height the height of the view
     * 
     * @return the subgrid view
     */
    default RectangularGrid subgrid(int left, int top, int width,
                                    int height) {
        return new RectangularGrid() {
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
                if (x < 0 || y < 0 || x >= width || y >= height) return 0;
                return RectangularGrid.this.color(x + left, y + top);
            }
        };
    }

    /**
     * Print a representation of this grid. Errors are swallowed, but
     * the first error terminates output.
     * 
     * @param out the destination
     * 
     * @param chars characters to use for each colour. The final
     * character is used for colour numbers beyond the range of the
     * indices of this character sequence.
     */
    default void print(Appendable out, CharSequence chars) {
        try {
            append(out, chars);
        } catch (IOException e) {
            // Swallowed.
        }
    }

    /**
     * Append a representation of this grid.
     * 
     * @param out the destination
     * 
     * @param chars characters to use for each colour. The final
     * character is used for colour numbers beyond the range of the
     * indices of this character sequence.
     * 
     * @throws IOException if an I/O problem occurs
     */
    default void append(Appendable out, CharSequence chars)
        throws IOException {
        String nl = System.getProperty("line.separator");
        final int width = width();
        final int height = height();
        final int len = chars.length();
        final char last = chars.charAt(len - 1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = color(x, y);
                if (c >= 0 && c < len)
                    out.append(chars.charAt(c));
                else
                    out.append(last);
            }
            out.append(nl);
        }
    }
}
