package org.kc7bfi.jflac.util;

/**
 * libFLAC - Free Lossless Audio Codec library Copyright (C) 2000,2001,2002,2003
 * Josh Coalson
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Bit-wide input stream.
 * @author kc7bfi
 */
public class InputBitStream {
    private static final int BITS_PER_BLURB = 8;
    private static final int BITS_PER_BLURB_LOG2 = 3;
    private static final byte BLURB_TOP_BIT_ONE = ((byte) 0x80);
    private static final long[] MASK32 = new long[]{0, 0x0000000000000001, 0x0000000000000003, 0x0000000000000007, 0x000000000000000F,
            0x000000000000001F, 0x000000000000003F, 0x000000000000007F, 0x00000000000000FF, 0x00000000000001FF, 0x00000000000003FF,
            0x00000000000007FF, 0x0000000000000FFF, 0x0000000000001FFF, 0x0000000000003FFF, 0x0000000000007FFF, 0x000000000000FFFF,
            0x000000000001FFFF, 0x000000000003FFFF, 0x000000000007FFFF, 0x00000000000FFFFF, 0x00000000001FFFFF, 0x00000000003FFFFF,
            0x00000000007FFFFF, 0x0000000000FFFFFF, 0x0000000001FFFFFF, 0x0000000003FFFFFF, 0x0000000007FFFFFF, 0x000000000FFFFFFF,
            0x000000001FFFFFFF, 0x000000003FFFFFFF, 0x000000007FFFFFFF, 0x00000000FFFFFFFF, 0x00000001FFFFFFFFL,
            0x00000003FFFFFFFFL, 0x00000007FFFFFFFFL, 0x0000000FFFFFFFFFL, 0x0000001FFFFFFFFFL, 0x0000003FFFFFFFFFL,
            0x0000007FFFFFFFFFL, 0x000000FFFFFFFFFFL, 0x000001FFFFFFFFFFL, 0x000003FFFFFFFFFFL, 0x000007FFFFFFFFFFL,
            0x00000FFFFFFFFFFFL, 0x00001FFFFFFFFFFFL, 0x00003FFFFFFFFFFFL, 0x00007FFFFFFFFFFFL, 0x0000FFFFFFFFFFFFL,
            0x0001FFFFFFFFFFFFL, 0x0003FFFFFFFFFFFFL, 0x0007FFFFFFFFFFFFL, 0x000FFFFFFFFFFFFFL, 0x001FFFFFFFFFFFFFL,
            0x003FFFFFFFFFFFFFL, 0x007FFFFFFFFFFFFFL, 0x00FFFFFFFFFFFFFFL, 0x01FFFFFFFFFFFFFFL, 0x03FFFFFFFFFFFFFFL,
            0x07FFFFFFFFFFFFFFL, 0x0FFFFFFFFFFFFFFFL, 0x1FFFFFFFFFFFFFFFL, 0x3FFFFFFFFFFFFFFFL, 0x7FFFFFFFFFFFFFFFL,
            0xFFFFFFFFFFFFFFFFL};
    
    private byte[] buffer = new byte[256];
    private int inBlurbs = 0;
    private int inBits = 0;
    private int totalBits = 0; // must always == BITS_PER_BLURB*blurbs+bits
    private int consumedBlurbs = 0;
    private int consumedBits = 0;
    private int totalConsumedBits = 0;
    private int totalBitsRead = 0;
    
    private short readCRC16 = 0;
    
    private InputStream inStream;
    
    /**
     * The constructor.
     * @param is    The InputStream to read bits from
     */
    public InputBitStream(InputStream is) {
        this.inStream = is;
    }
    
    private void resize(int newCapacity) {
        if (buffer.length >= newCapacity) return;
        System.out.println("RESIZE FROM " + buffer.length + " TO " + newCapacity);
        byte[] newBuffer = new byte[newCapacity];
        System.arraycopy(buffer, 0, newBuffer, 0, inBlurbs + ((inBits != 0) ? 1 : 0));
        buffer = newBuffer;
        return;
    }
    
