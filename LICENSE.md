Unfortunately the license story for jFLAC is a bit complicated:

* It's a port of the C++ libFLAC which was initially licensed as LGPL but was
  [relicensed to BSD](https://git.xiph.org/?p=flac.git;a=commitdiff;h=afd8107872c6a877b66957ee192b43530782c6ec)
  when FLAC moved to the Xiph.Org foundation (2003)
* jFLAC started in 2004 where the code was already under BSD and used [the same
  license in its initial commit](https://sourceforge.net/p/jflac/code/ci/1becc7dad6dde3c5a4654956bd983bfb5d3f81e4/tree/LICENSE.txt).
* Unfortunately some original C++ source files still had the LGPL header and
  they were [copied as-is in the Java port](https://sourceforge.net/p/jflac/code/ci/9692a106ee944594774de52ddebec0488f96fc7f/tree/src/java/org/kc7bfi/jflac/StreamDecoder.java)
* jFLAC authors then made more significant changes under the Apache license but then [switched back to LGPL](https://sourceforge.net/p/jflac/code/ci/37604225c116531797efd8e32497e829e9ae73eb/) at some point, probably realizing they couldn't relicense it as Apache.

As a result, it's a bit unclear what license this project should use. My interpretation is that it's LGPL as it was the license headers in the C++ files that the Java port is based on. Please note that I wasn't aware of this situation until v1.5.2 of jFLAC, so version 1.5.0 and 1.5.1 advertised an Apache license as it was the one used by the SourceForge project I forked.

Please see [issue #13](https://github.com/nguillaumin/jflac/issues/13) for more details or if you want to participate in the discussion. Thanks for @Sciss and @alexhk90 for raising this issue and looking into past commits to try to make sense of it.
