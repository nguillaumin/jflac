package org.kc7bfi.jflac.frame;

/**
 * libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

import java.io.IOException;

import org.kc7bfi.jflac.ChannelData;
import org.kc7bfi.jflac.Constants;
import org.kc7bfi.jflac.FixedPredictor;
import org.kc7bfi.jflac.util.InputBitStream;

public class ChannelFixed extends ChannelBase {
    static final int MAX_FIXED_ORDER = 4;

    protected EntropyCodingMethod entropyCodingMethod; // The residual coding method.
    protected int order; // The polynomial order.
    protected int[] warmup = new int[MAX_FIXED_ORDER]; // Warmup samples to prime the predictor, length == order.
    protected int[] residual; // The residual signal, length == (blocksize minus order) samples.

    public ChannelFixed(InputBitStream is, Header header, ChannelData channelData, int bps, int wastedBits, int order) throws IOException {
        super(header, wastedBits);
        
        this.residual = channelData.residual;
        this.order = order;

        // read warm-up samples
        for (int u = 0; u < order; u++) {
            warmup[u] = is.readRawInt(bps);
        }

        // read entropy coding method info
        int type = is.readRawUInt(Constants.ENTROPY_CODING_METHOD_TYPE_LEN);
        EntropyPartitionedRice pr;
        switch (type) {
            case Constants.ENTROPY_CODING_METHOD_PARTITIONED_RICE :
                int u32 = is.readRawUInt(Constants.ENTROPY_CODING_METHOD_PARTITIONED_RICE_ORDER_LEN);
                pr = new EntropyPartitionedRice();
                entropyCodingMethod = pr;
                pr.order = u32;
                pr.contents = channelData.partitionedRiceContents;
                pr.readResidual(is, order, pr.order, header, channelData.residual);
                break;
            default :
                throw new IOException("STREAM_DECODER_UNPARSEABLE_STREAM");
        }

        // decode the subframe
        System.arraycopy(warmup, 0, channelData.output, 0, order);
        FixedPredictor.restoreSignal(residual, header.blockSize - order, order, channelData.output, order);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer("FLACSubframe_Fixed: Order="+order+" WastedBits="+wastedBits);
        for (int i = 0; i < order; i++) sb.append(" warmup["+i+"]="+warmup[i]);
        return sb.toString();
    }
}