    private void grow(int minBlurbsToAdd) {
        int newCapacity = (buffer.length + minBlurbsToAdd + 255) / 256;
        resize(newCapacity);
    }
    
    private void ensureSize(int bitsToAdd) {
        int blurbsToAdd = (bitsToAdd + 7) >> 3;
        if (buffer.length < (inBlurbs + blurbsToAdd))
            grow(blurbsToAdd);
    }
    
    private int readFromStream() throws IOException {
        // first shift the unconsumed buffer data toward the front as much as possible
        if (totalConsumedBits >= BITS_PER_BLURB) {
            int l = 0;
            int r = consumedBlurbs;
            int rEnd = inBlurbs + ((inBits != 0) ? 1 : 0);
            for (; r < rEnd; l++, r++)
                buffer[l] = buffer[r];
            for (; l < rEnd; l++)
                buffer[l] = 0;
            inBlurbs -= consumedBlurbs;
            totalBits -= consumedBlurbs << 3;
            consumedBlurbs = 0;
            totalConsumedBits = consumedBits;
        }
        
        // grow if we need to
        if (buffer.length <= 1) resize(16);
        
        // set the target for reading, taking into account blurb alignment
        // blurb == byte, so no gyrations necessary:
        int bytes = buffer.length - inBlurbs;
        
        // finally, read in some data
        bytes = inStream.read(buffer, inBlurbs, bytes);
        if (bytes <= 0) throw new EOFException();
        
        // now we have to handle partial blurb cases:
        // blurb == byte, so no gyrations necessary:
        inBlurbs += bytes;
        totalBits += bytes << 3;
        return bytes;
    }
    
    /**
     * Reset the bit stream.
     */
    public void reset() {
        inBlurbs = 0;
        inBits = 0;
        totalBits = 0;
        consumedBlurbs = 0;
        consumedBits = 0;
        totalConsumedBits = 0;
    }
    
    /**
     * Concatinate one InputBitStream to the end of this one.
     * @param src   The inputBitStream to copy
     * @return      True if copy was successful
     */
    public boolean concatenateAligned(InputBitStream src) {
        int bitsToAdd = src.totalBits - src.totalConsumedBits;
        if (bitsToAdd == 0) return true;
        if (inBits != src.consumedBits) return false;
        ensureSize(bitsToAdd);
        if (inBits == 0) {
            System.arraycopy(src.buffer, src.consumedBlurbs, buffer, inBlurbs, 
                    (src.inBlurbs - src.consumedBlurbs + ((src.inBits != 0) ? 1 : 0)));
        } else if (inBits + bitsToAdd > BITS_PER_BLURB) {
            buffer[inBlurbs] <<= (BITS_PER_BLURB - inBits);
            buffer[inBlurbs] |= (src.buffer[src.consumedBlurbs] & ((1 << (BITS_PER_BLURB - inBits)) - 1));
            System.arraycopy(src.buffer, src.consumedBlurbs + 1, buffer, inBlurbs + 11,
                    (src.inBlurbs - src.consumedBlurbs - 1 + ((src.inBits != 0) ? 1 : 0)));
        } else {
            buffer[inBlurbs] <<= bitsToAdd;
            buffer[inBlurbs] |= (src.buffer[src.consumedBlurbs] & ((1 << bitsToAdd) - 1));
        }
        inBits = src.inBits;
        totalBits += bitsToAdd;
        inBlurbs = totalBits / BITS_PER_BLURB;
        return true;
    }
    
    /**
     * Reset the read CRC-16 value.
     * @param seed  The initial CRC-16 value
     */
    public void resetReadCRC16(short seed) {
        readCRC16 = seed;
    }
    
    /**
     * return the read CRC-16 value.
     * @return  The read CRC-16 value
     */
    public short getReadCRC16() {
        return readCRC16;
    }
    
    /**
     * Test if the Bit Stream is byte aligned.
     * @return  True of bit stream is byte aligned
     */
    public boolean isByteAligned() {
        return ((inBits & 7) == 0);
    }
    
