package org.kc7bfi.jflac.io;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.kc7bfi.jflac.util.ByteSpace;
import org.kc7bfi.jflac.util.CRC16;
import org.kc7bfi.jflac.util.CRC8;
/*
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
/*
 * Along the way you will see two versions of some functions, selected by a
 * FLAC__NO_MANUAL_INLINING macro. One is the simplified, more readable, and
 * slow version, and the other is the same function where crucial parts have
 * been manually inlined and are much faster.
 *  
 */
/*
 * This should be at least twice as large as the largest number of blurbs
 * required to represent any 'number' (in any encoding) you are going to read.
 * With FLAC this is on the order of maybe a few hundred bits. If the buffer is
 * smaller than that, the decoder won't be able to read in a whole number that
 * is in a variable length encoding (e.g. Rice).
 * 
 * The number we are actually using here is based on what would be the
 * approximate maximum size of a verbatim frame at the default block size, for
 * CD audio (4096 sample * 4 bytes per sample), plus some wiggle room. 32kbytes
 * sounds reasonable. For kicks we subtract out 64 bytes for any alignment or
 * malloc overhead.
 * 
 * Increase this number to decrease the number of read callbacks, at the expense
 * of using more memory. Or decrease for the reverse effect, keeping in mind the
 * limit from the first paragraph.
 */
