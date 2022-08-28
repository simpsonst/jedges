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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads comma-separated values as lists of strings. Quoting and
 * escaping will be removed.
 *
 * @author simpsons
 */
final class CSVReader implements AutoCloseable {
    private final Reader base;

    /**
     * Create a CSV reader over a character source.
     * 
     * @param base the character source
     */
    public CSVReader(Reader base) {
        this.base = base;
    }

    /**
     * Read a row of comma-separated values as a list. This could read
     * more than one line if it contains escaped or quoted new-line
     * characters.
     * 
     * @return a list of values, or {@code null} if there are no more
     * rows
     * 
     * @throws IOException if an I/O error occurred
     */
    public List<String> readRow() throws IOException {
        List<String> result = new ArrayList<>();
        boolean quoted = false;
        int c;
        StringBuilder b = new StringBuilder();
        next:
        while ((c = base.read()) >= 0) {
            switch (c) {
            case '"':
                quoted = !quoted;
                break;

            case ',':
                if (quoted) break;
                result.add(decode(b.toString()));
                b.delete(0, b.length());
                continue next;

            case '\n':
                if (quoted) break;
                if (!result.isEmpty() || b.length() != 0)
                    result.add(decode(b.toString()));
                return result;
            }
            b.append((char) c);
        }
        if (c < 0 && result.isEmpty()) return null;
        result.add(decode(b.toString()));
        return result;
    }

    /**
     * Close the underlying character source. Subsequent calls to
     * {@link #readRow()} will likely fail.
     * 
     * @throws IOException if an I/O error occurred
     */
    @Override
    public void close() throws IOException {
        base.close();
    }

    private static String decode(String s) {
        StringBuilder r = new StringBuilder(s.trim());
        boolean last = false;
        for (int i = 0; i < r.length(); i++) {
            switch (r.charAt(i)) {
            case '"':
                if (last) {
                    last = false;
                    break;
                }
                r.deleteCharAt(i--);
                last = true;
                break;

            default:
                last = false;
                break;
            }
        }
        return r.toString();
    }
}
