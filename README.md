# Chapter 11: Designing Maintainable Server Applications

Open this folder in VS Code, at the level containing `pom.xml`. This is the
complete Chapter 11 application, not the later cumulative textbook application.
It uses Java 25, the Maven Wrapper, and native PostgreSQL 18. Docker is not needed.
Install the VS Code Extension Pack for Java and Spring Boot Extension Pack.

The source is milestone `3404b15` with reviewed boundary corrections from
Chapters 6 through 10 and Chapter 11 corrections. Customer 100 owns Cart 200.
There is no authentication or authorization yet. Use fictional data on localhost.
Do not use this teaching service for real customers.

## Prepare a new database before running Java

Use a separate disposable cluster on port **5436**. Preserve earlier chapter
databases. The fixed seeds in V3/V4 are not a general upgrade path for retained
student data. Never rewrite an applied migration or use Flyway repair to hide
a mismatch. The project needs migrations 1, 2, 2.1, 3, 4, and 4.1, in that order.
V4.1 removes inherited Customer write grants; runtime still reads Customer.

The following instructions target macOS/Linux. Windows users need an instructor-
provisioned PostgreSQL 18 instance or a suitable Linux environment. The Windows
route has not been rehearsed. `mvnw.cmd` is included but the shell commands below
are Bash. On macOS, enter `bash` first, even if your normal shell is Zsh.

Run commands separately and stop on an error. First check the tools:

```bash
bash
java -version
./mvnw --version
psql --version
initdb --version
pg_ctl --version
```

Java and Maven must report JDK 25. All PostgreSQL tools must report version 18.
For a Homebrew installation, add the versioned tools to PATH if needed:

```bash
export PATH="$(brew --prefix postgresql@18)/bin:$PATH"
```

Initialize a new private lab, without changing a background service:

```bash
export CH11_LAB_DIR=$(mktemp -d /tmp/northstar-ch11.XXXXXX)
printf 'Keep this lab path for restart: %s\n' "$CH11_LAB_DIR"
initdb -D "$CH11_LAB_DIR/data" -U ch11_admin --auth-local=trust --auth-host=scram-sha-256 --encoding=UTF8
pg_ctl -D "$CH11_LAB_DIR/data" -l "$CH11_LAB_DIR/postgres.log" -o "-p 5436 -h 127.0.0.1 -k $CH11_LAB_DIR" -w start
```

The private socket trusts the local lab owner. TCP requires passwords. This is
a disposable teaching setup, not a deployment configuration. Do not use sudo.
If port 5436 is occupied, stop and choose an unused port consistently. Do not
stop an unidentified service. Temporary storage may be removed by the OS.

From the project root, bootstrap only the new server:

```bash
psql -X -v ON_ERROR_STOP=1 -h "$CH11_LAB_DIR" -p 5436 -U ch11_admin -d postgres -f database/ch07/admin-bootstrap.sql
psql -X -h "$CH11_LAB_DIR" -p 5436 -U ch11_admin -d northstar_market
```

At the psql prompt, enter each command separately and wait for the next prompt:

```text
\password northstar_migrator
```

```text
\password northstar_app
```

```text
\q
```

Choose different disposable passwords and keep them off camera. Do not run
the SQL migrations manually. Flyway runs them during the build.

Verify TCP authentication with a wrong password, then repeat with the correct
runtime password. The first attempt must fail; the second must return
`northstar_app` and `northstar_market`:

```bash
psql -X -W -h 127.0.0.1 -p 5436 -U northstar_app -d northstar_market -c 'SELECT current_user, current_database();'
```

## Configure each Java or Maven terminal

In Bash, enter the password prompts one at a time:

```bash
export NORTHSTAR_DB_URL=jdbc:postgresql://127.0.0.1:5436/northstar_market
export NORTHSTAR_MIGRATION_DB_URL="$NORTHSTAR_DB_URL"
read -r -s -p 'Runtime password: ' NORTHSTAR_DB_PASSWORD
printf '\n'
read -r -s -p 'Migration password: ' NORTHSTAR_MIGRATION_DB_PASSWORD
printf '\n'
export NORTHSTAR_DB_PASSWORD NORTHSTAR_MIGRATION_DB_PASSWORD
```

New VS Code terminals do not inherit another terminal's exports. Repeat this
block wherever Java or Maven will run. Never print the environment or save
passwords in a file that ships with the project. Set `JAVA_HOME` to your JDK 25
installation if Maven selects a different JDK.

```bash
./mvnw clean verify
java -jar target/northstar-market-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1
```

Expect 81 tests with zero failures, errors, or skips. The tests reset the seeded
cart to version 0 and total 1. They are intended for
this disposable fixture only. Never run them against retained work. A normal
application restart does not reset data. First builds need Maven download access.
Inspect the final test summary; expected constraint-error logs are not failed tests.

## Lecture demonstration

Use `LECTURE-DEMO.md` for complete preparation and the single continuous
walkthrough on slide 12. Return to slide 13 after all four phases finish.

1. Trace the controller, service, mapper, and repositories, then verify the cart update.
2. Restart the same JAR with a stricter capacity and show invalid settings prevent startup.
3. Connect response request IDs to safe log events and check committed state after rejection.
4. Stop Java, run nine service/configuration/filter tests, then the one PostgreSQL rollback test.

Keep the fixture intact across the live requests. Only the final persistence
test resets the seed. Slides 7 and 9 explain worked examples before the demo.

Cart source is under `src/main/java/com/example/northstarmarket/cart/`, grouped
by responsibility. Focused tests live in `cart/service/CartServiceTests.java`,
configuration tests in `config/NorthstarPropertiesTests.java`, and correlation
tests in `web/RequestIdFilterTests.java` under `src/test/java/com/example/northstarmarket/`.

The optional `bruno-cart` collection preserves Chapter 10's seven-request workflow.
Use it only with a fresh fixture and the default limit of 20. The lecture uses
curl and its own sequence, documented in `LECTURE-DEMO.md`.
`contract/northstar-product-api.yaml` covers the carried-forward Product API.
It does not claim to describe the cart.

The application generates its own request ID. A “prepared for commit” log records
progress inside the service; it does not prove transaction commit. Verify a
successful response and read the state again. The request ID is local correlation,
not authentication or a distributed trace.

## Stop and resume without deleting work

Stop Java with Ctrl+C. In each terminal that holds passwords:

```bash
unset NORTHSTAR_DB_PASSWORD NORTHSTAR_MIGRATION_DB_PASSWORD NORTHSTAR_SHOW_SQL NORTHSTAR_CART_MAX_TOTAL_QUANTITY
```

In the original terminal, stop only the server you created:

```bash
pg_ctl -D "$CH11_LAB_DIR/data" -m fast -w stop
```

To resume, set `CH11_LAB_DIR` to the exact path you recorded, confirm it contains
your lab's `data/PG_VERSION` with value 18, and run the same `pg_ctl ... start`
command. Do not rerun initdb or bootstrap. Set application credentials again.
For a repeatable new rehearsal, create another disposable cluster after stopping
your previous one, or run the seed-restoring tests only on your owned lab.
Keep database directories, logs, `target/`, passwords, and editor state out of Git.
