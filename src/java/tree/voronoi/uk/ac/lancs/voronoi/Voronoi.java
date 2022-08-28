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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

/**
 * Computes the co-ordinates of Voronoi cells around a given set of
 * points in a convex polygonal perimeter.
 * 
 * <p>
 * This implementation works for each node by scanning each other node
 * in order of increasing distance from the primary node, and forming a
 * perpendicular line between them. Each line is then retained by the
 * node if it might truncate the two lines adjacent to it by angle.
 * 
 * <p>
 * Special cases are made of edge nodes, whose cells either have
 * infinite area (and must be truncated by a user-supplied perimeter),
 * or jut out beyond the perimeter (and must be similarly truncated).
 * 
 * @param <Node> The node type, from which a co-ordinate pair can be
 * derived
 *
 * @author simpsons
 */
public class Voronoi<Node> {
    private final Collection<? extends Node> nodes;
    private final List<? extends Point2D> boundary;
    private final Function<? super Node, ? extends Point2D> locations;

    /**
     * Set up the inputs for generating a Voronoi diagram. The next step
     * is usually to call {@link #process()} to compute and store the
     * result, and then {@link #getPolygons()} and
     * {@link #getAssociations()} to retrieve it.
     * 
     * @param nodes the set of nodes that will define the areas of the
     * diagram
     * 
     * @param locations a mapping from node to location
     * 
     * @param boundary the corners of a bounding polygon (the perimeter)
     */
    public Voronoi(Collection<? extends Node> nodes,
                   Function<? super Node, ? extends Point2D> locations,
                   List<? extends Point2D> boundary) {
        this.nodes = nodes;
        this.locations = locations;
        this.boundary = boundary;
    }

    /**
     * Get the last element of a list.
     * 
     * @param list the list whose last element is sought
     * 
     * @return the last element of the list
     * 
     * @throws IndexOutOfBoundsException if the list is empty
     */
    private static <T> T getLast(List<? extends T> list) {
        return list.get(list.size() - 1);
    }

    /**
     * Convert a node to its co-ordinates.
     * 
     * @param node the node whose co-ordinates are sought
     * 
     * @return the node's co-ordinates
     */
    private Point2D get(Node node) {
        return locations.apply(node);
    }

    /**
     * Tracks which node a line belongs to, if any. Lines along the
     * perimeter do not have this mapping.
     */
    private Map<Line, Node> associations;

    /**
     * Get the mapping from any line to the node it surrounds, as
     * determined from the last call to {@link #process()}.
     * 
     * @return the associations from the last call
     */
    public Map<Line, Node> getAssociations() {
        return associations;
    }

    /**
     * Tracks lines already created between nodes.
     */
    private Map<Node, List<Line>> polygons;

    /**
     * Get the polygons from the last call to {@link #process()}.
     * 
     * @return the polygons from the last call
     */
    public Map<Node, List<Line>> getPolygons() {
        return polygons;
    }

    /**
     * Make a polygon (a sequence of adjoining lines) from a its
     * corners.
     * 
     * @param corners the corners of the polygon
     * 
     * @return a sequence of lines forming the polygon
     */
    private static List<Line> makePolygon(List<? extends Point2D> corners) {
        List<Line> perimeter = new ArrayList<>(corners.size());
        Point2D last = getLast(corners);
        for (Point2D cur : corners) {
            perimeter.add(BasicLine.through(last, cur));
            last = cur;
        }
        return perimeter;
    }

