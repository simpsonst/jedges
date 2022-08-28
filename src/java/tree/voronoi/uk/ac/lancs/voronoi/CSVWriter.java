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

import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Writes lists of strings as comma-separated values. Special characters
 * will be properly quoted or escaped.
 *
 * @author simpsons
 */
final class CSVWriter implements AutoCloseable, Flushable {
    private final Writer base;

    /**
     * Create a CSV writer over a character sink.
     * 
     * @param base the character sink
     */
    public CSVWriter(Writer base) {
        this.base = base;
    }

    /**
     * Write a row of data.
     * 
     * @param fields the data to be written
     * 
     * @throws IOException if an I/O error occurred
     */
    public void writeRow(List<? extends String> fields) throws IOException {
        String sep = "";
        for (String s : fields) {
            base.write(sep);
            base.write(encode(s));
            sep = ",";
        }
        base.write('\n');
    }

    /**
     * Flush the underlying character sink.
     * 
     * @throws IOException if an I/O error occurred
     */
    @Override
    public void flush() throws IOException {
        base.flush();
    }

    /**
     * Close the underlying character sink. Subsequent calls to
     * {@link #writeRow(List)} and {@link #flush()} will likely fail.
     * 
     * @throws IOException if an I/O error occurred
     */
    @Override
    public void close() throws IOException {
        base.close();
    }

    private static String encode(String s) {
        if (s.indexOf('\n') < 0 && s.indexOf(',') < 0 && s.indexOf('"') < 0
            && s.trim().equals(s)) return s;
        s = s.replace("\"", "\"\"");
        return "\"" + s + '"';
    }
}
