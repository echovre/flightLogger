#To Build:
```
git clone <repo>

mvn -N io.takari:maven:wrapper

maven clean install

```
You'll also need a MongoDB instance for persistence at localhost:27017
 (unauthenticated, default setup should be fine)


#To Run/Test:
```
./mvnw spring-boot:run

curl -X POST -H "Content-Type: application/json" --data @logfile.json http://localhost:8080/log 

curl -X GET http://localhost:8080/log/73f52e9a-247f-43d5-9501-173bff715ba2

curl -X GET http://localhost:8080/log/73f52e9a-247f-43d5-9501-173bff715ba2/batteryconsumed
```
To see database entries:

``` 
mongo

use test

db.transactions.find()
```