package org.kc7bfi.jflac.sound.spi;

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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
 * A format conversion provider provides format conversion services from one or
 * more input formats to one or more output formats. Converters include codecs,
 * which encode and/or decode audio data, as well as transcoders, etc.
 * Format converters provide methods for determining what conversions are
 * supported and for obtaining an audio stream from which converted data can be
 * read.
 * 
 * The source format represents the format of the incoming audio data, which
 * will be converted.
 * 
 * The target format represents the format of the processed, converted audio
 * data. This is the format of the data that can be read from the stream
 * returned by one of the getAudioInputStream methods.
 * 
 * @author Marc Gimpel, Wimba S.A. (marc@wimba.com)
 * @version $Revision: 1.3 $
 */
public class FlacFormatConvertionProvider extends FormatConversionProvider {

    /** */
    private static final AudioFormat.Encoding[] NO_ENCODING = {};

    /** */
    private static final AudioFormat.Encoding[] PCM_ENCODING = { AudioFormat.Encoding.PCM_SIGNED };

    /** */
    private static final AudioFormat.Encoding[] FLAC_ENCODING = { FlacEncoding.FLAC };

    /** */
    private static final AudioFormat.Encoding[] BOTH_ENCODINGS = {
            FlacEncoding.FLAC, 
            AudioFormat.Encoding.PCM_SIGNED 
    };

    /** */
    private static final AudioFormat[] NO_FORMAT = {};

    /**
     * Obtains the set of source format encodings from which format conversion
     * services are provided by this provider.
     * @return array of source format encodings.
     * The array will always have a length of at least 1.
     */
    public AudioFormat.Encoding[] getSourceEncodings() {
        AudioFormat.Encoding[] encodings = { 
                FlacEncoding.FLAC,
                AudioFormat.Encoding.PCM_SIGNED 
                };
        return encodings;
    }

    /**
     * Obtains the set of target format encodings to which format conversion
     * services are provided by this provider.
     * @return array of target format encodings.
     * The array will always have a length of at least 1.
     */
    public AudioFormat.Encoding[] getTargetEncodings() {
        AudioFormat.Encoding[] encodings = { 
                FlacEncoding.FLAC, 
                AudioFormat.Encoding.PCM_SIGNED 
        };
        return encodings;
    }

    /**
     * Obtains the set of target format encodings supported by the format
     * converter given a particular source format. If no target format encodings
     * are supported for this source format, an array of length 0 is returned.
     * @param sourceFormat format of the incoming data.
     * @return array of supported target format encodings.
     */
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            AudioFormat.Encoding[] encodings = { FlacEncoding.FLAC };
            return encodings;
        } else if (sourceFormat.getEncoding() instanceof FlacEncoding) {
            AudioFormat.Encoding[] encodings = { AudioFormat.Encoding.PCM_SIGNED };
            return encodings;
        } else {
            AudioFormat.Encoding[] encodings = {};
            return encodings;
        }
    }

    /**
     * Obtains the set of target formats with the encoding specified supported by
     * the format converter. If no target formats with the specified encoding are
     * supported for this source format, an array of length 0 is returned.
     * @param targetEncoding desired encoding of the outgoing data.
     * @param sourceFormat format of the incoming data.
     * @return array of supported target formats.
     */
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetEncoding instanceof FlacEncoding) {
            if (sourceFormat.getChannels() > 2 || sourceFormat.getChannels() <= 0 || sourceFormat.isBigEndian()) {
                AudioFormat[] formats = {};
                return formats;
            } else {
                AudioFormat[] formats = { new AudioFormat(targetEncoding,
                        sourceFormat.getSampleRate(), -1, // sample size in bits
                        sourceFormat.getChannels(), -1, // frame size
                        -1, // frame rate
                        false) }; // little endian
                return formats;
            }
        } else if (sourceFormat.getEncoding() instanceof FlacEncoding && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            AudioFormat[] formats = { new AudioFormat(sourceFormat
                    .getSampleRate(), 16, // sample size in bits
                    sourceFormat.getChannels(), true, // signed
                    false) }; // little endian (for PCM wav)
            return formats;
        } else {
            AudioFormat[] formats = {};
            return formats;
        }
    }

    /**
     * Obtains an audio input stream with the specified encoding from the given
     * audio input stream.
     * @param targetEncoding - desired encoding of the stream after processing.
     * @param sourceStream - stream from which data to be processed should be read.
     * @return stream from which processed data with the specified target
     * encoding may be read.
     */
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        if (isConversionSupported(targetEncoding, sourceStream.getFormat())) {
            AudioFormat[] formats = getTargetFormats(targetEncoding, sourceStream.getFormat());
            if (formats != null && formats.length > 0) {
                AudioFormat sourceFormat = sourceStream.getFormat();
                AudioFormat targetFormat = formats[0];
                if (sourceFormat.equals(targetFormat)) {
                    return sourceStream;
                } else if (sourceFormat.getEncoding() instanceof FlacEncoding && targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    //try { sourceStream.reset(); } catch (Exception e) {System.out.println("Reset: "+e);}
                    return new Flac2PcmAudioInputStream(sourceStream, targetFormat, -1);
                } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof FlacEncoding) {
                    //return new Pcm2FlacAudioInputStream(sourceStream, targetFormat, -1);
                    throw new IllegalArgumentException("unable to convert " + sourceFormat.toString() + " to " + targetFormat.toString());
                } else {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat.toString() + " to " + targetFormat.toString());
                }
            } else {
                throw new IllegalArgumentException("target format not found");
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }

    /**
     * Obtains an audio input stream with the specified format from the given
     * audio input stream.
     * @param targetFormat - desired data format of the stream after processing.
     * @param sourceStream - stream from which data to be processed should be read.
     * @return stream from which processed data with the specified format may be
     * read.
     */
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        if (isConversionSupported(targetFormat, sourceStream.getFormat())) {
            AudioFormat[] formats = getTargetFormats(
                    targetFormat.getEncoding(), sourceStream.getFormat());
            if (formats != null && formats.length > 0) {
                AudioFormat sourceFormat = sourceStream.getFormat();
                if (sourceFormat.equals(targetFormat)) {
                    return sourceStream;
                } else if (sourceFormat.getEncoding() instanceof FlacEncoding
                        && targetFormat.getEncoding().equals(
                                AudioFormat.Encoding.PCM_SIGNED)) {
                    return new Flac2PcmAudioInputStream(sourceStream,
                            targetFormat, -1);
                } else if (sourceFormat.getEncoding().equals(
                        AudioFormat.Encoding.PCM_SIGNED)
                        && targetFormat.getEncoding() instanceof FlacEncoding) {
                    //return new Pcm2FlacAudioInputStream(sourceStream, targetFormat, -1);
                    throw new IllegalArgumentException("unable to convert "
                            + sourceFormat.toString() + " to "
                            + targetFormat.toString());
               } else {
                    throw new IllegalArgumentException("unable to convert "
                            + sourceFormat.toString() + " to "
                            + targetFormat.toString());
                }
            } else {
                throw new IllegalArgumentException("target format not found");
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }
}
