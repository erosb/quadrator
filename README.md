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

## Documentation

The MkDocs environment is created in the repository-local `.venv`; no Python
packages need to be installed globally. With Python 3 available, run:

```shell
python3 scripts/mkdocs.py serve
```

To build the site into `site/`, run:

```shell
python3 scripts/mkdocs.py build
```
