texfile="proposal"

pdf: clean
	/opt/local/bin/pdflatex ${texfile}
	/opt/local/bin/bibtex ${texfile}
	/opt/local/bin/pdflatex ${texfile}
	/opt/local/bin/pdflatex ${texfile}

clean:
	rm -f *.aux *.dvi *.idx *.log *.mx1 *.mx2 *.ps *.blg *.bbl