    /**
     * Test if the Bit Stream consumed bits is byte aligned.
     * @return  True of bit stream consumed bits is byte aligned
     */
    public boolean isConsumedByteAligned() {
        return ((consumedBits & 7) == 0);
    }
    
    /**
     * return the number of bits to read to align the byte.
     * @return  The number of bits to align the byte
     */
    public int bitsLeftForByteAlignment() {
        return 8 - (consumedBits & 7);
    }
    
    /**
     * return the number of bytes left to read.
     * @return  The number of bytes left to read
     */
    public int getInputBytesUnconsumed() {
        return (totalBits - totalConsumedBits) >> 3;
    }
    
    
    public int riceBits(int val, int parameter) {
        int msbs, uval;
        // fold signed to unsigned
        if (val < 0) {
            // equivalent to (unsigned)(((--val) < < 1) - 1); but without the overflow problem at MININT
            uval = (int) (((-(++val)) << 1) + 1);
        } else {
            uval = (int) (val << 1);
        }
        msbs = uval >> parameter;
        return 1 + parameter + msbs;
    }
    /*
     * DRR FIX # ifdef SYMMETRIC_RICE boolean
     * write_symmetric_rice_signed(BitBuffer8 * bb, int val, unsigned parameter) {
     * unsigned total_bits, interesting_bits, msbs; uint32 pattern;
     * 
     * ASSERT(0 != bb); ASSERT(0 != buffer); ASSERT(parameter <= 31); // init
     * pattern with the unary end bit and the sign bit if (val < 0) { pattern =
     * 3; val = -val; } else pattern = 2;
     * 
     * msbs = val >> parameter; interesting_bits = 2 + parameter; total_bits =
     * interesting_bits + msbs; pattern < <= parameter; pattern |= (val & ((1 < <
     * parameter) - 1)); // the binary LSBs
     * 
     * if (total_bits <= 32) { if (!write_raw_uint32(bb, pattern, total_bits))
     * return false; } else { // write the unary MSBs if (!write_zeroes(bb,
     * msbs)) return false; // write the unary end bit, the sign bit, and binary
     * LSBs if (!write_raw_uint32(bb, pattern, interesting_bits)) return false; }
     * return true; }
     * 
     * boolean write_symmetric_rice_signed_escape(BitBuffer8 * bb, int val,
     * unsigned parameter) { unsigned total_bits, val_bits; uint32 pattern;
     * 
     * ASSERT(0 != bb); ASSERT(0 != buffer); ASSERT(parameter <= 31);
     * 
     * val_bits = bitmath_silog2(val); total_bits = 2 + parameter + 5 +
     * val_bits;
     * 
     * if (total_bits <= 32) { pattern = 3; pattern < <= (parameter + 5);
     * pattern |= val_bits; pattern < <= val_bits; pattern |= (val & ((1 < <
     * val_bits) - 1)); if (!write_raw_uint32(bb, pattern, total_bits)) return
     * false; } else { // write the '-0' escape code first if
     * (!write_raw_uint32(bb, 3 u < < parameter, 2 + parameter)) return false; //
     * write the length if (!write_raw_uint32(bb, val_bits, 5)) return false; //
     * write the value if (!write_raw_int32(bb, val, val_bits)) return false; }
     * return true; } # endif // ifdef SYMMETRIC_RICE
     */
    
    
    /*
     * DRR FIX boolean peek_bit(unsigned * val, boolean(* read_callback) (byte
     * buffer[], unsigned * bytes, void * client_data), void * client_data) {
     * 
     * while (1) { if (total_consumed_bits < total_bits) { val =
     * (buffer[consumed_blurbs] & BLURB_BIT_TO_MASK(consumed_bits)) ? 1 : 0;
     * return true; } else { if (!read_from_client_(bb, read_callback,
     * client_data)) return false; } } }
     */
    
    /**
     * skip over bits in bit stream without updating CRC.
     * @param bits  Number of bits to skip
     * @throws IOException  Thrown if error reading from input stream
     */
    public void skipBitsNoCRC(int bits) throws IOException {
        if (bits == 0) return;
        int n = consumedBits & 7;
        int m;
        if (n != 0) {
            m = Math.min(8 - n, bits);
            readRawUInt(m);
            bits -= m;
        }
        m = bits / 8;
        if (m > 0) {
            readByteBlockAlignedNoCRC(null, m);
            bits %= 8;
        }
        if (bits > 0) {
            readRawUInt(bits);
        }
    }
    
