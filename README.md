# actor-selection
test sporadic actor selection failures (cf. https://github.com/akka/akka/issues/18149)

The test requires a running MongoDb (localhost:27017) and creates a database called "actor-selection".

Run ```mvn verify``` in order to see the sporadic failure.
