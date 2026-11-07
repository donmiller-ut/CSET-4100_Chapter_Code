# Chapter 11 demonstration walkthrough

Use the standalone `ch11-maintainable-server-architecture` folder. Open its
`pom.xml` level in VS Code. This continuous demonstration uses native PostgreSQL 18,
Java 25, the Maven Wrapper, Bash, and curl. Neither Docker nor Bruno is required
for preparation or delivery. The optional `bruno-cart` collection is a
separate rehearsal of the unchanged Chapter 10 workflow.

This is an online lecture. Explain what you are checking, perform the action,
and interpret the result without waiting for student responses. Installation,
downloads, database initialization, and password entry happen before recording.

## Part 1: Preparation, before the lecture

1. Follow the project-root `README.md` through database initialization, passwords,
   environment variables, and `./mvnw clean verify`. Use the new disposable
   PostgreSQL cluster on port **5436**, not a retained Chapter 10 database.
2. Confirm the build finishes with **81 tests**, zero failures, errors, or skips.
   The tests restore Customer 100's Cart 200 to version 0 and total quantity 1.
   The one item is Product 42, quantity 1. Do not run fixture-resetting tests while
   the application is running.
3. In VS Code, open two Bash terminals at the project root. Name them **App** and
   **Requests**. If a terminal starts in Zsh, enter `bash` as its own command.
4. App needs the README's exported JDBC URLs and two password variables. Requests
   needs no database password for curl. Never show a password or print the environment.
5. In App, ensure the default capacity applies, then start the built JAR:

```bash
unset NORTHSTAR_CART_MAX_TOTAL_QUANTITY NORTHSTAR_SHOW_SQL
java -jar target/northstar-market-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1
```

6. Wait for `Started NorthstarMarketApplication`. In Requests, check health and
   the fixture. Run each command separately:

```bash
curl -i http://127.0.0.1:8080/actuator/health
```

```bash
curl -i http://127.0.0.1:8080/api/customers/100/cart
```

Expect HTTP 200, health `UP`, and cart `cartId: 200`, `customerId: 100`,
`version: 0`, `totalQuantity: 1`. If the cart differs, stop Java with Ctrl+C,
run `./mvnw -Dtest=CartPersistenceIntegrationTests test` in the configured App
terminal on this disposable lab, then restart the JAR. Do not guess version values
to force the script through. Do not delete a database or rewrite migrations.

7. Open these files as editor tabs. All paths below are relative to the project
   root. Collapse imports only after pointing out their dependency direction.

| Tab | File |
| --- | --- |
| Controller | `src/main/java/com/example/northstarmarket/cart/controller/CartController.java` |
| Use-case contract | `src/main/java/com/example/northstarmarket/cart/service/CartOperations.java` |
| Service | `src/main/java/com/example/northstarmarket/cart/service/CartService.java` |
| Mapper | `src/main/java/com/example/northstarmarket/cart/service/CartMapper.java` |
| Repository | `src/main/java/com/example/northstarmarket/cart/repository/ShoppingCartRepository.java` |
| Typed settings | `src/main/java/com/example/northstarmarket/config/NorthstarProperties.java` |
| Registration | `src/main/java/com/example/northstarmarket/NorthstarMarketApplication.java` |
| Settings values | `src/main/resources/application.properties` |
| Correlation filter | `src/main/java/com/example/northstarmarket/web/RequestIdFilter.java` |
| Service tests | `src/test/java/com/example/northstarmarket/cart/service/CartServiceTests.java` |
| Persistence tests | `src/test/java/com/example/northstarmarket/cart/CartPersistenceIntegrationTests.java` |

**Preparation ends here. Begin the live demonstration on slide 12.**

## Part 2: One continuous demonstration, slide 12

Allow about 22 minutes, including Java restarts and focused tests. Stay in VS Code
through all four phases. Return to slide 13 only after the final test completes.
Slides 7 and 9 are worked examples, not instructions to start separate demos.

| Phase | Approximate time | End checkpoint |
| --- | --- | --- |
| 1. Trace and update | 5 minutes | Total 3, version 1 |
| 2. Configure the same JAR | 6 minutes | Total 3, version 1; default restored |
| 3. Correlate and verify | 6 minutes | Total 13, version 2; rejected item absent |
| 4. Run focused tests | 5 minutes | 9 focused tests + 1 rollback test pass; seed restored |

These times are rehearsal estimates. Wait for startup and test completion rather
than rushing to meet the estimate. Do not reset the cart between phases 1–3.

### Phase 1: Follow one use case

**Starting state:** App is running with limit 20. Cart version 0, total 1.
The purpose is to connect responsibility boundaries
to an unchanged HTTP interaction, not to narrate every import.

1. Show the cart package in VS Code. Expand `controller`, `dto`, `service`,
   `repository`, `model`, and `exception`. Explain that the feature stays together;
   its responsibilities now have explicit locations. Product remains in its
   earlier flat package because this refactor is limited to the cart.