    /**
     * read a single bit.
     * @return  The bit
     * @throws IOException  Thrown if error reading input stream
     */
    public int readBit() throws IOException {
        while (true) {
            if (totalConsumedBits < totalBits) {
                int val = ((buffer[consumedBlurbs] & (0x80 >> consumedBits)) != 0) ? 1 : 0;
                consumedBits++;
                if (consumedBits == BITS_PER_BLURB) {
                    readCRC16 = CRC16.update(buffer[consumedBlurbs], readCRC16);
                    consumedBlurbs++;
                    consumedBits = 0;
                }
                totalConsumedBits++;
                totalBitsRead++;
                return val;
            } else {
                readFromStream();
            }
        }
    }
    
    /**
     * read a bit into an integer value.
     * The bits of the input integer are shifted left and the 
     * read bit is placed into bit 0.
     * @param val   The integer to shift and add read bit
     * @return      The updated integer value
     * @throws IOException  Thrown if error reading input stream
     */
    public int readBitToInt(int val) throws IOException {
        while (true) {
            if (totalConsumedBits < totalBits) {
                val <<= 1;
                val |= ((buffer[consumedBlurbs] & (0x80 >> consumedBits)) != 0) ? 1 : 0;
                consumedBits++;
                if (consumedBits == BITS_PER_BLURB) {
                    readCRC16 = CRC16.update(buffer[consumedBlurbs], readCRC16);
                    consumedBlurbs++;
                    consumedBits = 0;
                }
                totalConsumedBits++;
                totalBitsRead++;
                return val;
            } else {
                readFromStream();
            }
        }
    }
    
    /**
     * peek at the next bit and add it to the input integer.
     * The bits of the input integer are shifted left and the 
     * read bit is placed into bit 0.
     * @param val   The input integer
     * @param bit   The bit to peek at
     * @return      The updated integer value
     * @throws IOException  Thrown if error reading input stream
     */
    public int peekBitToInt(int val, int bit) throws IOException {
        while (true) {
            if ((totalConsumedBits + bit) < totalBits) {
                val <<= 1;
                if ((consumedBits + bit) >= BITS_PER_BLURB) {
                    bit = (consumedBits + bit) % BITS_PER_BLURB;
                    val |= ((buffer[consumedBlurbs + 1] & (0x80 >> bit)) != 0) ? 1 : 0;
                } else {
                    val |= ((buffer[consumedBlurbs] & (0x80 >> (consumedBits + bit))) != 0) ? 1 : 0;
                }
                return val;
            } else {
                readFromStream();
            }
        }
    }
    
    /**
     * read a bit into a long value.
     * The bits of the input long are shifted left and the 
     * read bit is placed into bit 0.
     * @param val   The long to shift and add read bit
     * @return      The updated long value
     * @throws IOException  Thrown if error reading input stream
     */
    public long readBitToLong(long val) throws IOException {
        while (true) {
            if (totalConsumedBits < totalBits) {
                val <<= 1;
                val |= ((buffer[consumedBlurbs] & (0x80 >> consumedBits)) != 0) ? 1 : 0;
                consumedBits++;
                if (consumedBits == BITS_PER_BLURB) {
                    readCRC16 = CRC16.update(buffer[consumedBlurbs], readCRC16);
                    consumedBlurbs++;
                    consumedBits = 0;
                }
                totalConsumedBits++;
                totalBitsRead++;
                return val;
            } else {
                readFromStream();
            }
        }
    }
    
    /**
     * read bits into an unsigned integer.
     * @param bits  The number of bits to read
     * @return      The bits as an unsigned integer
     * @throws IOException  Thrown if error reading input stream
     */
    public int readRawUInt(int bits) throws IOException {
        int val = 0;
        for (int i = 0; i < bits; i++) {
            val = readBitToInt(val);
        }
        return val;
    }
    
