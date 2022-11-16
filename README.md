Simple HTTP proxy for [messagedb](https://github.com/message-db/message-db). Mostly an example of using [messagedb4s](https://github.com/zcox/messagedb4s) with [http4s](https://http4s.org/).

```
/streams/{streamName}/messages
    - GET
        - return all stream messages, in order
        - could take position, batchSize, condition query params too
    - /last
        - GET
            - returns the stream's last message
    - /first
        - GET
            - returns the stream's first message

/streams/{streamName}/messages/{eventId}
    - PUT
        - write new message to stream

/categories/{categoryName}/messages
    - GET
        - return all category messages
        - query params for other params

/categories/{categoryName}/messages/unbounded
    - GET
        - return all category messages from global position zero
        - returns all existing messages first
        - leaves connection open, and returns new messages as they are added
        - uses chunked encoding, each chunk is a json object and a newline
```
