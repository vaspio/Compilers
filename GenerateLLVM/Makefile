all: compile

compile:
	java -jar ../jtb132di.jar -te minijava.jj
	java -jar ../javacc5.jar minijava-jtb.jj
	javac Main.java

c:
	rm -f *.class *~ *.ll

r:
	make c
	make compile
	java Main Examples/QuickSort.java Examples/MoreThan4.java Examples/LinearSearch.java Examples/Factorial.java Examples/BubbleSort.java