2. Open `CartController`. Follow the PUT mapping to `CartOperations`. Point out
   path and body validation, delegation, and the returned DTO. There is no JPA
   repository in this controller. Chapter 10 already separated the use case;
   Chapter 11 makes those boundaries easier to inspect and test.
3. Open `CartService.setItemQuantity`. Follow the version check, Product lookup,
   item write, capacity decision, cart summary, and mapping. Highlight
   `@Transactional` on the public method called through the managed bean.
   Keep both writes in that boundary. A private helper is not a new proxy entry.
4. Open `CartMapper`. Show that it reads model state and builds DTOs, orders the
   items, and excludes Customer email. It does not query repositories. Mapping
   happens before the service transaction ends, while relationships can be read.
5. In Requests, submit this command once:

```bash
curl -i -X PUT http://127.0.0.1:8080/api/customers/100/cart/items/43 \
  -H 'Content-Type: application/json' \
  -d '{"quantity":2,"expectedVersion":0}'
```

Expect HTTP 200, version 1, total 3, and Product 43 quantity 2 alongside Product 42.
Then read the cart independently:

```bash
curl -i http://127.0.0.1:8080/api/customers/100/cart
```

6. Explain that URI, JSON fields, version progression, and persisted result match
   Chapter 10. Renaming packages alone would not prove this.
7. In App, press Ctrl+C and wait for graceful shutdown. Leave PostgreSQL running.
   Continue directly into Phase 2. Keep the service tests for Phase 4 so the
   request, configuration, and diagnostic evidence stays together.

**Checkpoint:** cart still version 1, total 3. No fixture reset has occurred.

### Phase 2: Configure the same artifact

**Starting state:** Java stopped, cart version 1 and total 3.

1. Open `NorthstarProperties`, then `NorthstarMarketApplication`. Show the record,
   `@ConfigurationProperties`, nested `@Valid`, range 1–20, and
   `@ConfigurationPropertiesScan` registration.
2. In `application.properties`, show this as one physical line:

```properties
northstar.cart.maximum-total-quantity=${NORTHSTAR_CART_MAX_TOTAL_QUANTITY:20}
```

3. Explain that the file supplies 20 and the named environment variable can select
   a stricter policy. The upper bound agrees with the database constraint.
   A profile selects configuration; it does not protect secrets or prove correctness.
   There is no need to create a new profile for this one-value demonstration.
4. In App, start the unchanged JAR with a limit of 3:

```bash
NORTHSTAR_CART_MAX_TOTAL_QUANTITY=3 java -jar target/northstar-market-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1
```

The assignment applies only to this command. Wait for startup. In Requests:

```bash
curl -i -X PUT http://127.0.0.1:8080/api/customers/100/cart/items/44 \
  -H 'Content-Type: application/json' \
  -d '{"quantity":1,"expectedVersion":1}'
```

Expect HTTP 409, code `cart_capacity_exceeded`, with an attempted total of 4
and a maximum of 3. Read the cart again:

```bash
curl -i http://127.0.0.1:8080/api/customers/100/cart
```

Expect version 1, total 3, and no Product 44 item. This proves the selected
configuration affects the use case and the failed update leaves committed state intact.
5. Stop Java with Ctrl+C. In App, deliberately supply an invalid value:

```bash
NORTHSTAR_CART_MAX_TOTAL_QUANTITY=0 java -jar target/northstar-market-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1
```

Wait until the process exits. Expect `APPLICATION FAILED TO START` with a binding
or validation message for `northstar.cart.maximum-total-quantity`. It must not
remain serving traffic. Explain that the failure prevents an invalid deployment.
6. Restart with the default, preserving a local log for Phase 3. In App:

```bash
set -o pipefail
unset NORTHSTAR_CART_MAX_TOTAL_QUANTITY
java -jar target/northstar-market-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 2>&1 | tee /tmp/northstar-ch11-demo.log
```

Wait for startup. `tee` overwrites this dedicated rehearsal log. Do not use a real
customer log or commit this file. The cart remains version 1 and total 3.

### Phase 3: Correlate events and verify the boundary

Keep App running with the default limit of 20. Stay in VS Code.

1. In Requests, submit Product 44 quantity 10 and capture the response headers:

```bash
curl -sS -D /tmp/northstar-ch11-success.headers \
  -X PUT http://127.0.0.1:8080/api/customers/100/cart/items/44 \
  -H 'Content-Type: application/json' \
  -d '{"quantity":10,"expectedVersion":1}'
```

Expect version 2 and total 13. Show the headers, then extract the request ID:

```bash
cat /tmp/northstar-ch11-success.headers
request_id=$(awk 'tolower($1)=="x-request-id:" {gsub("\r","",$2); print $2}' /tmp/northstar-ch11-success.headers)
grep -F "$request_id" /tmp/northstar-ch11-demo.log
```