    /**
     * peek at bits into an unsigned integer without advancing the input stream.
     * @param bits  The number of bits to read
     * @return      The bits as an unsigned integer
     * @throws IOException  Thrown if error reading input stream
     */
    public int peekRawUInt(int bits) throws IOException {
        int val = 0;
        for (int i = 0; i < bits; i++) {
            val = peekBitToInt(val, i);
        }
        return val;
    }
    
    /**
     * read bits into a signed integer.
     * @param bits  The number of bits to read
     * @return      The bits as a signed integer
     * @throws IOException  Thrown if error reading input stream
     */
    public int readRawInt(int bits) throws IOException { 
        if (bits == 0) { return 0; }
        int v = 0;
        for (int i = 0; i < bits; i++) {
            v = readBitToInt(v);
        }
        
        // fix the sign
        int val;
        int i = 32 - bits;
        if (i != 0) {
            v <<= i;
            val = (int) v;
            val >>= i;
        } else {
            val = (int) v;
        }
        return val;
    }
    
    /**
     * read bits into an unsigned long.
     * @param bits  The number of bits to read
     * @return      The bits as an unsigned long
     * @throws IOException  Thrown if error reading input stream
     */
    public long readRawULong(int bits) throws IOException {
        long val = 0;
        for (int i = 0; i < bits; i++) {
            val = readBitToLong(val);
        }
        return val;
    }
    
    /**
     * read bits into an unsigned little endian integer.
     * @return      The bits as an unsigned integer
     * @throws IOException  Thrown if error reading input stream
     */
    public int readRawIntLittleEndian() throws IOException {
        int x32 = readRawUInt(8);
        int x8 = readRawUInt(8);
        x32 |= (x8 << 8);
        x8 = readRawUInt(8);
        x32 |= (x8 << 16);
        x8 = readRawUInt(8);
        x32 |= (x8 << 24);
        return x32;
    }
    
    /**
     * Read a block of bytes (aligned) without updating the CRC value.
     * @param val   The array to receive the bytes. If null, no bytes are returned
     * @param nvals The number of bytes to read
     * @throws IOException  Thrown if error reading input stream
     */
    public void readByteBlockAlignedNoCRC(byte[] val, int nvals) throws IOException {
        while (nvals > 0) {
            int chunk = Math.min(nvals, inBlurbs - consumedBlurbs);
            if (chunk == 0) {
                readFromStream();
            } else {
                if (val != null) System.arraycopy(buffer, consumedBlurbs, val, 0, chunk);
                nvals -= chunk;
                consumedBlurbs += chunk;
                totalConsumedBits = (consumedBlurbs << BITS_PER_BLURB_LOG2);
                totalBitsRead += (chunk << BITS_PER_BLURB_LOG2);
            }
        }
    }
    
    /**
     * Read and count the number of zero bits.
     * @return  The number of zero bits read
     * @throws IOException  Thrown if error reading input stream
     */
    public int readUnaryUnsigned() throws IOException {
        int val = 0;
        while (true) {
            int bit = readBit();
            if (bit != 0) break;
            val++;
        }
        return val;
    }
    
