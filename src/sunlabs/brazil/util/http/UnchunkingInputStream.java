/*
 * UnchunkingInputStream.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2009 Sun Microsystems, Inc.
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version 
 * 1.0 (the "License"). You may not use this file except in compliance with 
 * the License. A copy of the License is included as the file "license.terms",
 * and also available at http://www.sun.com/
 * 
 * The Original Code is from:
 *    Brazil project web application toolkit release 2.3.
 * The Initial Developer of the Original Code is: suhler.
 * Portions created by suhler are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): suhler.
 *
 * Version:  1.1
 * Created by suhler on 09/09/01
 * Last modified by suhler on 09/09/01 10:18:31
 *
 * Version Histories:
 *
 * 1.2 70/01/01-00:00:02 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 util/http/UnchunkingInputStream.java
 *
 * 1.1 09/09/01-10:18:31 (suhler)
 *   date and time created 09/09/01 10:18:31 by suhler
 *
 */

package sunlabs.brazil.util.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * Un-chunk a chunked encoded stream.
 * ( This was taken from HttpRequest, 
 * and should replace it.)
 */

public class UnchunkingInputStream extends HttpInputStream {
    public static final int LINE_LIMIT=2000;  // max legal http header line
    MimeHeaders trailers;	// any trailing headers :)
    boolean eof;
    int bytesLeft;

    public
    UnchunkingInputStream(InputStream in) {
	this(in, null);
    }

    public
    UnchunkingInputStream(InputStream in, MimeHeaders trailers) {
	super(in);
	this.trailers = trailers==null ? new MimeHeaders() : trailers;
    }

    @Override
    public int
    read() throws IOException {
	if ((bytesLeft <= 0) && (getChunkSize() == false)) {
	    return -1;
	}
	bytesLeft--;
	return in.read();
    }

    @Override
    public int
    read(byte[] buf, int off, int len) throws IOException {
	int total = 0;
	while (true) {
	    if ((bytesLeft <= 0) && (getChunkSize() == false)) {
		break;
	    }
	    int count = super.read(buf, off, Math.min(bytesLeft, len));
	    total += count;
	    off += count;
	    bytesLeft -= count;
	    len -= count;

	    if ((len <= 0) || (available() == 0)) {
		break;
	    }
	}

	return (total == 0) ? -1 : total;
    }

    private boolean
    getChunkSize() throws IOException {
	if (eof) {
	    return false;
	}

	/*
	 * Although HTTP/1.1 chunking spec says that there is one "\r\n"
	 * between chunks, some servers (for example, maps.yahoo.com) 
	 * send more than one blank line between chunks.  So, read and skip
	 * all the blank lines seen between chunks.
	 */

	String line;
	do {
	    // Sanity check: limit chars when expecting a chunk size.

	    line = ((HttpInputStream) in).readLine(LINE_LIMIT);
	} while ((line != null) && (line.length() == 0));

	try {
	    bytesLeft = Integer.parseInt(line.trim(), 16);
	} catch (Exception e) {
	    throw new IOException("malformed chunk");
	}
	if (bytesLeft == 0) {
	    eof = true;
	    trailers.read((HttpInputStream)in);
	    return false;
	}
	return true;
    }

    public MimeHeaders getTrailers() {
	return trailers;
    }
}
