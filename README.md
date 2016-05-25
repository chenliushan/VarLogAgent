# VarLogAgent

This is the component of the AfTool.
Main function: Modify the Target program's byte code when it is loading 
and log the target program's variables value at runtime.

Note: It relys on the AfTool 's first step's tem output 
-- the accessible variables for very line in json format 
(which should locate at path: <code>tmp/firstStepOut.json</code>)