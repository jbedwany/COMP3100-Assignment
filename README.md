# Distributed Systems Client

This project implements a client to interact with a simulated distributed systems server, found at https://github.com/distsys-MQ/ds-sim. It is designed to be run on Linux.

### Running the Simulation

Due to some Java build path weirdness there are some specific instructions to follow to run the demo.
* Clone this repository:
```
git clone https://github.com/jbedwany/COMP3100-Assignment
```
* Clone the ds-sim repository:
```
git clone https://github.com/distsys-MQ/ds-sim
```
* Enter the src directory:
```
cd COMP3100-Assignment/src/
```
* Copy the reference client + server to the directory:
```
cp ../../ds-sim/src/pre-compiled/ds-* .
```
* Recompile the classes if necessary:
```
javac Job.java Server.java
```
* Unpack the test suite:
```
tar xvf S2TestScript.tar
```
* Run the test suite:
```
python3 s2_test.py "java App.java localhost 50000 <username>" -n -r results/ref_results.json
```

## Author
Justin Bedwany 46598634