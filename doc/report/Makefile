TEXFILE="report"

all: pdf

# This target is needed for vim-latex latex to work.
pdf: clean
	/opt/local/bin/pdflatex ${TEXFILE}
	/opt/local/bin/bibtex ${TEXFILE}
	/opt/local/bin/pdflatex ${TEXFILE}
	/opt/local/bin/pdflatex ${TEXFILE}

clean:
	rm -f *.aux *.dvi *.idx *.log *.mx1 *.mx2 *.ps *.blg *.bbl
