# Spec Delta

## Purpose

Defines the local development environment and the continuous integration gate: which services a developer starts, what configuration those services need, and what the pipeline must prove before a change is accepted.

## ADDED Requirements

### Requirement: One-command local stack

The project SHALL provide a Docker Compose file that starts the frontend, the backend, PostgreSQL, Redis and a mail catcher together, so that a developer needs a single command to run the whole system.

#### Scenario: The stack starts from a clean machine

- **WHEN** a developer runs the documented compose command on a machine with Docker and Docker Compose available
- **THEN** the frontend, backend, PostgreSQL, Redis and the mail catcher all start and become reachable

#### Scenario: The schema is applied automatically

- **WHEN** the stack is started against an empty database volume
- **THEN** the database schema the application expects exists once the backend has started
- **AND** the schema was applied by the backend's own migrations rather than by manual steps

#### Scenario: Configuration is supplied without editing tracked files

- **WHEN** a developer starts the stack for the first time
- **THEN** the required settings are supplied from a local untracked configuration file derived from the provided example

### Requirement: Documented execution guide

The project SHALL ship a README that states the prerequisites, the command that starts the stack, and the URLs a developer visits to use the application and to read captured email.

#### Scenario: A new developer starts the project

- **WHEN** a developer follows the README from a clean checkout
- **THEN** the README names the required tool versions, the single command that starts everything, and the URL of the running application

#### Scenario: The README shows the workspace layout

- **WHEN** a reader looks for the shape of the repository
- **THEN** the README contains a directory tree showing where the frontend, backend, container and CI files live

#### Scenario: The README explains how to read captured email

- **WHEN** a developer needs to see a message the application sent
- **THEN** the README names the URL of the mail catcher's web interface

### Requirement: Continuous integration gate

The project SHALL run a CI pipeline on every push and pull request that lints, tests and builds the container images for both the backend and the frontend.

#### Scenario: A change is pushed

- **WHEN** a commit is pushed to a branch with an open pull request
- **THEN** the pipeline runs lint, test and image-build steps for the backend and the frontend

#### Scenario: A test fails

- **WHEN** any test in either project fails
- **THEN** the pipeline reports failure

#### Scenario: A lint error is introduced

- **WHEN** a change introduces a lint error in the frontend
- **THEN** the pipeline reports failure

### Requirement: Coverage baseline is enforced

The CI pipeline SHALL fail when line coverage in either the backend or the frontend falls below 80 percent.

#### Scenario: Coverage falls below the baseline

- **WHEN** the backend or the frontend reports line coverage below 80 percent
- **THEN** the pipeline step fails

#### Scenario: Coverage meets the baseline

- **WHEN** both projects report line coverage of at least 80 percent
- **THEN** the coverage step passes

### Requirement: Coverage excludes non-behavioral code

The coverage measurement for both projects SHALL exclude configuration classes, generated code, type declarations and application entry points, so that the reported percentage reflects behavior rather than boilerplate.

#### Scenario: A configuration-only class does not lower the percentage

- **WHEN** a class contains only configuration and no executable behavior
- **THEN** it is excluded from the coverage measurement
