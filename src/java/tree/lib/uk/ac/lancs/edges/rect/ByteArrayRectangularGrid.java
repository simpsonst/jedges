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

/**
 * Cell state is arranged row-by-row, one byte per cell. Byte values are
 * interpreted as unsigned. In accordance with the general contract of
 * {@link RectangularGrid}, cells outside the defined area are of colour
 * 0.
 * 
 * @resume A rectangular grid based on a byte array
 *
 * @author simpsons
 */
public class ByteArrayRectangularGrid implements RectangularGrid {
    private final int width, height;
    private final byte[] data;
    private final int offset;

    /**
     * Create a grid from an array of bytes, one per cell. The raw data
     * is not copied.
     * 
     * @param width the width of the grid
     * 
     * @param height the height of the grid
     * 
     * @param data the state of the grid
     */
    public ByteArrayRectangularGrid(int width, int height, byte[] data) {
        this(width, height, data, 0);
    }

    /**
     * Create a grid from a portion of an array.
     * 
     * @param width the width of the grid in cells
     * 
     * @param height the height of the grid in cells
     * 
     * @param data the source array
     * 
     * @param offset the offset of the first byte to be used as the
     * value of cell (0, 0)
     */
    public ByteArrayRectangularGrid(int width, int height, byte[] data,
                                    int offset) {
        if (data.length - offset < width * height)
            throw new IllegalArgumentException("bytes provided: "
                + (data.length - offset) + "; bytes required: "
                + width * height);
        this.width = width;
        this.height = height;
        this.data = data;
        this.offset = offset;
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
        if (x < 0 || y < 0 || x >= width || y >= height) return 0;
        return data[offset + x + y * width] & 0xff;
    }
}
