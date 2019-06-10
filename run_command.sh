hm=(0 1 2)
vm=(1 2 3)
for h in "${hm[@]}"
do
	for v in "${vm[@]}"
	do
		$ java -cp assign_parsing/assign_parsing.jar:assign_parsing/assign_parsing-submit.jar -server -mx2000m edu.berkeley.nlp.assignments.parsing.PCFGParserTester -path wsj/ -parserType GENERATIVE -quiet -Dhm=$h -Dvm=$v >> log_TrainSize.txt
	done
done

# Use System.getProperty("<name>")