public class InputBitStream {
    static private final int BITS_PER_BLURB = 8;
    static private final int BITS_PER_BLURB_LOG2 = 3;
    static private final int BYTES_PER_BLURB = 1;
    static private final byte BLURB_ALL_ONES = ((byte) 0xff);
    static private final byte BLURB_TOP_BIT_ONE = ((byte) 0x80);
    //static private final int BLURB_BIT_TO_MASK(b) (((blurb)'\x80') >> (b))
    //static private final int CRC16_UPDATE_BLURB(bb, blurb, crc)
    // CRC16_UPDATE((blurb), (crc));
    //#define FLAC__CRC16_UPDATE(data, crc) (crc) = ((crc)<<8) ^
    // FLAC__crc16_table[((crc)>>8) ^ (data)];
    static private final int BITBUFFER_DEFAULT_CAPACITY = ((65536 - 64) * 8) / BITS_PER_BLURB;
    static final long[] mask32 = new long[]{0, 0x0000000000000001, 0x0000000000000003, 0x0000000000000007, 0x000000000000000F,
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
    //#define BLURBS_TO_BITS(blurbs) ((blurbs) << BITS_PER_BLURB_LOG2)
    private byte[] buffer;
    private int capacity; // in blurbs
    private int blurbs;
    private int bits;
    private int totalBits; /* must always == BITS_PER_BLURB*blurbs+bits */
    private int consumedBlurbs;
    private int consumedBits;
    private int totalConsumedBits; /*
                                    * must always ==
                                    * BITS_PER_BLURB*consumed_blurbs+consumed_bits
                                    */
    private short readCRC16;
    //private blurb save_head, save_tail;
    private InputStream is;
    /*
     * WATCHOUT: The current implentation is not friendly to shrinking, i.e. it
     * does not shift left what is consumed, it just chops off the end, whether
     * there is unconsumed data there or not. This is OK because currently we
     * never shrink the buffer, but if this ever changes, we'll have to do some
     * fixups here.
     */
    private boolean resize(int newCapacity) {
        if (capacity == newCapacity)
            return true;
        byte[] newBuffer = new byte[newCapacity];
        System.arraycopy(buffer, 0, newBuffer, 0, Math.min(blurbs + ((bits != 0) ? 1 : 0), newCapacity));
        if (newCapacity < blurbs + ((bits != 0) ? 1 : 0)) {
            blurbs = newCapacity;
            bits = 0;
            totalBits = newCapacity << 3;
        }
        if (newCapacity < consumedBlurbs + ((consumedBits != 0) ? 1 : 0)) {
            consumedBlurbs = newCapacity;
            consumedBits = 0;
            totalConsumedBits = newCapacity << 3;
        }
        buffer = newBuffer;
        capacity = newCapacity;
        return true;
    }
    private boolean grow(int minBlurbsToAdd) {
        int new_capacity = Math.max(capacity * 2, capacity + minBlurbsToAdd);
        return resize(new_capacity);
    }
    private boolean ensureSize(int bitsToAdd) {
        if ((capacity << 3) < totalBits + bitsToAdd)
            return grow((bitsToAdd >> 3) + 2);
        else
            return true;
    }
    private int readFromStream() throws IOException {
        // first shift the unconsumed buffer data toward the front as much as
        // possible
        if (totalConsumedBits >= BITS_PER_BLURB) {
            int l = 0;
            int r = consumedBlurbs;
            int r_end = blurbs + ((bits != 0) ? 1 : 0);
            for (; r < r_end; l++, r++)
                buffer[l] = buffer[r];
            for (; l < r_end; l++)
                buffer[l] = 0;
            blurbs -= consumedBlurbs;
            totalBits -= consumedBlurbs << 3;
            consumedBlurbs = 0;
            totalConsumedBits = consumedBits;
        }
        // grow if we need to
        if (capacity <= 1) {
            resize(16);
        }
        // set the target for reading, taking into account blurb alignment
        // blurb == byte, so no gyrations necessary:
        int bytes = capacity - blurbs;
        // finally, read in some data
        bytes = is.read(buffer, blurbs, bytes);
        if (bytes <=0 ) throw new EOFException();
        // now we have to handle partial blurb cases:
        // blurb == byte, so no gyrations necessary:
        blurbs += bytes;
        totalBits += bytes << 3;
        return bytes;
    }
    /***************************************************************************
     * 
     * Class constructor/destructor
     *  
     **************************************************************************/
    public InputBitStream(InputStream is) {
        this.is = is;
        init();
    }
    /***************************************************************************
     * 
     * Public class methods
     *  
     **************************************************************************/
    private boolean init() {
        buffer = null;
        capacity = 0;
        blurbs = 0;
        bits = 0;
        totalBits = 0;
        consumedBlurbs = 0;
        consumedBits = 0;
        totalConsumedBits = 0;
        return clear();
    }
    private boolean initFrom(byte[] inBuffer, int bytes) {
        if (!init())
            return false;
        if (!ensureSize(bytes << 3))
            return false;
        System.arraycopy(inBuffer, 0, buffer, 0, bytes);
        blurbs = bytes / BYTES_PER_BLURB;
        bits = (bytes % BYTES_PER_BLURB) << 3;
        totalBits = bytes << 3;
        return true;
    }
    public boolean concatenateAligned(InputBitStream src) {
        int bits_to_add = src.totalBits - src.totalConsumedBits;
        if (bits_to_add == 0)
            return true;
        if (bits != src.consumedBits)
            return false;
        if (!ensureSize(bits_to_add))
            return false;
        if (bits == 0) {
            System.arraycopy(src.buffer, src.consumedBlurbs, buffer, blurbs, (src.blurbs - src.consumedBlurbs + ((src.bits != 0)
                    ? 1
                    : 0)));
        } else if (bits + bits_to_add > BITS_PER_BLURB) {
            buffer[blurbs] <<= (BITS_PER_BLURB - bits);
            buffer[blurbs] |= (src.buffer[src.consumedBlurbs] & ((1 << (BITS_PER_BLURB - bits)) - 1));
            System.arraycopy(src.buffer, src.consumedBlurbs + 1, buffer, blurbs + 11,
                    (src.blurbs - src.consumedBlurbs - 1 + ((src.bits != 0) ? 1 : 0)));
        } else {
            buffer[blurbs] <<= bits_to_add;
            buffer[blurbs] |= (src.buffer[src.consumedBlurbs] & ((1 << bits_to_add) - 1));
        }
        bits = src.bits;
        totalBits += bits_to_add;
        blurbs = totalBits / BITS_PER_BLURB;
        return true;
    }
    public void free() {
        buffer = null;
        capacity = 0;
        blurbs = bits = totalBits = 0;
        consumedBlurbs = consumedBits = totalConsumedBits = 0;
    }
    public boolean clear() {
        if (buffer == null) {
            capacity = BITBUFFER_DEFAULT_CAPACITY;
            buffer = new byte[capacity];
        } else {
            for (int i = 0; i < blurbs + ((bits != 0) ? 1 : 0); i++)
                buffer[i] = 0;
        }
        blurbs = bits = totalBits = 0;
        consumedBlurbs = consumedBits = totalConsumedBits = 0;
        return true;
    }
    /*
     * DRR FIX boolean clone(BitBuffer8 * dest, const BitBuffer * src) {
     * ASSERT(0 != dest); ASSERT(0 != dest - > buffer); ASSERT(0 != src);
     * ASSERT(0 != src - > buffer);
     * 
     * if (dest - > capacity < src - > capacity) if (!bitbuffer_resize_(dest,
     * src - > capacity)) return false; memcpy(dest - > buffer, src - > buffer,
     * sizeof(blurb) * min(src - > capacity, src - > blurbs + 1)); dest - >
     * blurbs = src - > blurbs; dest - > bits = src - > bits; dest - >
     * total_bits = src - > total_bits; dest - > consumed_blurbs = src - >
     * consumed_blurbs; dest - > consumed_bits = src - > consumed_bits; dest - >
     * total_consumed_bits = src - > total_consumed_bits; dest - > read_crc16 =
     * src - > read_crc16; return true; }
     */
    public void resetReadCRC16(short seed) {
        readCRC16 = seed;
    }
    public short getReadCRC16() {
        return readCRC16;
    }
    public short getWriteCRC16() {
        return CRC16.calc(buffer, blurbs);
    }
    public byte getWriteCRC8() {
        return CRC8.calc(buffer, blurbs);
    }
    public boolean isByteAligned() {
        return ((bits & 7) == 0);
    }
    public boolean isConsumedByteAligned() {
        return ((consumedBits & 7) == 0);
    }
    public int bitsLeftForByteAlignment() {
        return 8 - (consumedBits & 7);
    }
    public int getInputBytesUnconsumed() {
        return (totalBits - totalConsumedBits) >> 3;
    }
    /*
     * DRR FIX void get_buffer(const byte * * buffer, unsigned * bytes) {
     * ASSERT((bb - > consumed_bits & 7) == 0 && (bb - > bits & 7) == 0); buffer =
     * bb - > buffer + bb - > consumed_blurbs; bytes = bb - > blurbs - bb - >
     * consumed_blurbs; }
     */
    /*
     * DRR FIX void release_buffer(BitBuffer8 * bb) { (void) bb; }
     */
    public boolean writeZeroes(int bits) {
        if (bits == 0)
            return true;
        if (!ensureSize(bits))
            return false;
        totalBits += bits;
        while (bits > 0) {
            int n = Math.min(BITS_PER_BLURB - bits, bits);
            buffer[blurbs] <<= n;
            bits -= n;
            bits += n;
            if (bits == BITS_PER_BLURB) {
                blurbs++;
                bits = 0;
            }
        }
        return true;
    }
    public boolean write_raw_uint32(int val, int bits) {
        if (bits == 0)
            return true;
        /*
         * inline the size check so we don't incure a function call
         * unnecessarily
         */
        if ((capacity << 3) < totalBits + bits) {
            if (!ensureSize(bits))
                return false;
        }
        /*
         * zero-out unused bits; WATCHOUT: other code relies on this, so this
         * needs to stay
         */
        if (bits < 32)
            /*
             * @@@ gcc seems to require this because the following line causes
             * incorrect results when bits==32; investigate
             */
            val &= (~(0xffffffff << bits)); /* zero-out unused bits */
        totalBits += bits;
        while (bits > 0) {
            int n = BITS_PER_BLURB - bits;
            if (n == BITS_PER_BLURB) { /* i.e. bb->bits == 0 */
                if (bits < BITS_PER_BLURB) {
                    buffer[blurbs] = (byte) val;
                    this.bits = bits;
                    break;
                } else if (bits == BITS_PER_BLURB) {
                    buffer[blurbs++] = (byte) val;
                    break;
                } else {
                    int k = bits - BITS_PER_BLURB;
                    buffer[blurbs++] = (byte) (val >> k);
                    /*
                     * we know k < 32 so no need to protect against the gcc bug
                     * mentioned above
                     */
                    val &= (~(0xffffffff << k));
                    bits -= BITS_PER_BLURB;
                }
            } else if (bits <= n) {
                buffer[blurbs] <<= bits;
                buffer[blurbs] |= val;
                if (bits == n) {
                    blurbs++;
                    bits = 0;
                } else
                    bits += bits;
                break;
            } else {
                int k = bits - n;
                buffer[blurbs] <<= n;
                buffer[blurbs] |= (val >> k);
                /*
                 * we know n > 0 so k < 32 so no need to protect against the gcc
                 * bug mentioned above
                 */
                val &= (~(0xffffffff << k));
                bits -= n;
                blurbs++;
                bits = 0;
            }
        }
        return true;
    }
    public boolean write_raw_int32(int val, int bits) {
        return write_raw_uint32((int) val, bits);
    }
    public boolean write_raw_uint64(long val, int bits) {
        if (bits == 0)
            return true;
        if (!ensureSize(bits))
            return false;
        val &= mask32[bits];
        totalBits += bits;
        while (bits > 0) {
            if (bits == 0) {
                if (bits < BITS_PER_BLURB) {
                    buffer[blurbs] = (byte) val;
                    this.bits = bits;
                    break;
                } else if (bits == BITS_PER_BLURB) {
                    buffer[blurbs++] = (byte) val;
                    break;
                } else {
                    int k = bits - BITS_PER_BLURB;
                    buffer[blurbs++] = (byte) (val >> k);
                    /*
                     * we know k < 64 so no need to protect against the gcc bug
                     * mentioned above
                     */
                    val &= (~(0xffffffffffffffffL << k));
                    bits -= BITS_PER_BLURB;
                }
            } else {
                int n = Math.min(BITS_PER_BLURB - bits, bits);
                int k = bits - n;
                buffer[blurbs] <<= n;
                buffer[blurbs] |= (val >> k);
                /*
                 * we know n > 0 so k < 64 so no need to protect against the gcc
                 * bug mentioned above
                 */
                val &= (~(0xffffffffffffffffL << k));
                bits -= n;
                bits += n;
                if (bits == BITS_PER_BLURB) {
                    blurbs++;
                    bits = 0;
                }
            }
        }
        return true;
    }
    public boolean write_raw_uint32_little_endian(int val) {
        /*
         * this doesn't need to be that fast as currently it is only used for
         * vorbis comments
         */
        /*
         * NOTE: we rely on the fact that write_raw_uint32() masks out the
         * unused bits
         */
        if (!write_raw_uint32(val, 8))
            return false;
        if (!write_raw_uint32(val >> 8, 8))
            return false;
        if (!write_raw_uint32(val >> 16, 8))
            return false;
        if (!write_raw_uint32(val >> 24, 8))
            return false;
        return true;
    }
    public boolean write_byte_block(byte[] vals, int nvals) {
        /* this could be faster but currently we don't need it to be */
        for (int i = 0; i < nvals; i++) {
            if (!write_raw_uint32((int) (vals[i]), 8))
                return false;
        }
        return true;
    }
    public boolean write_unary_unsigned(int val) {
        if (val < 32)
            return write_raw_uint32(1, ++val);
        else if (val < 64)
            return write_raw_uint64(1, ++val);
        else {
            if (!writeZeroes(val))
                return false;
            return write_raw_uint32(1, 1);
        }
    }
    public int rice_bits(int val, int parameter) {
        int msbs, uval;
        /* fold signed to unsigned */
        if (val < 0)
            /*
             * equivalent to (unsigned)(((--val) < < 1) - 1); but without the
             * overflow problem at MININT
             */
            uval = (int) (((-(++val)) << 1) + 1);
        else
            uval = (int) (val << 1);
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
    public boolean write_rice_signed(int val, int parameter) {
        int total_bits, interesting_bits, msbs, uval;
        int pattern;
        /* fold signed to unsigned */
        if (val < 0)
            /*
             * equivalent to (unsigned)(((--val) < < 1) - 1); but without the
             * overflow problem at MININT
             */
            uval = (int) (((-(++val)) << 1) + 1);
        else
            uval = (int) (val << 1);
        msbs = uval >> parameter;
        interesting_bits = 1 + parameter;
        total_bits = interesting_bits + msbs;
        pattern = 1 << parameter; /* the unary end bit */
        pattern |= (uval & ((1 << parameter) - 1)); /* the binary LSBs */
        if (total_bits <= 32) {
            if (!write_raw_uint32(pattern, total_bits))
                return false;
        } else {
            /* write the unary MSBs */
            if (!writeZeroes(msbs))
                return false;
            /* write the unary end bit and binary LSBs */
            if (!write_raw_uint32(pattern, interesting_bits))
                return false;
        }
        return true;
    }
    public boolean write_utf8_uint32(int val) {
        boolean ok = true;
        if (val < 0x80) {
            return write_raw_uint32(val, 8);
        } else if (val < 0x800) {
            ok &= write_raw_uint32(0xC0 | (val >> 6), 8);
            ok &= write_raw_uint32(0x80 | (val & 0x3F), 8);
        } else if (val < 0x10000) {
            ok &= write_raw_uint32(0xE0 | (val >> 12), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (val & 0x3F), 8);
        } else if (val < 0x200000) {
            ok &= write_raw_uint32(0xF0 | (val >> 18), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 12) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (val & 0x3F), 8);
        } else if (val < 0x4000000) {
            ok &= write_raw_uint32(0xF8 | (val >> 24), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 18) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 12) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (val & 0x3F), 8);
        } else {
            ok &= write_raw_uint32(0xFC | (val >> 30), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 24) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 18) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 12) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (val & 0x3F), 8);
        }
        return ok;
    }
    public boolean write_utf8_uint64(long val) {
        boolean ok = true;
        if (val < 0x80) {
            return write_raw_uint32((int) val, 8);
        } else if (val < 0x800) {
            ok &= write_raw_uint32(0xC0 | (int) (val >> 6), 8);
            ok &= write_raw_uint32(0x80 | (int) (val & 0x3F), 8);
        } else if (val < 0x10000) {
            ok &= write_raw_uint32(0xE0 | (int) (val >> 12), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) (val & 0x3F), 8);
        } else if (val < 0x200000) {
            ok &= write_raw_uint32(0xF0 | (int) (val >> 18), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 12) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) (val & 0x3F), 8);
        } else if (val < 0x4000000) {
            ok &= write_raw_uint32(0xF8 | (int) (val >> 24), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 18) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 12) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) (val & 0x3F), 8);
        } else if (val < 0x80000000) {
            ok &= write_raw_uint32(0xFC | (int) (val >> 30), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 24) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 18) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 12) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) (val & 0x3F), 8);
        } else {
            ok &= write_raw_uint32(0xFE, 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 30) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 24) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 18) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 12) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) ((val >> 6) & 0x3F), 8);
            ok &= write_raw_uint32(0x80 | (int) (val & 0x3F), 8);
        }
        return ok;
    }
    public boolean zero_pad_to_byte_boundary() {
        /* 0-pad to byte boundary */
        if ((bits & 7) != 0)
            return writeZeroes(8 - (bits & 7));
        else
            return true;
    }
    /*
     * DRR FIX boolean peek_bit(unsigned * val, boolean(* read_callback) (byte
     * buffer[], unsigned * bytes, void * client_data), void * client_data) {
     * 
     * while (1) { if (total_consumed_bits < total_bits) { val =
     * (buffer[consumed_blurbs] & BLURB_BIT_TO_MASK(consumed_bits)) ? 1 : 0;
     * return true; } else { if (!read_from_client_(bb, read_callback,
     * client_data)) return false; } } }
     */
    public void skipBitsNoCRC(int bits) throws IOException {
        if (bits > 0) {
            int n = consumedBits & 7;
            int m;
            int x;
            if (n != 0) {
                m = Math.min(8 - n, bits);
                x = readRawUInt(m);
                bits -= m;
            }
            m = bits / 8;
            if (m > 0) {
                readByteBlockAlignedNoCRC(null, m);
                bits %= 8;
            }
            if (bits > 0) {
                x = readRawUInt(bits);
            }
        }
    }
    int readBit() throws IOException {
        //  BitBuffer8 * bb,
        //  unsigned * val,
        //  boolean(* read_callback) (byte buffer[], unsigned * bytes, void *
        // client_data),
        //  void * client_data) {
        int val;
        while (true) {
            if (totalConsumedBits < totalBits) {
                val = ((buffer[consumedBlurbs] & (0x80 >> consumedBits)) != 0) ? 1 : 0;
                consumedBits++;
                if (consumedBits == BITS_PER_BLURB) {
                    readCRC16 = CRC16.update(buffer[consumedBlurbs], readCRC16);
                    //read_crc16 = (read_crc16<<8) ^
                    // CRC.crc16_table[(read_crc16>>8) ^
                    // buffer[consumed_blurbs]];
                    consumedBlurbs++;
                    consumedBits = 0;
                }
                totalConsumedBits++;
                return val;
            } else {
                readFromStream();
            }
        }
    }
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
                return val;
            } else {
                readFromStream();
            }
        }
    }
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
                return val;
            } else {
                readFromStream();
            }
        }
    }
    public int readRawUInt(int bits) throws IOException {
        int val = 0;
        for (int i = 0; i < bits; i++) {
            val = readBitToInt(val);
        }
        return val;
    }
    public int peekRawUInt(int bits) throws IOException {
        int val = 0;
        for (int i = 0; i < bits; i++) {
            val = peekBitToInt(val, i);
        }
        return val;
    }
    public int readRawInt(int bits) throws IOException { //boolean(*
        //read_callback) (byte buffer[], unsigned * bytes, void * client_data),
        //void * client_data) # ifdef NO_MANUAL_INLINING { int i; int v; int
        // val;
        if (bits == 0) { return 0; }
        int val;
        int v = 0;
        for (int i = 0; i < bits; i++) {
            v = readBitToInt(v);
        }
        // fix the sign
        int i = 32 - bits;
        //System.out.print("ReadInt i="+i+" v="+Integer.toHexString(v));
        if (i != 0) {
            v <<= i;
            val = (int) v;
            val >>= i;
        } else
            val = (int) v;
        //System.out.println(" val="+val);
        return val;
    }
    
    public long readRawLong(int bits) throws IOException {
        //boolean(* read_callback) (byte buffer[], unsigned * bytes, void *
        // client_data),
        //void * client_data) # ifdef NO_MANUAL_INLINING {
        long val = 0;
        for (int i = 0; i < bits; i++) {
            val = readBitToLong(val);
        }
        return val;
    }
    public int readRawIntLittleEndian() throws IOException {
        int x8 = 0;
        int x32 = 0;
        // this doesn't need to be that fast as currently it is only used for
        // vorbis comments
        x32 = readRawUInt(8);
        x8 = readRawUInt(8);
        x32 |= (x8 << 8);
        x8 = readRawUInt(8);
        x32 |= (x8 << 16);
        x8 = readRawUInt(8);
        x32 |= (x8 << 24);
        return x32;
    }
    public void readByteBlockAlignedNoCRC(byte[] val, int nvals) throws IOException {
        //boolean(* read_callback) (byte buffer[], unsigned * bytes, void *
        // client_data),
        //void * client_data) {
        //byte[] val = new byte[nvals];
        while (nvals > 0) {
            int chunk = Math.min(nvals, blurbs - consumedBlurbs);
            if (chunk == 0) {
                readFromStream();
            } else {
                if (val != null)
                    System.arraycopy(buffer, consumedBlurbs, val, 0, BYTES_PER_BLURB * chunk);
                //val += BYTES_PER_BLURB * chunk;
                //}
                nvals -= chunk;
                consumedBlurbs += chunk;
                totalConsumedBits = (consumedBlurbs << BITS_PER_BLURB_LOG2);
            }
        }
    }
    public int readUnaryUnsigned() throws IOException {
        //BitBuffer * bb,
        //unsigned * val,
        //boolean(* read_callback) (byte buffer[], unsigned * bytes, void *
        // client_data),
        //void * client_data) # ifdef NO_MANUAL_INLINING {
        int bit, val_ = 0;
        while (true) {
            bit = readBit();
            //   return false;
            if (bit != 0)
                break;
            val_++;
        }
        //* val = val_;
        return val_;
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
    public void readRiceSignedBlock(int vals[], int pos, int nvals, int parameter) throws IOException {
        //boolean(* read_callback) (byte buffer[], unsigned * bytes, void *
        // client_data),
        //void * client_data) {
        //const blurb * buffer = buffer;
        int i, j, val_i = 0;
        int cbits = 0, uval = 0, msbs = 0, lsbs_left = 0;
        byte blurb, save_blurb;
        int state = 0; // 0 = getting unary MSBs, 1 = getting binary LSBs
        if (nvals == 0) return;
        i = consumedBlurbs;
        // We unroll the main loop to take care of partially consumed blurbs
        // here.
        if (consumedBits > 0) {
            save_blurb = blurb = buffer[i];
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
                        lsbs_left = parameter;
                        state++;
                        if (cbits == BITS_PER_BLURB) {
                            cbits = 0;
                            readCRC16 = CRC16.update(save_blurb, readCRC16);
                            break;
                        }
                    } else {
                        msbs += BITS_PER_BLURB - cbits;
                        cbits = 0;
                        readCRC16 = CRC16.update(save_blurb, readCRC16);
                        break;
                    }
                } else {
                    int available_bits = BITS_PER_BLURB - cbits;
                    if (lsbs_left >= available_bits) {
                        uval <<= available_bits;
                        uval |= ((blurb & 0xff) >> cbits);
                        cbits = 0;
                        readCRC16 = CRC16.update(save_blurb, readCRC16);
                        if (lsbs_left == available_bits) {
                            // compose the value
                            uval |= (msbs << parameter);
                            if ((uval & 1) != 0)
                                vals[pos+val_i++] = -((int) (uval >> 1)) - 1;
                            else
                                vals[pos+val_i++] = (int) (uval >> 1);
                            if (val_i == nvals)
                                break;
                            msbs = 0;
                            state = 0;
                        }
                        lsbs_left -= available_bits;
                        break;
                    } else {
                        uval <<= lsbs_left;
                        uval |= ((blurb & 0xff) >> (BITS_PER_BLURB - lsbs_left));
                        blurb <<= lsbs_left;
                        cbits += lsbs_left;
                        // compose the value
                        uval |= (msbs << parameter);
                        if ((uval & 1) != 0)
                            vals[pos+val_i++] = -((int) (uval >> 1)) - 1;
                        else
                            vals[pos+val_i++] = (int) (uval >> 1);
                        if (val_i == nvals) {
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
        }
        // Now that we are blurb-aligned the logic is slightly simpler
        while (val_i < nvals) {
            for (; i < blurbs && val_i < nvals; i++) {
                save_blurb = blurb = buffer[i];
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
                            lsbs_left = parameter;
                            state++;
                            if (cbits == BITS_PER_BLURB) {
                                cbits = 0;
                                readCRC16 = CRC16.update(save_blurb, readCRC16);
                                break;
                            }
                        } else {
                            msbs += BITS_PER_BLURB - cbits;
                            cbits = 0;
                            readCRC16 = CRC16.update(save_blurb, readCRC16);
                            break;
                        }
                    } else {
                        int available_bits = BITS_PER_BLURB - cbits;
                        if (lsbs_left >= available_bits) {
                            uval <<= available_bits;
                            uval |= ((blurb & 0xff) >> cbits);
                            cbits = 0;
                            readCRC16 = CRC16.update(save_blurb, readCRC16);
                            if (lsbs_left == available_bits) {
                                // compose the value
                                uval |= (msbs << parameter);
                                if ((uval & 1) == 1)
                                    vals[pos+val_i++] = -((int) (uval >> 1)) - 1;
                                else
                                    vals[pos+val_i++] = (int) (uval >> 1);
                                if (val_i == nvals)
                                    break;
                                msbs = 0;
                                state = 0;
                            }
                            lsbs_left -= available_bits;
                            break;
                        } else {
                            uval <<= lsbs_left;
                            uval |= ((blurb & 0xff) >> (BITS_PER_BLURB - lsbs_left));
                            blurb <<= lsbs_left;
                            cbits += lsbs_left;
                            // compose the value
                            uval |= (msbs << parameter);
                            if ((uval & 1) != 0)
                                vals[pos+val_i++] = -((int) (uval >> 1)) - 1;
                            else
                                vals[pos+val_i++] = (int) (uval >> 1);
                            if (val_i == nvals) {
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
            if (val_i < nvals) {
                readFromStream();
                // these must be zero because we can only get here if we got to
                // the end of the buffer
                i = 0;
            }
        }
    }
    /*
     * on return, if *val == 0xffffffff then the utf-8 sequence was invalid, but
     * the return value will be true
     */
    public int readUTF8Int(ByteSpace raw) throws IOException {
        int val;
        int v = 0;
        int x;
        int i;
        x = readRawUInt(8);
        if (raw != null)
            raw.space[raw.pos++] = (byte) x;
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
    /*
     * on return, if *val == 0xffffffffffffffff then the utf-8 sequence was
     * invalid, but the return value will be true
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
    /*
     * DRR FIX void dump(const BitBuffer * bb, FILE * out) { unsigned i, j; if
     * (bb == 0) { fprintf(out, "bitbuffer is NULL\n"); } else { fprintf( out,
     * "bitbuffer: capacity=%u blurbs=%u bits=%u total_bits=%u consumed:
     * blurbs=%u, bits=%u, total_bits=%u\n", capacity, blurbs, bits, total_bits,
     * consumed_blurbs, consumed_bits, total_consumed_bits);
     * 
     * for (i = 0; i < blurbs; i++) { fprintf(out, "%08X: ", i); for (j = 0; j <
     * BITS_PER_BLURB; j++) if (i * BITS_PER_BLURB + j < total_consumed_bits)
     * fprintf(out, "."); else fprintf(out, "%01u", buffer[i] & (1 < <
     * (BITS_PER_BLURB - j - 1)) ? 1 : 0); fprintf(out, "\n"); } if (bits > 0) {
     * fprintf(out, "%08X: ", i); for (j = 0; j < bits; j++) if (i *
     * BITS_PER_BLURB + j < total_consumed_bits) fprintf(out, "."); else
     * fprintf(out, "%01u", buffer[i] & (1 < < (bits - j - 1)) ? 1 : 0);
     * fprintf(out, "\n"); } } }
     */
}