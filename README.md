# Distributed Systems Client

This project implements a client to interact with a simulated distributed systems server, found at https://github.com/distsys-MQ/ds-sim. It is designed to be run on Linux.

### Running the Simulation

* Clone this repository:
```
git clone https://github.com/jbedwany/COMP3100-Assignment
```
* Clone the ds-sim repository:
```
git clone https://github.com/distsys-MQ/ds-sim
```
* Compile the classes if necessary:
```
javac COMP3100-Assignment/src/*.java COMP3100-Assignment/client.java
```
* Run the ds-sim server with the config file of your choosing. Make sure to include the -n flag to allow proper reading of newlines.
```
ds-sim/src/pre-compiled/ds-server localhost 50000 -n -c ds-sim/configs/sample-configs/sample-config01.xml
```
* Run this client with the IP, port, and user to authenticate as. Note in the below example, 'n' is the user.
```
cd COMP3100-Assignment
java client localhost 50000 n
```
## Author
Justin Bedwany 46598634