    /*
     * # ifdef SYMMETRIC_RICE boolean read_symmetric_rice_signed( BitBuffer8 *
     * bb, int * val, unsigned parameter, boolean(* read_callback) (byte
     * buffer[], unsigned * bytes, void * client_data), void * client_data) {
     * uint32 sign = 0, lsbs = 0, msbs = 0;
     * 
     * ASSERT(0 != bb); ASSERT(0 != buffer); ASSERT(parameter <= 31); // read
     * the unary MSBs and end bit if (!read_unary_unsigned(bb, & msbs,
     * read_callback, client_data)) return false; // read the sign bit if
     * (!read_bit_to_uint32(bb, & sign, read_callback, client_data)) return
     * false; // read the binary LSBs if (!read_raw_uint32(bb, & lsbs,
     * parameter, read_callback, client_data)) return false; // compose the
     * value val = (msbs < < parameter) | lsbs; if (sign) val = - (* val);
     * 
     * return true; } # endif // ifdef SYMMETRIC_RICE
     * 
     * boolean read_rice_signed( BitBuffer8 * bb, int * val, unsigned parameter,
     * boolean(* read_callback) (byte buffer[], unsigned * bytes, void *
     * client_data), void * client_data) { uint32 lsbs = 0, msbs = 0; unsigned
     * uval;
     * 
     * ASSERT(0 != bb); ASSERT(0 != buffer); ASSERT(parameter <= 31); // read
     * the unary MSBs and end bit if (!read_unary_unsigned(bb, & msbs,
     * read_callback, client_data)) return false; // read the binary LSBs if
     * (!read_raw_uint32(bb, & lsbs, parameter, read_callback, client_data))
     * return false; // compose the value uval = (msbs < < parameter) | lsbs; if
     * (uval & 1) val = - ((int) (uval >> 1)) - 1; else val = (int) (uval >> 1);
     * 
     * return true; }
     */
    