    /**
     * This function still does not create a Delaunay triangulation.
     * 
     * @param nodes the input nodes
     * 
     * @param locations a mapping from node to its location
     * 
     * @return the neighbours of each node in the resultant graph
     */
    private static <N> Map<N, List<N>>
        getDelaunayNeighbours(Collection<? extends N> nodes,
                              Function<? super N, ? extends Point2D> locations) {
        /* Create edges between all nodes. */
        Collection<Line> eligible = newIdentitySet();
        Map<N, Collection<Line>> edgeIndex = new IdentityHashMap<>();
        Map<Line, N> starts = new IdentityHashMap<>();
        List<Line> unshorts = new ArrayList<>();
        {
            List<? extends N> nodeList = nodes instanceof List
                ? (List<? extends N>) nodes : new ArrayList<>(nodes);
            final int size = nodeList.size();
            for (int i = 0; i < size - 1; i++) {
                N a = nodeList.get(i);
                Point2D aLoc = locations.apply(a);
                for (int j = i + 1; j < size; j++) {
                    N b = nodeList.get(j);
                    Point2D bLoc = locations.apply(b);

                    /* Create an edge between these two points. */
                    Line aToB = BasicLine.through(aLoc, bLoc);
                    Line bToA = aToB.reverse();

                    /* Maintain the mapping from each node to all its
                     * edges. */
                    edgeIndex.computeIfAbsent(a, k -> newIdentitySet())
                        .add(aToB);
                    edgeIndex.computeIfAbsent(b, k -> newIdentitySet())
                        .add(bToA);

                    /* Maintain the mapping from line to node. */
                    starts.put(aToB, a);
                    starts.put(bToA, b);

                    /* Add both lines as candidates for the minimum
                     * spanning tree. */
                    eligible.add(aToB);
                    eligible.add(bToA);

                    /* Retain only one of each pair for completing the
                     * triangulation. */
                    unshorts.add(aToB);
                }
            }
        }

        /* Cache computed distances. */
        Map<Line, Double> distances = new IdentityHashMap<>();
        for (Line line : unshorts) {
            double distance = line.length();
            distances.put(line, distance);
            distances.put(line.reverse(), distance);
        }

        /* Keep track of edges we retain for the result. */
        Collection<Line> retained = newIdentitySet();

        /* Keep track of edges that are candidates in the minimum
         * spanning tree. */
        SortedSet<Line> candidates = new TreeSet<>((a, b) -> Double
            .compare(distances.get(a), distances.get(b)));

        /* Pick one node arbitrarily. */
        N node = nodes.iterator().next();

        /* Form the minimum spanning tree. */
        do {
            /* Identify all edges leading from the most recently added
             * node. */
            Collection<Line> newCands =
                edgeIndex.get(node).stream().filter(k -> eligible.contains(k))
                    .collect(Collectors.toSet());

            /* The identified edges become new candidates, and won't be
             * picked again. */
            candidates.addAll(newCands);
            eligible.removeAll(newCands);

            /* Their duals must be eliminated from both current and
             * future sets to prevent loops. */
            Collection<Line> loopers = newCands.stream().map(Line::reverse)
                .collect(Collectors.toSet());
            eligible.removeAll(loopers);
            candidates.removeAll(loopers);

            /* Choose the shortest candidate. */
            Line chosen = candidates.first();

            /* Add the shortest candidate to the result. */
            retained.add(chosen);
            candidates.remove(chosen);

            /* Identify the added node, so that its other edges can
             * become candidates. */
            Line returning = chosen.reverse();
            node = starts.get(returning);
        } while (retained.size() + 1 < nodes.size());

        /* Remove elements of the minimum spanning tree and their duals
         * from the remaining candidates. */
        unshorts.removeAll(retained);
        unshorts.removeAll(retained.stream().map(Line::reverse)
            .collect(Collectors.toSet()));

        /* Sort the remaining candidates by length. */
        Collections.sort(unshorts, (a, b) -> Double
            .compare(distances.get(a), distances.get(b)));

        /* Add each candidate if it does not intersect with a line
         * already retained. */
        next_candidate:
        for (Line line : unshorts) {
            N lineFrom = starts.get(line);
            N lineTo = starts.get(line.reverse());
            for (Line already : retained) {
                N alreadyFrom = starts.get(already);
                if (alreadyFrom == lineFrom || alreadyFrom == lineTo)
                    continue;
                N alreadyTo = starts.get(already.reverse());
                if (alreadyTo == lineFrom || alreadyTo == lineTo) continue;
                Intersection x = line.intersect(already);
                if (!x.exists()) continue;
                if (!line.contains(x.t0())) continue;
                if (!already.contains(x.t1())) continue;
                continue next_candidate;
            }

            retained.add(line);
        }

        /* Map each node to the set of lines radiating into it. Each set
         * is ordered by bearing. */
        Map<N, SortedSet<Line>> spokes = new IdentityHashMap<>();
        Comparator<Line> byAngle = Line.BY_INCREASING_POSITIVE_X_AXIS_ANGLE;
        for (Line line : retained) {
            spokes
                .computeIfAbsent(starts.get(line),
                                 k -> new TreeSet<>(byAngle))
                .add(line.reverse());
            spokes.computeIfAbsent(starts.get(line.reverse()),
                                   k -> new TreeSet<>(byAngle))
                .add(line);
        }

        /* Convert the map destination to a list of neighbour nodes in
         * orbit about the key node. */
        Map<N, List<N>> neighbours = new IdentityHashMap<>();
        for (Map.Entry<N, SortedSet<Line>> entry : spokes.entrySet()) {
            neighbours.put(entry.getKey(), entry.getValue().stream()
                .map(starts::get).collect(Collectors.toList()));
        }
        return neighbours;
    }

