Simple HTTP proxy for messagedb. Mostly an example of using messagedb4s with http4s.

```
/streams/{streamName}/messages
    - GET
        - return all stream messages, in order
        - could take position, batchSize, condition query params too
    - POST/PUT
        - could use this to write new message to stream
    - /last
        - GET
            - returns the stream's last message
    - /first
        - GET
            - returns the stream's first message

/categories/{categoryName}/messages
    - GET
        - return all category messages
        - query params for other params
        - what about unbounded? keep http connection open, chunk new messages back to client
```