    public void readRiceSignedBlock(int[] vals, int pos, int nvals, int parameter) throws IOException {
        int i, j, valI = 0;
        int cbits = 0, uval = 0, msbs = 0, lsbsLeft = 0;
        byte blurb, saveBlurb;
        int state = 0; // 0 = getting unary MSBs, 1 = getting binary LSBs
        if (nvals == 0) return;
        i = consumedBlurbs;
        
        long startBits = consumedBlurbs * 8 + consumedBits;
        
        // We unroll the main loop to take care of partially consumed blurbs here.
        if (consumedBits > 0) {
            saveBlurb = blurb = buffer[i];
            cbits = consumedBits;
            blurb <<= cbits;
            while (true) {
                if (state == 0) {
                    if (blurb != 0) {
                        for (j = 0; (blurb & BLURB_TOP_BIT_ONE) == 0; j++)
                            blurb <<= 1;
                        msbs += j;
                        
                        // dispose of the unary end bit
                        blurb <<= 1;
                        j++;
                        cbits += j;
                        uval = 0;
                        lsbsLeft = parameter;
                        state++;
                        //totalBitsRead += msbs;
                        if (cbits == BITS_PER_BLURB) {
                            cbits = 0;
                            readCRC16 = CRC16.update(saveBlurb, readCRC16);
                            break;
                        }
                    } else {
                        msbs += BITS_PER_BLURB - cbits;
                        cbits = 0;
                        readCRC16 = CRC16.update(saveBlurb, readCRC16);
                        //totalBitsRead += msbs;
                        break;
                    }
                } else {
                    int availableBits = BITS_PER_BLURB - cbits;
                    if (lsbsLeft >= availableBits) {
                        uval <<= availableBits;
                        uval |= ((blurb & 0xff) >> cbits);
                        cbits = 0;
                        readCRC16 = CRC16.update(saveBlurb, readCRC16);
                        //totalBitsRead += availableBits;
                        if (lsbsLeft == availableBits) {
                            // compose the value
                            uval |= (msbs << parameter);
                            if ((uval & 1) != 0)
                                vals[pos + valI++] = -((int) (uval >> 1)) - 1;
                            else
                                vals[pos + valI++] = (int) (uval >> 1);
                            if (valI == nvals)
                                break;
                            msbs = 0;
                            state = 0;
                        }
                        lsbsLeft -= availableBits;
                        break;
                    } else {
                        uval <<= lsbsLeft;
                        uval |= ((blurb & 0xff) >> (BITS_PER_BLURB - lsbsLeft));
                        blurb <<= lsbsLeft;
                        cbits += lsbsLeft;
                        //totalBitsRead += lsbsLeft;
                        // compose the value
                        uval |= (msbs << parameter);
                        if ((uval & 1) != 0)
                            vals[pos + valI++] = -((int) (uval >> 1)) - 1;
                        else
                            vals[pos + valI++] = (int) (uval >> 1);
                        if (valI == nvals) {
                            // back up one if we exited the for loop because we
                            // read all nvals but the end came in the middle of
                            // a blurb
                            i--;
                            break;
                        }
                        msbs = 0;
                        state = 0;
                    }
                }
            }
            i++;
            consumedBlurbs = i;
            consumedBits = cbits;
            totalConsumedBits = (i << BITS_PER_BLURB_LOG2) | cbits;
            //totalBitsRead += (BITS_PER_BLURB) | cbits;
        }
        
        // Now that we are blurb-aligned the logic is slightly simpler
        while (valI < nvals) {
            for (; i < inBlurbs && valI < nvals; i++) {
                saveBlurb = blurb = buffer[i];
                cbits = 0;
                while (true) {
                    if (state == 0) {
                        if (blurb != 0) {
                            for (j = 0; (blurb & BLURB_TOP_BIT_ONE) == 0; j++) blurb <<= 1;
                            msbs += j;
                            // dispose of the unary end bit
                            blurb <<= 1;
                            j++;
                            cbits += j;
                            uval = 0;
                            lsbsLeft = parameter;
                            state++;
                            //totalBitsRead += msbs;
                            if (cbits == BITS_PER_BLURB) {
                                cbits = 0;
                                readCRC16 = CRC16.update(saveBlurb, readCRC16);
                                break;
                            }
                        } else {
                            msbs += BITS_PER_BLURB - cbits;
                            cbits = 0;
                            readCRC16 = CRC16.update(saveBlurb, readCRC16);
                            //totalBitsRead += msbs;
                            break;
                        }
                    } else {
                        int availableBits = BITS_PER_BLURB - cbits;
                        if (lsbsLeft >= availableBits) {
                            uval <<= availableBits;
                            uval |= ((blurb & 0xff) >> cbits);
                            cbits = 0;
                            readCRC16 = CRC16.update(saveBlurb, readCRC16);
                            //totalBitsRead += availableBits;
                            if (lsbsLeft == availableBits) {
                                // compose the value
                                uval |= (msbs << parameter);
                                if ((uval & 1) != 0)
                                    vals[pos + valI++] = -((int) (uval >> 1)) - 1;
                                else
                                    vals[pos + valI++] = (int) (uval >> 1);
                                if (valI == nvals)
                                    break;
                                msbs = 0;
                                state = 0;
                            }
                            lsbsLeft -= availableBits;
                            break;
                        } else {
                            uval <<= lsbsLeft;
                            uval |= ((blurb & 0xff) >> (BITS_PER_BLURB - lsbsLeft));
                            blurb <<= lsbsLeft;
                            cbits += lsbsLeft;
                            //totalBitsRead += lsbsLeft;
                            // compose the value
                            uval |= (msbs << parameter);
                            if ((uval & 1) != 0)
                                vals[pos + valI++] = -((int) (uval >> 1)) - 1;
                            else
                                vals[pos + valI++] = (int) (uval >> 1);
                            if (valI == nvals) {
                                // back up one if we exited the for loop because
                                // we read all nvals but the end came in the
                                // middle of a blurb
                                i--;
                                break;
                            }
                            msbs = 0;
                            state = 0;
                        }
                    }
                }
            }
            consumedBlurbs = i;
            consumedBits = cbits;
            totalConsumedBits = (i << BITS_PER_BLURB_LOG2) | cbits;
            //totalBitsRead += (BITS_PER_BLURB) | cbits;
            if (valI < nvals) {
                long endBits = totalConsumedBits;
                //System.out.println("SE0 "+startBits+" "+endBits);
                totalBitsRead += endBits - startBits;
                readFromStream();
                // these must be zero because we can only get here if we got to
                // the end of the buffer
                i = 0;
                startBits = totalConsumedBits;
            }
        }
        
        long endBits = totalConsumedBits;
        //System.out.println("SE1 "+startBits+" "+endBits);
        totalBitsRead += endBits - startBits;
    }
    
