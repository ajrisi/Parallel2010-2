set title "Efficiency vs. Processors"
set output 'efficiency_vs_procs.pdf'
set terminal pdf
set key inside left top
set xlabel "Processors"
set ylabel "Efficiency"
set ytics nomirror
set logscale x 2


plot './parallel_results.tsv' using 3:6 index 0 title '512 B inputs' with linespoints,\
     './parallel_results.tsv' using 3:6 index 1 title '1 kB inputs' with linespoints,\
     './parallel_results.tsv' using 3:6 index 2 title '2 kB inputs' with linespoints,\
     './parallel_results.tsv' using 3:6 index 3 title '4 kB inputs' with linespoints,\
     './parallel_results.tsv' using 3:6 index 4 title '8 kB inputs' with linespoints,\
     './parallel_results.tsv' using 3:6 index 6 title '16 kB inputs' with linespoints,\
     './parallel_results.tsv' using 3:6 index 6 title '32 kB inputs' with linespoints,\
     './parallel_results.tsv' using 3:6 index 7 title '64 kB inputs' with linespoints,\
     './parallel_results.tsv' using 3:6 index 8 title '128 kB inputs' with linespoints

