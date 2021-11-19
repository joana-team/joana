Cat Shop
========
A shop to buy cats with an associated wiki. Example for a minimal two server architecture with a cycle.
The Available species command queries the personality via the wiki and the wiki calls the available species command
to check whether a shop has a specific cat. This is a loop that might occur in reality.

Parts
    - `models.yaml`: the shared components and definitions
    - `shop`: shop microservice
    - `wiki`: wiki microservice
    - `build`: builds both the shop and the wiki
    - `./package` build both folders
    - `./start` start servers
    - `sudo docker-compose up`: create a swagger ui at http://localhost:8080