    /**
     * read UTF8 integer.
     * on return, if *val == 0xffffffff then the utf-8 sequence was invalid, but
     * the return value will be true
     * @param raw   The raw bytes read (output). If null, no bytes are returned
     * @return      The integer read
     * @throws IOException  Thrown if error reading input stream
     */
    public int readUTF8Int(ByteSpace raw) throws IOException {
        int val;
        int v = 0;
        int x;
        int i;
        x = readRawUInt(8);
        if (raw != null) raw.space[raw.pos++] = (byte) x;
        if ((x & 0x80) == 0) { // 0xxxxxxx
            v = x;
            i = 0;
        } else if (((x & 0xC0) != 0) && ((x & 0x20) == 0)) { // 110xxxxx
            v = x & 0x1F;
            i = 1;
        } else if (((x & 0xE0) != 0) && ((x & 0x10) == 0)) { // 1110xxxx
            v = x & 0x0F;
            i = 2;
        } else if (((x & 0xF0) != 0) && ((x & 0x08) == 0)) { // 11110xxx
            v = x & 0x07;
            i = 3;
        } else if (((x & 0xF8) != 0) && ((x & 0x04) == 0)) { // 111110xx
            v = x & 0x03;
            i = 4;
        } else if (((x & 0xFC) != 0) && ((x & 0x02) == 0)) { // 1111110x
            v = x & 0x01;
            i = 5;
        } else {
            val = 0xffffffff;
            return val;
        }
        for (; i > 0; i--) {
            x = peekRawUInt(8);
            if (((x & 0x80) == 0) || ((x & 0x40) != 0)) { // 10xxxxxx
                val = 0xffffffff;
                return val;
            }
            x = readRawUInt(8);
            if (raw != null)
                raw.space[raw.pos++] = (byte) x;
            v <<= 6;
            v |= (x & 0x3F);
        }
        val = v;
        return val;
    }
    
    /**
     * read UTF long.
     * on return, if *val == 0xffffffffffffffff then the utf-8 sequence was
     * invalid, but the return value will be true
     * @param raw   The raw bytes read (output). If null, no bytes are returned
     * @return      The long read
     * @throws IOException  Thrown if error reading input stream
     */
    public long readUTF8Long(ByteSpace raw) throws IOException {
        long v = 0;
        int x;
        int i;
        long val;
        x = readRawUInt(8);
        if (raw != null)
            raw.space[raw.pos++] = (byte) x;
        if (((x & 0x80) == 0)) { // 0xxxxxxx
            v = x;
            i = 0;
        } else if (((x & 0xC0) != 0) && ((x & 0x20) == 0)) { // 110xxxxx
            v = x & 0x1F;
            i = 1;
        } else if (((x & 0xE0) != 0) && ((x & 0x10) == 0)) { // 1110xxxx
            v = x & 0x0F;
            i = 2;
        } else if (((x & 0xF0) != 0) && ((x & 0x08) == 0)) { // 11110xxx
            v = x & 0x07;
            i = 3;
        } else if (((x & 0xF8) != 0) && ((x & 0x04) == 0)) { // 111110xx
            v = x & 0x03;
            i = 4;
        } else if (((x & 0xFC) != 0) && ((x & 0x02) == 0)) { // 1111110x
            v = x & 0x01;
            i = 5;
        } else if (((x & 0xFE) != 0) && ((x & 0x01) == 0)) { // 11111110
            v = 0;
            i = 6;
        } else {
            val = 0xffffffffffffffffL;
            return val;
        }
        for (; i > 0; i--) {
            x = peekRawUInt(8);
            if (((x & 0x80) == 0) || ((x & 0x40) != 0)) { // 10xxxxxx
                val = 0xffffffffffffffffL;
                return val;
            }
            x = readRawUInt(8);
            if (raw != null)
                raw.space[raw.pos++] = (byte) x;
            v <<= 6;
            v |= (x & 0x3F);
        }
        val = v;
        return val;
    }
    
    /**
     * Consumed Blurbs.
     * @return Returns the consumedBlurbs.
     */
    public int getConsumedBlurbs() {
        return consumedBlurbs;
    }
    
    /**
     * Total Blurbs read.
     * @return Returns the total blurbs read.
     */
    public int getTotalBlurbsRead() {
        return ((totalBitsRead + 7) / 8);
    }
}
