use HTTP::Daemon;
use HTTP::Status;

my $d = HTTP::Daemon->new(LocalPort => 3003) || die;
print "Please contact me at: <URL:", $d->url, ">\n";
while (my $c = $d->accept) {
    while (my $r = $c->get_request) {
		$c->send_error(503);
		$c->send_header("Retry-After", "43");
    	$c->close;
    }
    undef($c);
}
