# TODO — Pending Requirements & Tasks

This file contains pending requirements, bugs, and technical tasks that need to be addressed.

## General Guidelines

- Implement in the order you like, unless a priority like `[HIGH]` is specified.
- Use `[ ]` for pending, `[/]` for in-progress, `[-]` for blocked, and `[x]` for completed.
- Ask questions if something is unclear before starting.

## Definition of Done (DoD)

For every completed item, ensure:

- [ ] Code compiles without errors and remains clean/DRY.
- [ ] UI and UX remain consistent with the rest of the app.
- [ ] Tests are updated/added, run locally, and pass.
- [ ] `architecture.md` (especially Chapter 6) is updated if necessary.
- [ ] `user_manual.md` is updated for any user-facing changes.

## Bugs

<!-- Try to include: Expected vs Actual behavior, Steps to reproduce -->

### Radio module

- [ ]

### Music module

### Podcast module

- [ ]
- [ ]

### Common module

- [ ]

## New Features

<!-- Try to include: Acceptance Criteria -->

### Radio module

- [ ]

### Music module

- [ ]

### Podcast module

- [x] Remove the 'now playing' screen. The mini player at the bottom should link to the detail screen when clicked.
- [x] When marked played in the detail screen, the episode must be removed, and playing stopped, and the mini player should be cleared. We go back to the proper Recent episode list screen

### Common module

- []

## Technical Debt & Chores

<!-- For refactoring, dependency updates, or internal improvements -->

### Radio module

- [ ]

### Music module

- [ ]

### Podcast module

- [ ]

### Common module

- [ ]
- [ ] Implement Import/Export for Podcast subscriptions (OPML format)
- [ ] Implement Import/Export for Radio station lists (JSON/Text)
- [ ] When using import/export, podcast and radio should be both handled at the same time (not 2 different buttons to do the same thing for each one)
