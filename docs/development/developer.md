Development Workflows
=====================

Information about LibreClinica development and contribution workflows.
The main-only project procedure below takes precedence over historical upstream conventions.

# Version Control

Git version control system is used for tracking changes in LibreClinica
project repositories. Those are hosted publicly via [ReliaTec GmbH
GitHub](https://github.com/reliatec-gmbh/) organisation.

# Repository

[LibreClinica](https://github.com/reliatec-gmbh/LibreClinica) (origin)
repository was created as a fork from
[OpenClinica](https://github.com/OpenClinica/OpenClinica) (upstream)
public repository. Certain naming conventions are defined in order to
prevent naming collisions (for versions, branches and tags).

# Issue Management

GitHub integrated issues management should be used to creates tickets
before any code contributions. Tickets assigned to milestones that
reflect the LibreClinica software versioning scheme. For each version an
appropriate Kanban board is defined to track the progress of development
activities.

# Main-only project workflow

The owner directive dated 2026-09-20 supersedes the upstream branching model
for this project. Work only on `main` in the primary checkout under C:\Projects.
This includes new features, fixes, tests, reviews and release preparation.
Do not create or switch to another branch, create a worktree, or use an old
agent worktree. The shared guards must remain enabled.

Before editing, read the shared and repository AGENTS.md files, inspect the
working tree and trace the existing implementation and callers. Preserve
unrelated changes and coordinate concurrent authors.

```shell
git rev-parse --show-toplevel
git branch --show-current
git status --short
git fetch origin
```

When the working tree is clean, synchronize with `git pull --ff-only origin main`.
Review and test the completed diff, commit it to main, and push main to the
configured origin using the authenticated transport in AGENTS.md. Verify the
remote commit after pushing. If histories diverge, reconcile them on main
while preserving unique changes.

Record reviewer findings, the checks actually run and any release approval
against the final commit. Release tags identify approved versions without
introducing another working branch.

# Tags

Tags should be created for released LibreClinica versions.

# Release Versioning

There is a need to start with fresh versioning scheme for LibreClinica
(independently from upstream) to allow independent release cycle.

-   MAJOR.MINOR.PATCH

Database scheme versioning is currently unchanged. Up until now there
have not been changes in DB scheme introduced in LibreClinica since the
time of fork.

# Contributions

Local contributions are reviewed, committed and pushed on main in this primary
checkout. Any separately authorized upstream contribution can be prepared as a
patch from the reviewed commit. If the contribution is targeting a
registered bug ticket, this bug need to be described (ideally using
defined bug report template) in a reproducible manner and reproduced by
somebody else from the team of contributors. For new features the
appropriate test specification need to be submitted fitting the numeric
scheme used for test documentation (this should be ideally clarified
with the main contributor team before).