Expect an INFO event with that ID and `Cart update prepared for commit`,
Product 44, quantity 10, total 13. The event occurs inside the transaction.
It describes progress before commit, so it is not a commit receipt.
2. Attempt a total of 21:

```bash
curl -sS -D /tmp/northstar-ch11-rejected.headers \
  -X PUT http://127.0.0.1:8080/api/customers/100/cart/items/46 \
  -H 'Content-Type: application/json' \
  -d '{"quantity":8,"expectedVersion":2}'
```

Expect HTTP 409 in the captured headers and code `cart_capacity_exceeded`.
The problem body's `requestId` must match `X-Request-ID`. Find the event:

```bash
cat /tmp/northstar-ch11-rejected.headers
request_id=$(awk 'tolower($1)=="x-request-id:" {gsub("\r","",$2); print $2}' /tmp/northstar-ch11-rejected.headers)
grep -F "$request_id" /tmp/northstar-ch11-demo.log
```

Expect a WARN event with attempted total 21 and configured maximum 20.
Point out the selected fields. Do not log the entire request, Customer object,
password, or token. The ID connects events; it does not identify an authenticated user.
3. Read the cart:

```bash
curl -i http://127.0.0.1:8080/api/customers/100/cart
```

Expect version 2 and total 13 with Products 42, 43, 44. Product 46 must be absent.
This independent read supplies state evidence the warning cannot provide.
4. Open `RequestIdFilter`. Show server generation, response header, MDC scope,
   and try-with-resources cleanup. This applies to the synchronous request thread.
   It does not automatically propagate context to asynchronous tasks or other services.
### Phase 4: Run the focused tests and interpret their limits

1. Stop Java in App with Ctrl+C and wait for the Bash prompt. Keep PostgreSQL
   running. App retains the exported database credentials needed by Maven.
2. Run the service, configuration, and filter tests together:

```bash
./mvnw -Dtest=CartServiceTests,NorthstarPropertiesTests,RequestIdFilterTests test
```

Expect **9 tests**, zero failures, errors, or skips, and BUILD SUCCESS:
three service tests, four configuration tests, and two filter tests. Expected
configuration-failure warnings belong to passing rejection tests. These tests
do not reset the database fixture.

3. Open `rejectsAStaleVersionBeforeLookingUpTheProduct` in `CartServiceTests`.
   Show that a stale version stops Product lookup and item writes. The mocked
   collaborators establish decision order, not database rollback.
4. Point to the configuration tests for valid binding, zero, values above 20,
   and missing nested settings. Point to `RequestIdFilterTests` for matching
   header/context, distinct request IDs, and cleanup on success and failure.
5. Run the database rollback test separately, still with Java stopped:

```bash
./mvnw '-Dtest=CartPersistenceIntegrationTests#failureAfterItemFlushRollsBackTheWholeUseCase' test
```

Expect **1 test**, zero failures, errors, or skips, and BUILD SUCCESS. Open the
method and show its transaction and post-failure database checks. This test
proves rollback after a flush against PostgreSQL. It restores the disposable
fixture to **version 0, total 1**. This reset is why it belongs after the live
requests, not between them.

6. Interpret the evidence: mocks check decisions, PostgreSQL checks transactions,
   HTTP checks the external contract, and logs explain a particular request.
   The preparation's full suite had 81 tests; these focused commands ran 9 + 1.
7. **Return to slide 13.** The continuous demonstration is complete.

## Part 3: After the lecture

Java should be stopped. Use the README's stop instructions for the private
PostgreSQL cluster, and unset credentials in every configured terminal.
Keep the recorded lab path if you want to resume. Do not delete earlier work.

To rehearse from the beginning, start your owned cluster, configure App, run
`./mvnw clean verify`, and return to Part 1. The full suite resets the cart.

## Troubleshooting

| Symptom | Check and recovery |
| --- | --- |
| `read: -p: no coprocess` | Enter Bash before the README password commands. |
| PostgreSQL role does not exist | Confirm port 5436 and the new cluster's bootstrap. Do not assume a `postgres` role. |
| Build cannot connect | Check the owned cluster, JDK, JDBC URLs, and hidden password entry in this terminal. |
| Demo returns version conflict | Stop Java, reset only this disposable fixture with the persistence tests, and restart from Phase 1 after confirming total 1/version 0. |
| Limit 3 has no effect | Stop the old Java process. Restart using the inline assignment in Phase 2 and wait for startup. |
| No matching log line | Use the PUT request's ID. GET is not a cart-update event. Ensure Phase 3 runs through `tee`. |
| Invalid-value startup fails for a database reason | Restore database connectivity first, then repeat to observe the intended configuration failure. |
| Tests pass but the demo differs | Confirm this chapter folder and default capacity. A later cumulative project has additional features. |
