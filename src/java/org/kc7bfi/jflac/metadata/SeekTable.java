/*
 * Created on Mar 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.metadata;

import java.io.IOException;

import org.kc7bfi.jflac.io.InputBitStream;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SeekTable extends MetadataBase {
    static final private int SEEKPOINT_LENGTH = 18;
    static final private long SEEKPOINT_PLACEHOLDER = 0xffffffffffffffffL;

    protected SeekPoint[] points;

    public SeekTable(InputBitStream is, boolean isLast, int length) throws IOException {
        super(isLast, length);

        int numPoints = length / SEEKPOINT_LENGTH;

        points = new SeekPoint[numPoints];
        for (int i = 0; i < points.length; i++) {
            points[i] = new SeekPoint(is);
        }
        length -= (length * SEEKPOINT_LENGTH);
        
        // if there is a partial point left, skip over it
        if (length > 0) is.readByteBlockAlignedNoCRC(null, length);
    }

    boolean isLegal() {
        long prevSampleNumber = 0;
        boolean gotPrev = false;

        for (int i = 0; i < points.length; i++) {
            if (gotPrev) {
                if (points[i].sampleNumber != SEEKPOINT_PLACEHOLDER && points[i].sampleNumber <= prevSampleNumber)
                    return false;
            }
            prevSampleNumber = points[i].sampleNumber;
            gotPrev = true;
        }

        return true;
    }

    /* DRR FIX
        int format_seektable_sort(StreamMetadata_SeekTable seek_table) {
            unsigned i, j;
            boolean first;

            // sort the seekpoints
            qsort(
                seek_table - > points,
                seek_table - > num_points,
                sizeof(StreamMetadata_SeekPoint),
                (int (*) (const void *, const void *)) seekpoint_compare_);

            // uniquify the seekpoints
            first = true;
            for (i = j = 0; i < seek_table - > num_points; i++) {
                if (seek_table - > points[i].sample_number != SEEKPOINT_PLACEHOLDER) {
                    if (!first) {
                        if (seek_table - > points[i].sample_number == seek_table - > points[j - 1].sample_number)
                            continue;
                    }
                }
                first = false;
                seek_table - > points[j++] = seek_table - > points[i];
            }

            for (i = j; i < seek_table - > num_points; i++) {
                seek_table - > points[i].sample_number = SEEKPOINT_PLACEHOLDER;
                seek_table - > points[i].stream_offset = 0;
                seek_table - > points[i].frame_samples = 0;
            }

            return j;
        }
        */
}
