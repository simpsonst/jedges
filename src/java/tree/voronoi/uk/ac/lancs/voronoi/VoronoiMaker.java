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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @resume A command to generate Voronoi tessellations from labelled
 * points
 * 
 *
 * @author simpsons
 */
public final class VoronoiMaker {
    /**
     * Create a Voronoi tessellation from a set of labelled points.
     * 
     * <p>
     * This program reads CSV from Standard Input. Each record must be a
     * label, X co-ordinate, Y-co-ordindate.
     * 
     * <p>
     * The output is CSV again, giving label, polygon degree,
     * co-ordinate pairs for the polygon, number of Voronoi cell
     * neighbours, labels for those neighbours.
     * 
     * @param args pairs of co-ordinates defining the boundary
     */
    public static void main(String[] args) throws Exception {
        /* Load the boundary. */
        if (args.length % 2 != 0)
            throw new IllegalArgumentException("Odd number of arguments");
        if (args.length < 6)
            throw new IllegalArgumentException("At least three points "
                + "required for boundary");
        List<Point2D> boundary = new ArrayList<>();
        for (int i = 0; i < args.length; i += 2) {
            final double x = Double.parseDouble(args[i]);
            final double y = Double.parseDouble(args[i + 1]);
            boundary.add(new Point2D.Double(x, y));
        }

        /* Load the points. */
        Collection<Point2D> points = new ArrayList<>();
        try (CSVReader in = new CSVReader(new InputStreamReader(System.in))) {
            for (List<String> row = in.readRow(); row != null; row =
                in.readRow()) {
                String label = row.get(0).trim();
                double x = Double.parseDouble(row.get(1).trim());
                double y = Double.parseDouble(row.get(2).trim());
                @SuppressWarnings("serial")
                Point2D p = new Point2D.Double(x, y) {
                    @Override
                    public String toString() {
                        return label;
                    }
                };
                points.add(p);
            }
        }

        /* Compute cells. */
        Voronoi<Point2D> voronoi = new Voronoi<>(points, p -> p, boundary);
        voronoi.process();

        /* Print out the data. */
        Map<Line, Point2D> assoc = voronoi.getAssociations();
        try (CSVWriter out =
            new CSVWriter(new OutputStreamWriter(System.out))) {
            for (Map.Entry<Point2D, List<Line>> entry : voronoi.getPolygons()
                .entrySet()) {
                List<String> row = new ArrayList<>();
                String label = entry.getKey().toString();
                row.add(label);
                List<Line> sides = entry.getValue();
                List<Point2D> neighbours = new ArrayList<>(sides.size());
                row.add(Integer.toString(sides.size()));
                for (Line side : sides) {
                    /* Record the neighbour if there is one. */
                    Point2D other = assoc.get(side.reverse());
                    if (other != null) neighbours.add(other);

                    /* Append the co=ordinates. */
                    Point2D pos = side.getStart();
                    row.add(Double.toString(pos.getX()));
                    row.add(Double.toString(pos.getY()));
                }
                row.add(Integer.toString(neighbours.size()));
                for (Point2D n : neighbours)
                    row.add(n.toString());
                out.writeRow(row);
            }
        }
    }
}
