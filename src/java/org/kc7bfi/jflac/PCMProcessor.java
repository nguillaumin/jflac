/*
 * Created on Jun 2, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac;

import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteSpace;

/**
 * @author kc7bfi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface PCMProcessor {
	
	public void processStreamInfo(StreamInfo streamInfo);
	public void processPCM(ByteSpace pcm);
}
