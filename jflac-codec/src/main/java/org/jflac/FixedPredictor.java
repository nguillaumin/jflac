package org.jflac;

/**
 *  libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2000,2001,2002,2003  Josh Coalson
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

/**
 * Fixed Predictor utility class.
 * @author kc7bfi
 */
public class FixedPredictor {
    
    private static final double M_LN2 = 0.69314718055994530942;
    
    /**
     * Compute the residual from the compressed signal.
     * @param data
     * @param dataLen
     * @param order
     * @param residual
     */
    public static void computeResidual(int[] data, int dataLen, int order, int[] residual) {
        int idataLen = (int) dataLen;
        
        switch (order) {
            case 0 :
                for (int i = 0; i < idataLen; i++) {
                    residual[i] = data[i];
                }
                break;
            case 1 :
                for (int i = 0; i < idataLen; i++) {
                    residual[i] = data[i] - data[i - 1];
                }
                break;
            case 2 :
                for (int i = 0; i < idataLen; i++) {
                    /* == data[i] - 2*data[i-1] + data[i-2] */
                    residual[i] = data[i] - (data[i - 1] << 1) + data[i - 2];
                }
                break;
            case 3 :
                for (int i = 0; i < idataLen; i++) {
                    /* == data[i] - 3*data[i-1] + 3*data[i-2] - data[i-3] */
                    residual[i] = data[i] - (((data[i - 1] - data[i - 2]) << 1) + (data[i - 1] - data[i - 2])) - data[i - 3];
                }
                break;
            case 4 :
                for (int i = 0; i < idataLen; i++) {
                    /* == data[i] - 4*data[i-1] + 6*data[i-2] - 4*data[i-3] + data[i-4] */
                    residual[i] = data[i] - ((data[i - 1] + data[i - 3]) << 2) + ((data[i - 2] << 2) + (data[i - 2] << 1)) + data[i - 4];
                }
                break;
            default :
        }
    }
    
    /**
     * Restore the signal from the fixed predictor.
     * @param residual  The residual data
     * @param dataLen   The length of residual data
     * @param order     The preicate order
     * @param data      The restored signal (output)
     * @param startAt   The starting position in the data array
     */
    public static void restoreSignal(int[] residual, int dataLen, int order, int[] data, int startAt) {
        int idataLen = (int) dataLen;
        
        switch (order) {
            case 0 :
                for (int i = 0; i < idataLen; i++) {
                    data[i + startAt] = residual[i];
                }
                break;
            case 1 :
                for (int i = 0; i < idataLen; i++) {
                    data[i + startAt] = residual[i] + data[i + startAt - 1];
                }
                break;
            case 2 :
                for (int i = 0; i < idataLen; i++) {
                    /* == residual[i] + 2*data[i-1] - data[i-2] */
                    data[i + startAt] = residual[i] + (data[i + startAt - 1] << 1) - data[i + startAt - 2];
                }
                break;
            case 3 :
                for (int i = 0; i < idataLen; i++) {
                    /* residual[i] + 3*data[i-1] - 3*data[i-2]) + data[i-3] */
                    data[i + startAt] = residual[i] + (((data[i + startAt - 1] - data[i + startAt - 2]) << 1) + (data[i + startAt - 1] - data[i + startAt - 2])) + data[i + startAt - 3];
                }
                break;
            case 4 :
                for (int i = 0; i < idataLen; i++) {
                    /* == residual[i] + 4*data[i-1] - 6*data[i-2] + 4*data[i-3] - data[i-4] */
                    data[i + startAt] = residual[i] + ((data[i + startAt - 1] + data[i + startAt - 3]) << 2) - ((data[i + startAt - 2] << 2) + (data[i + startAt - 2] << 1)) - data[i + startAt - 4];
                }
                break;
            default :
        }
    }
}
