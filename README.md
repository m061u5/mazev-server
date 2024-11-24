# Server and Client prompt

```text
Write server and client in java 21.
Server requirements:
- creates server socket
- operates in endless loop:
    - accept connection
    - creates commands processing thread to process client commands
    - creates client connection thread per client connection
- do not keep list of created threads

Client connection thread requirements:
- waits for authorization message
- sends unauthorized message if key is incorrect and exits
- sends authorized message
- creates client command thread per client connection
- creates client state thread per client connection

Client command thread requirements:
- operates in endless loop:
    - receives command
    - sends to commands processing thread

Client state thread requirements:
- operates in endless loop:
    - sends state to client when notified

Command processing thread:
- operates in endless loop:
    - processes all collected commands at once every one second
    - calculates new state using client commands
    - updates state
    - always notifies client state threads using condition or other lock

Client requirements:
- connects to server
- sends authorization message with authorization key
- exits when receives unauthorized message
- continue when receives authorized message
- then in the endless loop:
    - receives state
    - calculates new command
    - sends command to server

Command requirements:
- use sealed interface to represent all variants
- use java records
- Increment command
- Decrement command

State requirements:
- use java records
- keeps value

Common requirements:
- use json
- must not use synchronized
- use local type inference with final
- use switch expression
- use slf4j logger
- use virtual threads
```