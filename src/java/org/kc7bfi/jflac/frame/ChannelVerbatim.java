/*
 * Created on Mar 15, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.frame;

import java.io.IOException;

import org.flac.ChannelData;
import org.flac.io.InputBitStream;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ChannelVerbatim extends ChannelBase {
    protected int[] data; // A pointer to verbatim signal.
    
    public ChannelVerbatim(InputBitStream is, Header header, ChannelData channelData, int bps, int wastedBits) throws IOException {
        super(header, wastedBits);

        data = channelData.residual;

        for (int i = 0; i < header.blockSize; i++) {
            data[i] = is.readRawInt(bps);
        }

        // decode the subframe
        System.arraycopy(data, 0, channelData.output, 0, header.blockSize);
    }
    
    public String toString() {
        return "ChannelVerbatim: WastedBits="+wastedBits;
    }
}