    /**
     * This is intended to generate a Delaunay triangulation from a set
     * of points, from which a Voronoi tessellation can be generated.
     * However, it doesn't work; try with 10 as the seed and 5 points.
     * 
     * @param nodes the input set of nodes
     * 
     * @param locations a mapping from node to location
     * 
     * @return a mapping from each node to a sequence of neighbour nodes
     * in the resulting triangulation
     */
    @SuppressWarnings("unused")
    private static <N> Map<N, List<N>>
        getNeighbours(Collection<? extends N> nodes,
                      Function<? super N, ? extends Point2D> locations) {
        /* Create a line between every pair of nodes, remembering which
         * nodes generated it. Sort them by increasing distance. */
        class Edge implements Comparable<Edge> {
            final double distance;
            final N a, b;
            final Line line;

            Edge(N a, N b) {
                this.a = a;
                this.b = b;
                line =
                    BasicLine.through(locations.apply(a), locations.apply(b));
                this.distance = line.length();
            }

            @Override
            public int compareTo(Edge o) {
                return Double.compare(this.distance, o.distance);
            }
        }
        List<Edge> edges =
            new ArrayList<>((nodes.size() * nodes.size() - nodes.size()) / 2);
        List<? extends N> nodeList = nodes instanceof List
            ? (List<? extends N>) nodes : new ArrayList<>(nodes);
        final int size = nodeList.size();
        for (int i = 0; i < size - 1; i++)
            for (int j = i + 1; j < size; j++)
                edges.add(new Edge(nodeList.get(i), nodeList.get(j)));
        Collections.sort(edges);

        /* Starting with the shortest line, retain a line only if it
         * does not cross an already retained line. */
        Collection<Edge> retained = newIdentitySet();
        next_candidate:
        for (Edge edge : edges) {
            for (Edge already : retained) {
                /* Allow edges that share a common node. TODO: Disallow
                 * some parallel lines? */
                if (edge.a == already.a || edge.b == already.b
                    || edge.a == already.b || edge.b == already.a) continue;

                /* Do these edges' lines intersect? */
                Intersection x = edge.line.intersect(already.line);
                if (!x.exists()) continue;
                if (!edge.line.contains(x.t0())) continue;
                if (!already.line.contains(x.t1())) continue;
                continue next_candidate;
            }

            /* This edge's line does not intersect with any already
             * retained, so keep it. */
            retained.add(edge);
        }

        /* Now map each node to the set of lines radiating from it. Each
         * set is ordered by bearing. */
        Map<N, SortedSet<Line>> spokes = new IdentityHashMap<>();
        Comparator<Line> byAngle = Line.BY_INCREASING_POSITIVE_X_AXIS_ANGLE;
        for (Edge edge : retained) {
            spokes.computeIfAbsent(edge.a, k -> new TreeSet<>(byAngle))
                .add(edge.line);
            spokes.computeIfAbsent(edge.b, k -> new TreeSet<>(byAngle))
                .add(edge.line.reverse());
        }

        /* Keep a mapping from lines to their end points. */
        Map<Line, N> endPoints = new IdentityHashMap<>();
        for (Edge edge : retained) {
            endPoints.put(edge.line, edge.b);
            endPoints.put(edge.line.reverse(), edge.a);
        }

        /* Convert the map destination to a list of neighbour nodes in
         * orbit about the key node. */
        Map<N, List<N>> neighbours = new IdentityHashMap<>();
        for (Map.Entry<N, SortedSet<Line>> entry : spokes.entrySet()) {
            neighbours.put(entry.getKey(), entry.getValue().stream()
                .map(endPoints::get).collect(Collectors.toList()));
        }
        return neighbours;
    }

    private static <E> Set<E> newIdentitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static <N> Map<N, List<Line>>
        buildSidesFromNeighbours(Map<N, List<N>> neighbours,
                                 Function<? super N, ? extends Point2D> locations) {
        Map<N, Map<N, Line>> foo = new IdentityHashMap<>();
        Map<N, List<Line>> sidesPerNode = new IdentityHashMap<>();
        for (Map.Entry<N, List<N>> entry : neighbours.entrySet()) {
            N base = entry.getKey();
            Point2D baseLoc = locations.apply(base);
            List<N> points = entry.getValue();
            List<Line> sides = new ArrayList<>(points.size());
            sidesPerNode.put(base, sides);
            for (N other : points) {
                /* See if we've already created this line. */
                Map<N, Line> sub = foo.get(base);
                Line line = sub == null ? null : sub.get(other);
                if (line == null) {
                    /* Create a line half-way between the two nodes, and
                     * perpendicular to the line between them. */
                    line = BasicLine.between(baseLoc, locations.apply(other));

                    /* Store its reverse for later. */
                    Line dual = line.reverse();
                    foo.computeIfAbsent(other, k -> new IdentityHashMap<>())
                        .put(base, dual);
                }

                /* Add the found or created line to the sequence. */
                sides.add(line);
            }
        }
        return sidesPerNode;
    }

