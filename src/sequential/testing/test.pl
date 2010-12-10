#!/usr/bin/perl

if(($#ARGV + 1) != 3) {
    print "usage: test.pl <number of tests> <inputa> <inputb>\n";
    exit 1;
}

print "Starting tests...\n";

$i = 0;
$total = 0;
while($i < $ARGV[0]) {
    $out = `/usr/bin/time ./recursive $ARGV[1] $ARGV[2] 2>&1`;
    $time = -1;
    if($out =~ /(\d+\.\d+)user/) {
	$time = $1;
	$total += $1;
    }
    print "Recursive Test $i: $time seconds\n";
    $i++;
}
print "Recursive Average time: " . $total / $ARGV[0] . " seconds\n";


$i = 0;
$total = 0;
while($i < $ARGV[0]) {
    $out = `/usr/bin/time ./memo $ARGV[1] $ARGV[2] 2>&1`;
    $time = -1;
    if($out =~ /(\d+\.\d+)user/) {
	$time = $1;
	$total += $1;
    }
    print "Recursive w/ Memoization Test $i: $time seconds\n";
    $i++;
}
print "Recursive w/ Memoization Average time: " . $total / $ARGV[0] . " seconds\n";

$i = 0;
$total = 0;
while($i < $ARGV[0]) {
    $out = `/usr/bin/time ./dynamic $ARGV[1] $ARGV[2] 2>&1`;
    $time = -1;
    if($out =~ /(\d+\.\d+)user/) {
	$time = $1;
	$total += $1;
    }
    print "Dynamic Test $i: $time seconds\n";
    $i++;
}
print "Dynamic Average time: " . $total / $ARGV[0] . " seconds\n";

$i = 0;
$total = 0;
while($i < $ARGV[0]) {
    $out = `/usr/bin/time ./hirschberg $ARGV[1] $ARGV[2] 2>&1`;
    $time = -1;
    if($out =~ /(\d+\.\d+)user/) {
	$time = $1;
	$total += $1;
    }
    print "Hirschberg Test $i: $time seconds\n";
    $i++;
}
print "Hirschberg Average time: " . $total / $ARGV[0] . " seconds\n";


