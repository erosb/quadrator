# Quadrator

## Code coverage

JaCoCo is configured in the Maven build to collect coverage while the tests run
and generate an HTML report during the `verify` phase:

```shell
mvn verify
```

The report is written to
`target/site/jacoco/index.html`. Open that file in a browser to view line,
branch, and instruction coverage metrics.
