/*
 * Created on Mar 13, 2004
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
public class ChannelConstant extends ChannelBase {
    protected int value; // The constant signal value.

    public ChannelConstant(InputBitStream is, Header header, ChannelData channelData, int bps, int wastedBits) throws IOException {
        super(header, wastedBits);

        value = is.readRawInt(bps);

        // decode the subframe
        for (int i = 0; i < header.blockSize; i++) channelData.output[i] = value;
    }
    
    public String toString() {
        return "ChannelConstant: Value="+value+" WastedBits="+wastedBits;
    }
}
