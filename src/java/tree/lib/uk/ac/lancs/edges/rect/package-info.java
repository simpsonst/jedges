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

/**
 * {@link uk.ac.lancs.edges.rect.RectangularLayout} is a convenient
 * implementation of a layout for a rectangular grid of square cells.
 * Other geometries, e.g., triangular, isometric, hexagonal, even
 * irregular, are possible simply by providing a suitable implementation
 * of {@code Layout}. Given a two-colour
 * {@link uk.ac.lancs.edges.rect.RectangularGrid}, one can trace the
 * shape formed by cells of the non-zero colour with an idiom such as:
 * 
 * <pre>
 * static void trace(RectangularGrid grid, Scribe&lt;Point&gt; scribe) {
 *     Layout&lt;Point&gt; layout = new RectangularLayout(grid);
 *     new Tracer&lt;&gt;(layout, scribe).walkThrough();
 * }
 * </pre>
 * 
 * <p>
 * The co-ordinates provided to the scribe take the form of a
 * {@link java.awt.Point}.
 * 
 * <p>
 * Multicoloured tracing is more involved, requiring one tracing step
 * per colour. The following shows a minimal idiom:
 * 
 * <pre>
 * Slicer&lt;RectangularGrid, Point&gt; rectangularSlicer =
 *     new BasicSlicer<>(RectangularColorCollector.INSTANCE,
 *                       ArbitraryColorSelector.INSTANCE,
 *                       MinimalRectangularColorOptimizer.INSTANCE,
 *                       RectangularLayoutFactory.INSTANCE);
 * 
 * Collection&lt;Tracer&lt;Point>> tracers = new ArrayList<>();
 * List&lt;MyScribe> scribes = new ArrayList<>();
 * rectangularSlicer.slice(grid, c -> new MyScribe(c),
 *                         tracers, scribes);
 * </pre>
 * 
 * <p>
 * The
 * {@link uk.ac.lancs.edges.Slicer#slice(Object, java.util.function.IntFunction, Collection, java.util.List)}
 * call fills the two provided containers with a {@code Tracer} for each
 * colour, and a user-supplied scribe for each colour. The scribes are
 * in the order that they should be rendered.
 * 
 * @resume Classes for tracing rectangular grids
 */
package uk.ac.lancs.edges.rect;

import java.util.Collection;