    /**
     * @undocumented
     */
    public void process2() {
        /* Initialize containers for output. */
        polygons = new WeakHashMap<>();
        associations = new WeakHashMap<>();

        /* Create lines describing the boundary. */
        final int boundarySize = boundary.size();
        List<Line> perimeter = makePolygon(boundary);

        /* Check that the boundary makes sense. */
        if (!isAnticlockwise(perimeter))
            throw new IllegalStateException("Boundary is not convex");

        /* For each node, get a list of neighbour nodes in a Delaunay
         * mesh. */
        Map<Node, List<Node>> neighbours =
            getDelaunayNeighbours(nodes, locations);

        /* For each node, build a basic Voronoi shell around it. */
        Map<Node, List<Line>> sidesPerNode =
            buildSidesFromNeighbours(neighbours, locations);

        /* Fix and complete the polygons around each node. */
        for (Map.Entry<Node, List<Line>> entry : sidesPerNode.entrySet()) {
            Node node0 = entry.getKey();
            List<Line> sides0 = entry.getValue();

            /* Put the sides into a list, inserting extra lines around
             * the boundary for gaps. */
            List<Line> sides1 = new ArrayList<>(sides0.size() + boundarySize);
            completePolygon(sides1, sides0, perimeter, associations, node0);

            /* Intersect each side with the next, and truncate both. */
            truncateSides(sides1);

            /* Look for lines with non-positive length. */
            eliminateNegativeSides(sides1);

            /* Look for lines breaking out of the bounding box, and
             * truncate them. */
            truncateToPerimeter(sides1, perimeter, associations, node0);

            /* Store the result. */
            polygons.put(node0, sides1);
        }
    }

    /**
     * Look for lines in a cycle that have negative parameter length,
     * and eliminate them. When a line is removed, the two adjacent
     * lines are truncated where they intersect, and the cycle is
     * re-scanned.
     * 
     * @param sides a sequence of lines, the last being adjacent to the
     * first
     */
    private static void eliminateNegativeSides(List<? extends Line> sides) {
        try_again:
        while (true) {
            for (int i = 0; i < sides.size(); i++) {
                /* Don't touch this side if it is convex. */
                Line cur = sides.get(i);
                if (cur.parameterLength() > 0.0) continue;

                /* This side forms a loop. Remove it, and get the
                 * previous and next sides to intersect. */
                Line prev = sides.get((i + sides.size() - 1) % sides.size());
                Line next = sides.get((i + sides.size() + 1) % sides.size());
                Intersection x = prev.intersect(next);
                prev.truncateMax(x.t0());
                next.truncateMin(x.t1());
                sides.remove(i);
                continue try_again;
            }
            break;
        }
    }

