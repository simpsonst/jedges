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

/**
 * The primary class is {@link uk.ac.lancs.edges.Tracer}. It takes a
 * {@link uk.ac.lancs.edges.Layout} which describes a polygon in
 * terms of several <dfn>steps</dfn>, with each straight side of the
 * polygon formed from one or more steps. The {@code Tracer} then finds
 * one or more closed paths made up of adjoining steps that trace parts
 * of the polygon. An even-odd fill rule is assumed and exploited when
 * two vertices of the polygon meet, as the {@code Tracer} prefers to
 * cross through these in a straight line if possible. Co-ordinates of
 * each path are passed to a {@link uk.ac.lancs.edges.Scribe} for
 * rendering.
 * 
 * <p>
 * Multicoloured, interlocking polygons are supported using
 * {@link uk.ac.lancs.edges.BasicSlicer}. Each colour is processed
 * separately, with all colours already processed before it interpreted
 * as transparent, and cells of all colours yet to be processed
 * temporarily interpreted as potential cells of the current colour to
 * produce more optimal paths.
 * 
 * <p>
 * The co-ordinate system is prescribed by the {@code Layout}.
 * {@code Tracer}, {@code Slicer} and {@code Scribe} take the
 * co-ordinate type as a generic parameter, and perform no actually
 * processing on co-ordinates. See the package
 * {@link uk.ac.lancs.edges.rect} for classes supporting rectangular
 * grids.
 * 
 * @resume Tools for tracing around polygons expressed as bitmaps
 *
 * @author simpsons
 */
package uk.ac.lancs.edges;
