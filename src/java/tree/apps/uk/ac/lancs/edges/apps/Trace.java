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

package uk.ac.lancs.edges.apps;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
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

import uk.ac.lancs.edges.ColorOptimizer;
import uk.ac.lancs.edges.MultiOptimizerSlicer;
import uk.ac.lancs.edges.Process;
import uk.ac.lancs.edges.Score;
import uk.ac.lancs.edges.Scribe;
import uk.ac.lancs.edges.Slicer;
import uk.ac.lancs.edges.rect.CleverRectangularColorOptimizer;
import uk.ac.lancs.edges.rect.MinimalRectangularColorOptimizer;
import uk.ac.lancs.edges.rect.PerimeterRectangularColorSelector;
import uk.ac.lancs.edges.rect.RectangularColorCollector;
import uk.ac.lancs.edges.rect.RectangularGrid;
import uk.ac.lancs.edges.rect.RectangularLayoutFactory;

public final class Trace {
    private static final String SVG_NS_URI = "http://www.w3.org/2000/svg";
    private static final String SVG_SYSTEM_ID =
        "http://www.w3.org/TR/2000/03/WD-SVG-20000303/DTD/"
            + "svg-20000303-stylable.dtd";
    private static final String SVG_PUBLIC_ID =
        "-//W3C//DTD SVG 20000303 Stylable//EN";

    interface ColorMappedRectangularGrid extends RectangularGrid {
        int getMaxColors();

        int getRGB(int num);
    }

    static final ColorMappedRectangularGrid makeGrid(File file)
        throws IOException {
        /* Load the file and get dimensions. */
        BufferedImage image = ImageIO.read(file);
        final int width = image.getWidth();
        final int height = image.getHeight();

        /* Derive a colour map, with 0 as transparent. */
        final Map<Integer, Integer> map = new HashMap<>();
        final List<Integer> invMap = new ArrayList<>();
        invMap.add(0);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int code = image.getRGB(x, y);
                int alpha = code >>> 24;
                if (alpha > 0 && alpha < 255)
                    throw new IllegalArgumentException("only 1bpp alpha supported");
                if (alpha == 0) continue;

                /* We have a solid colour now. */
                assert alpha == 255;
                if (map.containsKey(code)) continue;
                map.put(code, invMap.size());
                invMap.add(code);
                if (invMap.size() >= 20)
                    throw new IllegalArgumentException("too many colours");
            }

        /* Create and return a grid view of the image. */
        return new ColorMappedRectangularGrid() {
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
                if (x < 0 || x >= width || y < 0 || y >= height) return 0;
                return map.getOrDefault(image.getRGB(x, y), 0);
            }

            @Override
            public int getMaxColors() {
                return map.size();
            }

            @Override
            public int getRGB(int num) {
                return invMap.get(num);
            }
        };
    }

    public static void main(String[] args) throws Exception {
        /* Get a grid from an image, hopefully with only a small number
         * of distinct colours, and at most a 1bpp mask. */
        ColorMappedRectangularGrid grid = makeGrid(new File(args[0]));

        /* Create several competing optimizers. */
        List<ColorOptimizer<RectangularGrid>> optimizers = new ArrayList<>();
        optimizers.add(MinimalRectangularColorOptimizer.INSTANCE);
        optimizers
            .add(new CleverRectangularColorOptimizer(Score::movesAndDraws,
                                                     Score::draws, true));

        /* Create a slicer that will try out competing optimizers, and
         * choose the best for each colour. */
        Comparator<Score> scorer =
            (a, b) -> Integer.compare(a.draws(), b.draws());
        Slicer<RectangularGrid, Point> slicer =
            new MultiOptimizerSlicer<>(RectangularColorCollector.INSTANCE,
                                       new PerimeterRectangularColorSelector(),
                                       optimizers,
                                       RectangularLayoutFactory.INSTANCE,
                                       scorer);

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
        root.setAttribute("viewBox", String.format("0 0 %d %d", grid.width(),
                                                   grid.height()));
        Element group = doc.createElementNS(SVG_NS_URI, "g");
        root.appendChild(group);
        group.setAttribute("style", "stroke: none; fill-rule: evenodd");

        /* Slice the grid into colours and corresponding scribes and
         * processes. */
        List<Process> processes = new ArrayList<>(grid.getMaxColors());
        class MyScribe implements Scribe<Point> {
            final Element elem = doc.createElementNS(SVG_NS_URI, "path");
            final StringBuilder path = new StringBuilder();
            boolean down;

            MyScribe(int code) {
                int color = grid.getRGB(code);
                elem.setAttribute("style", String.format("fill: #%06x",
                                                         color & 0xffffff));
            }

            @Override
            public void draw(Point to) {
                if (down) {
                    path.append('L');
                } else {
                    down = true;
                    path.append('M');
                }
                path.append(to.x).append(' ').append(to.y);
            }

            @Override
            public void close() {
                down = false;
                path.append('z');
            }

            public void appendTo(Element parent) {
                elem.setAttribute("d", path.toString());
                parent.appendChild(elem);
            }
        }
        List<MyScribe> scribeOrder = new ArrayList<>(grid.getMaxColors());
        slicer.slice(grid, c -> new MyScribe(c), processes, scribeOrder);

        /* Set all the processes running. */
        processes.parallelStream().forEach(Process::processAll);

        /* Append the scribes in order. */
        scribeOrder.stream().forEachOrdered(s -> s.appendTo(group));

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
}
