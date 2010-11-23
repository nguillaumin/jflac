#!/usr/bin/perl -w

use File::Find;

find(\&wanted, '.');


sub wanted {
    return if $_ !~ /\.flac$/;

    my $flac = $_;
    my $wav = $flac; $wav =~ s/\.flac$/.wav/;
    my $jflac_wav = "jflac-".$wav;
    my $flac_wav = "lame-".$wav;
    my $ok;

    print "\n\n== Processing $flac\n";

    system("flac --output-name=$flac_wav --decode $flac");
    $ok = system("java -classpath /Projects/SourceForge/jFLAC/target/jflac-0.3.jar org.kc7bfi.jflac.apps.Decoder $flac $jflac_wav") >> 8;
    if ($ok != 0) {
	print "==== JFLAC FAILED $flac\n";
	goto done;
    }
    $ok = system("diff $jflac_wav $flac_wav")<<8;
    if ($ok != 0) {
	print "==== WAV DIFFERENCES $flac\n";
	goto done;
    }

    print "==== PASSED $flac\n";

   done:
    system("rm $jflac_wav $flac_wav"); 
}


