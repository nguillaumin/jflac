/*
 * Created on Mar 13, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.frame;
import java.io.IOException;

import org.kc7bfi.jflac.Constants;
import org.kc7bfi.jflac.io.InputBitStream;

/**
 * @author kc7bfi
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class EntropyPartitionedRice extends EntropyCodingMethod {

    protected int order; // The partition order, i.e. # of contexts = 2 ^ order.
    protected EntropyPartitionedRiceContents contents; // The context's Rice parameters and/or raw bits.

    void readResidual(InputBitStream is, int predictorOrder, int partitionOrder, Header header, int[] residual) throws IOException {
        int sample = 0;
        int partitions = 1 << partitionOrder;
        int partitionSamples = partitionOrder > 0 ? header.blockSize >> partitionOrder : header.blockSize - predictorOrder;
        contents.ensureSize(Math.max(6, partitionOrder));
        contents.parameters = new int[partitions];

        for (int partition = 0; partition < partitions; partition++) {
            int riceParameter = is.readRawUInt(Constants.ENTROPY_CODING_METHOD_PARTITIONED_RICE_PARAMETER_LEN);
            contents.parameters[partition] = riceParameter;
            if (riceParameter < Constants.ENTROPY_CODING_METHOD_PARTITIONED_RICE_ESCAPE_PARAMETER) {
                int u = (partitionOrder == 0 || partition > 0) ? partitionSamples : partitionSamples - predictorOrder;
                is.readRiceSignedBlock(residual, sample, u, riceParameter);
                sample += u;
            } else {
                riceParameter = is.readRawUInt(Constants.ENTROPY_CODING_METHOD_PARTITIONED_RICE_RAW_LEN);
                contents.rawBits[partition] = riceParameter;
                for (int u = (partitionOrder == 0 || partition > 0) ? 0 : predictorOrder; u < partitionSamples; u++, sample++) {
                    residual[sample] = is.readRawInt(riceParameter);
                }
            }
        }
    }
}