    /**
     * Truncate a polygon if it escapes beyond a perimeter.
     * 
     * @param sides a sequence of lines, the last being adjacent to the
     * first
     * 
     * @param perimeter a sequence of lines, the last being adjacent to
     * the first
     */
    private static <N> void truncateToPerimeter(List<Line> sides,
                                                List<? extends Line> perimeter,
                                                Map<? super Line, ? super N> associations,
                                                N node) {
        next_edge:
        for (ListIterator<Line> iter = sides.listIterator(); iter
            .hasNext();) {
            final Line outEdge = iter.next();

            /* Find out if this edge exits through the boundary. */
            final int outIndex;
            found_exit:
            do {
                for (ListIterator<? extends Line> periIter =
                    perimeter.listIterator(); periIter.hasNext();) {
                    int index = periIter.nextIndex();
                    Line side = periIter.next();

                    /* Detect whether the edge crosses the border here
                     * and turns left. */
                    Intersection x = outEdge.intersect(side);
                    if (!x.exists()) continue;
                    if (!outEdge.containsExclusive(x.t0())) continue;
                    if (!side.contains(x.t1())) continue;
                    if (outEdge.turn(side) < 0.0) continue;

                    /* Record this as the start position. */
                    outIndex = index;
                    break found_exit;
                }

                /* We failed to find an exit, so the polygon is already
                 * correct. */
                continue next_edge;
            } while (false);

            /* Look for the next edge that crosses the boundary back in. */
            final Line inEdge;
            final Intersection inx;
            final int inIndex;
            found_entrance:
            while (true) {
                final Line cand = loopNext(iter);
                assert cand != outEdge;
                for (ListIterator<? extends Line> periIter =
                    perimeter.listIterator(); periIter.hasNext();) {
                    int index = periIter.nextIndex();
                    Line side = periIter.next();

                    /* Detect whether the edge crosses the border here
                     * turning right. */
                    Intersection x = cand.intersect(side);
                    if (!x.exists()) continue;
                    if (!cand.containsExclusive(x.t0())) continue;
                    if (!side.contains(x.t1())) continue;
                    if (cand.turn(side) > 0.0) continue;

                    /* Record this as the end position. */
                    inIndex = index;
                    inEdge = cand;
                    inx = x;

                    break found_entrance;
                }

                /* This edge is totally outside the perimeter, so delete
                 * it, and look for the next one. */
                iter.remove();
            }

            /* The iterator now points just after the entering edge.
             * Move it back one so we can add new edges along the
             * perimeter. */
            iter.previous();

            /* Now add extra lines from outIndex to inIndex. */
            final int boundarySize = perimeter.size();
            final int nSides =
                1 + (inIndex + 4 + boundarySize - outIndex) % boundarySize;
            Line lastEdge = outEdge;
            for (int i = 0; i < nSides; i++) {
                /* Create a copy of the */
                int sideNo = (i + outIndex) % boundarySize;
                Line side = perimeter.get(sideNo).copy();
                associations.put(side, node);

                Intersection x = lastEdge.intersect(side);
                lastEdge.truncateMax(x.t0());
                side.truncateMin(x.t1());

                iter.add(side);
                lastEdge = side;
            }

            /* Truncate the last added edge against the entering edge. */
            inEdge.truncateMin(inx.t0());
            lastEdge.truncateMax(inx.t1());

            /* We're about to continue from the entering edge, which we
             * should probably do in case it goes straight out again
             * (though there's good reason to not expect that). */
        }

    }

    /**
     * Add sides to a convex polygon to complete it. Polygons that are
     * already complete (i.e., have finite area) will have no extra
     * elements. Polygons with open sections, i.e., where the turn from
     * one side to the next is greater than 180&deg;, are delimited by a
     * supplied perimeter.
     * 
     * @param output the collection into which the complete set of sides
     * will be added
     * 
     * @param input the sequence of sides already known to make the
     * polygon
     * 
     * @param perimeter the perimeter used to truncate open polygons
     */
    private static <N> void completePolygon(Collection<? super Line> output,
                                            List<? extends Line> input,
                                            List<? extends Line> perimeter,
                                            Map<? super Line, ? super N> associations,
                                            N node) {
        completePolygon(output, input, getLast(input), perimeter,
                        associations, node);
    }

    /**
     * Add sides to a convex polygon to complete it. Polygons that are
     * already complete (i.e., have finite area) will have no extra
     * elements. Polygons with open sections, i.e., where the turn from
     * one side to the next is greater than 180&deg;, are delimited by a
     * supplied perimeter.
     * 
     * @param output the collection into which the complete set of sides
     * will be added
     * 
     * @param input the ordered set of sides already known to make the
     * polygon
     * 
     * @param perimeter the perimeter used to truncate open polygons
     */
    private static <N> void completePolygon(Collection<? super Line> output,
                                            SortedSet<? extends Line> input,
                                            List<? extends Line> perimeter,
                                            Map<? super Line, ? super N> associations,
                                            N node) {
        completePolygon(output, input, input.last(), perimeter, associations,
                        node);
    }

