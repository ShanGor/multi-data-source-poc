# To support multiple database.

Only support Spring MVC for this implementation because it is using ThreadLocal. While Spring MVC handles a request with a independent thread.

If you are using Webflux, you need might force the request to pass Correlation-Id, for request management.

## Background
We need to support different data sources for different entities, to fulfill the data segregation requirement.

Now we have below entities to support:
- E1: Entity 1
- E2: Entity 2
- E3: Entity 3

## Technical Details
For Spring Boot, you have to specify a primary data source. Now let's assume E1 is the primary.

## Testing Commands:
### Before Start
You need to create 3 databases, configure the `application.yaml`, then create tables according to `src/main/resources/schema.sql`
### Go
The same database, and the same table, stored different data.
- E1: `insert into test values (1, 'test1');`
- E2: `insert into test values (1, 'test2');`
- E3: `insert into test values (1, 'test3');`


### Trigger for E1
```
> curl -i http://localhost:8080/users/1 -H "X-Entity:E1"
HTTP/1.1 200
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 10 Dec 2021 10:06:26 GMT

{"id":1,"username":"test1"}
```

### Trigger for E2  
```
> curl -i http://localhost:8080/users/1 -H "X-Entity:E2"
HTTP/1.1 200
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 10 Dec 2021 10:06:31 GMT

{"id":1,"username":"test2"}
```
### Trigger for E3
```
> curl -i http://localhost:8080/users/1 -H "X-Entity:E3"
HTTP/1.1 200
Content-Type: application/json
Transfer-Encoding: chunked
Date: Fri, 10 Dec 2021 10:06:23 GMT

{"id":1,"username":"test3"}
```
### Trigger for nothing
```
> curl -i http://localhost:8080/users/1 -H "X-Entity:E4"
HTTP/1.1 400
Transfer-Encoding: chunked
Date: Fri, 10 Dec 2021 10:06:14 GMT
Connection: close

Unknown Entity in X-Entity: E4
```

