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
import org.flac.LPCPredictor;
import org.flac.io.InputBitStream;
import org.flac.util.BitMath;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ChannelLPC extends ChannelBase {
    static final private int MAX_LPC_ORDER = 32;

    protected EntropyCodingMethod entropyCodingMethod; // The residual coding method.
    protected int order; // The FIR order.
    protected int qlpCoeffPrecision; // Quantized FIR filter coefficient precision in bits.
    protected int quantizationLevel; // The qlp coeff shift needed.
    protected int[] qlpCoeff = new int[MAX_LPC_ORDER]; // FIR filter coefficients.
    protected int[] warmup = new int[MAX_LPC_ORDER]; // Warmup samples to prime the predictor, length == order.
    protected int[] residual; // The residual signal, length == (blocksize minus order) samples.

    public ChannelLPC(InputBitStream is, Header header, ChannelData channelData, int bps, int wastedBits, int order) throws IOException {
        super(header, wastedBits);

        this.residual = channelData.residual;
        this.order = order;

        // read warm-up samples
        for (int u = 0; u < order; u++) {
            warmup[u] = is.readRawInt(bps);
        }

        // read qlp coeff precision
        int u32 = is.readRawUInt(Constants.SUBFRAME_LPC_QLP_COEFF_PRECISION_LEN);
        if (u32 == (1 << Constants.SUBFRAME_LPC_QLP_COEFF_PRECISION_LEN) - 1) {
            throw new IOException("STREAM_DECODER_ERROR_STATUS_LOST_SYNC");
        }
        qlpCoeffPrecision = u32 + 1;

        // read qlp shift
        quantizationLevel = is.readRawInt(Constants.SUBFRAME_LPC_QLP_SHIFT_LEN);

        // read quantized lp coefficiencts
        for (int u = 0; u < order; u++) {
            qlpCoeff[u] = is.readRawInt(qlpCoeffPrecision);
        }

        // read entropy coding method info
        int codingType = is.readRawUInt(Constants.ENTROPY_CODING_METHOD_TYPE_LEN);
        switch (codingType) {
            case Constants.ENTROPY_CODING_METHOD_PARTITIONED_RICE :
                ((EntropyPartitionedRice) entropyCodingMethod).order = is.readRawUInt(Constants.ENTROPY_CODING_METHOD_PARTITIONED_RICE_ORDER_LEN);
                ((EntropyPartitionedRice) entropyCodingMethod).contents = channelData.partitionedRiceContents;
                break;
            default :
                throw new IOException("STREAM_DECODER_UNPARSEABLE_STREAM");
        }
 
        // read residual
        if (entropyCodingMethod instanceof EntropyPartitionedRice) {
            ((EntropyPartitionedRice) entropyCodingMethod).readResidual(is, 
                order,
                ((EntropyPartitionedRice) entropyCodingMethod).order,
                header,
                channelData.residual);
        }

        // decode the subframe
        System.arraycopy(warmup, 0, channelData.output, 0, order);
        if (bps + qlpCoeffPrecision + BitMath.ilog2(order) <= 32) {
            if (bps <= 16 && qlpCoeffPrecision <= 16)
                LPCPredictor.restoreSignal(channelData.residual, header.blockSize - order, qlpCoeff, order, quantizationLevel, channelData.output, order);
            else
                LPCPredictor.restoreSignal(channelData.residual, header.blockSize - order, qlpCoeff, order, quantizationLevel, channelData.output, order);
        } else {
            LPCPredictor.restoreSignalWide(channelData.residual, header.blockSize - order, qlpCoeff, order, quantizationLevel, channelData.output, order);
        }
    }
    
    public String toString() {
        return "FLACSubframe_LPC: WastedBits="+wastedBits;
    }

}