    /**
     * Add sides to a convex polygon to complete it. Polygons that are
     * already complete (i.e., have finite area) will have no extra
     * elements. Polygons with open sections, i.e., where the turn from
     * one side to the next is greater than 180&deg;, are delimited by a
     * supplied perimeter.
     * 
     * @param output the collection into which the complete set of sides
     * will be added
     * 
     * @param input the collection of sides already known to make the
     * polygon, whose iteration yields the elements in order
     * 
     * @param prev the last element in the input
     * 
     * @param perimeter the perimeter used to truncate open polygons
     */
    private static <N> void
        completePolygon(Collection<? super Line> output,
                        Collection<? extends Line> input, Line prev,
                        List<? extends Line> perimeter,
                        Map<? super Line, ? super N> associations, N node) {
        final int boundarySize = perimeter.size();
        for (Line cur : input) {
            final double turn = prev.turn(cur);
            assert cur == prev || turn != 0.0;
            if (turn <= 0.0) {
                /* We found a gap. Identify the boundary lines it exits
                 * and re-enters through. */
                int inIndex = -1, outIndex = -1;
                {
                    Line out = prev;
                    Line in = cur;
                    double inBest = Double.NEGATIVE_INFINITY;
                    double outBest = Double.POSITIVE_INFINITY;
                    for (int i = 0; i < boundarySize; i++) {
                        Line side = perimeter.get(i);
                        Intersection inx = in.intersect(side);
                        if (inx.exists() && side.contains(inx.t1())
                            && (inIndex < 0 || inx.t0() < inBest)) {
                            inIndex = i;
                            inBest = inx.t0();
                        }
                        Intersection outx = out.intersect(side);
                        if (outx.exists() && side.contains(outx.t1())
                            && (outIndex < 0 || outx.t0() > outBest)) {
                            outIndex = i;
                            outBest = outx.t0();
                        }
                    }
                }

                /* Now add copies of the boundaries truncated to the
                 * right sizes. */
                final int nSides = 1
                    + (inIndex + 4 + boundarySize - outIndex) % boundarySize;
                for (int i = 0; i < nSides; i++) {
                    int sideNo = (i + outIndex) % boundarySize;
                    Line side = perimeter.get(sideNo).copy();
                    associations.put(side, node);

                    /* Retain this new side. */
                    output.add(side);

                    prev = side;
                }
            }

            /* Add this side to the list. */
            output.add(cur);
            prev = cur;
        }

    }

    /**
     * Go through a cyclic list of lines, and truncate each adjacent
     * pair of lines where they intersect.
     * 
     * @param sides a sequence of lines, the last being adjacent to the
     * first
     */
    private static void truncateSides(List<? extends Line> sides) {
        Line prev = getLast(sides);
        for (Line cur : sides) {
            Intersection x = prev.intersect(cur);
            prev.truncateMax(x.t0());
            cur.truncateMin(x.t1());
            prev = cur;
        }
    }

    /**
     * Check whether a polygon is convex and anticlockwise.
     * 
     * @param polygon the lines that form the polygon
     * 
     * @return true if all turns are to anticlockwise
     */
    private static boolean isAnticlockwise(List<? extends Line> polygon) {
        Line prev = getLast(polygon);
        for (Line cur : polygon) {
            final double turn = prev.turn(cur);
            if (turn <= 0.0) return false;
            prev = cur;
        }
        return true;
    }

