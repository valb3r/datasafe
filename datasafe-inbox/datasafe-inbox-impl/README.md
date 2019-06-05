# Datasafe Inbox Service

This module is designed to provide for message exchange with the user.

We will use CMS (RFC 5652) and S/MIME (RFC 5751) to envelope message exchanged between users.

Our default implementation will use DFS backend to store document exchanged between user. 
We might also use another type of communication backend like SMTP server to provide for the same functionality.

## Sharing file
![Sharing details](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/valb3r/datasafe/develop/docs/diagrams/high-level/inbox_write.puml&fmt=svg&vvv=1&sanitize=true)

## Reading shared file
![Reading shared details](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/valb3r/datasafe/develop/docs/diagrams/high-level/inbox_read.puml&fmt=svg&vvv=1&sanitize=true)