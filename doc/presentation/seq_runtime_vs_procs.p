set title "Runtime vs. Processors"
set output 'seq_runtime_vs_procs.pdf'
set terminal pdf
set key inside right top
set xlabel "Input Size"
set ylabel "Runtime (s)"
set ytics nomirror
set logscale x 2
set logscale y 4

plot './seq_results.tsv' using 2:4 title 'Sequential Run' with linespoints