    /**
     * Process the current set of points in the boundary, and generate
     * Voronoi polygons around each one.
     */
    public void process() {
        /* Initialize containers for output. */
        polygons = new WeakHashMap<>();
        associations = new WeakHashMap<>();

        if (nodes.isEmpty()) return;

        /* Create lines describing the boundary. */
        final int boundarySize = boundary.size();
        List<Line> perimeter = makePolygon(boundary);

        /* Check that the boundary makes sense. */
        if (!isAnticlockwise(perimeter))
            throw new IllegalStateException("Boundary is not convex");

        /* TODO: Check that all nodes are within the boundary, or throw
         * an exception. */

        /* TODO: Check that all points are distinct. */

        /* Keep track of lines and their duals. */
        Map<Node, Map<Node, Line>> lineIndex = new IdentityHashMap<>();

        /* Find lines around each node. */
        for (Node node0 : nodes) {
            Point2D pt0 = get(node0);

            /* Get a list of all other nodes in increasing order of
             * distance from this one. */
            Comparator<Node> byDistance = (a, b) -> Double
                .compare(pt0.distance(get(a)), pt0.distance(get(b)));
            List<Node> nearest = nodes.stream().filter(e -> e != node0)
                .sorted(byDistance).collect(Collectors.toList());

            /* Create somewhere to store this part of the result. */
            NavigableSet<Line> sides0 =
                new TreeSet<>(Line.BY_INCREASING_POSITIVE_X_AXIS_ANGLE);

            Map<Node, Line> secondIndex = lineIndex
                .computeIfAbsent(node0, k -> new IdentityHashMap<>());

            /* Consider every other node in increasing order of
             * distance. */
            for (Node node1 : nearest) {
                /* Get the line separating these two nodes. Create it if
                 * necessary, storing its dual in the opposite map
                 * navigation. */
                Point2D pt1 = get(node1);
                Line line = secondIndex.computeIfAbsent(node1, k -> {
                    Line r = BasicLine.between(pt0, pt1);
                    associations.put(r, node0);
                    Line opp = r.reverse();
                    associations.put(opp, node1);
                    Line old = lineIndex
                        .computeIfAbsent(node1, k2 -> new IdentityHashMap<>())
                        .put(node0, opp);
                    assert old == null;
                    return r;
                });

                /* We always add the first side. */
                if (sides0.isEmpty()) {
                    sides0.add(line);
                    continue;
                }

                /* Identify the side immediately before this one in
                 * terms of relative angle. */
                SortedSet<Line> prevs = sides0.headSet(line);
                final int pos = prevs.size();
                final Line prev = pos == 0 ? sides0.last() : prevs.last();

                /* Lines parallel to existing lines should be discarded. */
                Intersection prevInLine = prev.intersect(line);
                if (prevInLine.parallel()) {
                    continue;
                }

                /* Identify the side immediately after this one in terms
                 * of relative angle. */
                Line next = sides0.ceiling(line);
                if (next == null) next = sides0.first();

                /* If the previous and next sides are the same, there is
                 * only one side, and we must add the new one to it. */
                if (prev == next) {
                    assert sides0.size() == 1;
                    sides0.add(line);

                    /* Note: If we want to keep track of gaps (to detect
                     * when we can truncate the search, which might not
                     * be possible in general), we need to check if the
                     * new line is antiparallel to the existing one. If
                     * so, a new gap is created. */
                    continue;
                }

                /* Lines parallel to existing lines should be discarded. */
                Intersection lineInNext = line.intersect(next);
                if (lineInNext.parallel()) continue;

                /* See whether the existing lines turn in the right
                 * direction. */
                final double turn = prev.turn(next);
                if (turn > -Math.PI && turn <= 0.0) {
                    /* There is a gap between these two lines (they
                     * might even be the same one), so we must add the
                     * new line. */
                    sides0.add(line);
                    continue;
                }

                if (prevInLine.t1() >= lineInNext.t0()) {
                    /* This line doesn't cut off any existing lines. */
                    continue;
                }

                /* This cuts a corner of the polygon, so we should keep
                 * it. */
                sides0.add(line);
            }

            /* Put the sides into a list, inserting extra lines around
             * the boundary for gaps. */
            List<Line> sides1 = new ArrayList<>(sides0.size() + boundarySize);
            completePolygon(sides1, sides0, perimeter, associations, node0);

            /* Intersect each side with the next, and truncate both. */
            truncateSides(sides1);

            /* Look for lines with non-positive length. */
            eliminateNegativeSides(sides1);

            /* Look for lines breaking out of the bounding box, and
             * truncate them. */
            truncateToPerimeter(sides1, perimeter, associations, node0);

            /* Store the result. */
            polygons.put(node0, sides1);
        }
    }

