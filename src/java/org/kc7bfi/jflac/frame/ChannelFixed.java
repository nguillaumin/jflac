/*
 * Created on Mar 13, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.frame;

import java.io.IOException;

import org.flac.ChannelData;
import org.flac.Constants;
import org.flac.FixedPredictor;
import org.flac.io.InputBitStream;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
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
