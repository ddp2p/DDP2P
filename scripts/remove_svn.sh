for a in *class; do svn del ${a%.class}.java; done
for a in ../widgets/components/*java; do b=${a%.class}; svn del ${b##*/}; done