    /**
     * Cyclicly iterate forwards over a list. If the cursor is at the
     * end of the list, move it to the start.
     * 
     * @param iter the iterator to modify
     * 
     * @return the next element in the list
     */
    private static <E> E loopNext(ListIterator<E> iter) {
        if (!iter.hasNext()) while (iter.hasPrevious())
            iter.previous();

        return iter.next();
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        /* Create a not-necessarily-rectangular bounding area. */
        List<Point2D> boundary =
            Arrays.asList(new Point2D.Double(-100, -100),
                          new Point2D.Double(+1100, -100),
                          new Point2D.Double(+1100, +1100),
                          new Point2D.Double(-100, +1100));

        /* Create some random points. */
        Collection<Point2D> points = new ArrayList<>();
        Random rng = new Random(10);
        String names = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz" + "0123456789";
        for (int i = 0; i < Integer.parseInt(args[0]); i++) {
            final int pos = i % names.length();
            final String name = names.substring(pos, pos + 1);
            @SuppressWarnings("serial")
            Point2D p = new Point2D.Double(rng.nextDouble() * 1000,
                                           rng.nextDouble() * 1000) {
                public String toString() {
                    return name;
                }
            };
            points.add(p);
            System.err.printf("%s: %g,%g%n", p, p.getX(), p.getY());
        }

        Map<Point2D, List<Point2D>> neighbours =
            getDelaunayNeighbours(points, p -> p);

        Voronoi<Point2D> voronoi = new Voronoi<>(points, p -> p, boundary);
        voronoi.process();

        /* Work out the rectangular bounding box for the whole system. */
        Rectangle2D boundingBox = new Rectangle2D.Double();
        for (Point2D p : boundary)
            boundingBox.add(p);

        /* Fatten this box so we can see overflows. */
        boundingBox.add(new Point2D.Double(boundingBox.getMaxX() + 50,
                                           boundingBox.getMaxY() + 50));
        boundingBox.add(new Point2D.Double(boundingBox.getMinX() - 50,
                                           boundingBox.getMinY() - 50));

        /* Create a blank XML document. */
        final Document doc;
        {
            DocumentBuilderFactory docBuildFac =
                DocumentBuilderFactory.newInstance();
            docBuildFac.setNamespaceAware(true);
            DocumentBuilder docBuild = docBuildFac.newDocumentBuilder();
            DOMImplementation impl = docBuild.getDOMImplementation();
            DocumentType doctype =
                impl.createDocumentType("svg", SVG_PUBLIC_ID, SVG_SYSTEM_ID);
            doc = impl.createDocument(SVG_NS_URI, "svg", doctype);
        }

        /* Create an SVG shell. */
        Element root = doc.getDocumentElement();
        root.setAttribute("viewBox",
                          String.format("%g %g %g %g", boundingBox.getX(),
                                        -boundingBox.getY()
                                            - boundingBox.getHeight(),
                                        boundingBox.getWidth(),
                                        boundingBox.getHeight()));

        /* Add a path for each area. */
        for (Map.Entry<Point2D, List<Line>> entry : voronoi.getPolygons()
            .entrySet()) {
            /* Choose a pale colour randomly. */
            int color = 0;
            for (int i = 0; i < 3; i++) {
                color <<= 8;
                color |= 128 + rng.nextInt(80);
            }

            /* Draw the area in the pale color. */
            List<Line> polygon = entry.getValue();
            Element area = doc.createElementNS(SVG_NS_URI, "path");
            root.appendChild(area);
            area.setAttribute("style", String
                .format("stroke: none; fill: #%06x", color & 0xffffff));
            area.setAttribute("d",
                              pathOf(polygon.stream()
                                  .map(line -> line.getStart())
                                  .collect(Collectors.toList())));
        }

        /* Draw Delaunay mesh. */
        for (Map.Entry<Point2D, List<Point2D>> entry : neighbours
            .entrySet()) {
            Point2D from = entry.getKey();
            for (Point2D to : entry.getValue()) {
                Element dot = doc.createElementNS(SVG_NS_URI, "line");
                root.appendChild(dot);
                dot.setAttribute("style", "stroke-width: 2; "
                    + "stroke: black; " + "fill: none");
                dot.setAttribute("x1", String.format("%g", from.getX()));
                dot.setAttribute("y1", String.format("%g", -from.getY()));
                dot.setAttribute("x2", String.format("%g", to.getX()));
                dot.setAttribute("y2", String.format("%g", -to.getY()));
            }
        }

        /* Draw nodes as black dots. */
        for (Point2D centroid : points) {
            Element dot = doc.createElementNS(SVG_NS_URI, "circle");
            root.appendChild(dot);
            dot.setAttribute("style", "stroke: none; fill: black");
            dot.setAttribute("cx", String.format("%g", centroid.getX()));
            dot.setAttribute("cy", String.format("%g", -centroid.getY()));
            dot.setAttribute("r", "10");
        }

        /* Add the border. */
        Element border = doc.createElementNS(SVG_NS_URI, "path");
        root.appendChild(border);
        border.setAttribute("style", "opacity: 0.3; " + "stroke-width: 20; "
            + "stroke: black; " + "fill: none");
        border.setAttribute("d", pathOf(boundary));

        /* Write out the document. */
        TransformerFactory transFac = TransformerFactory.newInstance();
        Transformer trans = transFac.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.METHOD, "xml");
        trans.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, SVG_PUBLIC_ID);
        trans.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, SVG_SYSTEM_ID);
        trans.transform(new DOMSource(doc), new StreamResult(System.out));
    }

    private static final String SVG_NS_URI = "http://www.w3.org/2000/svg";
    private static final String SVG_SYSTEM_ID =
        "http://www.w3.org/TR/2000/03/WD-SVG-20000303/DTD/"
            + "svg-20000303-stylable.dtd";
    private static final String SVG_PUBLIC_ID =
        "-//W3C//DTD SVG 20000303 Stylable//EN";

    private static String pathOf(List<? extends Point2D> list) {
        StringBuilder result = new StringBuilder();
        String mode = "M";
        for (Point2D p : list) {
            result.append(mode).append(p.getX()).append(' ')
                .append(-p.getY());
            mode = " L";
        }
        result.append('z');
        return result.toString();
    }
}
