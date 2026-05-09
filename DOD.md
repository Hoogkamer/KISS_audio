# Definition of Done (DoD)

To maintain a stable and high-quality codebase, every task must satisfy the following criteria before being considered "Done":

## 1. Code Quality & Correctness
- [ ] **No Compilation Errors:** Ensure the code builds successfully. Check for missing or extra braces, typos, and unresolved references.
- [ ] **State Management:** Verify that MVI state updates are handled correctly and UI reflects the state changes immediately.
- [ ] **Cleanup:** Ensure that stopping playback also clears relevant UI states (mini-player, metadata) where appropriate.

## 2. Documentation & Release
- [ ] **Architecture (Section 6):** Update `architecture.md` if state management or navigation logic has changed.
- [ ] **User Manual:** Update `user_manual.md` if new features are added or existing workflows change.
- [ ] **GitHub Release:** Create a new Tag (e.g., `v1.0.1`) and push to GitHub. Verify the automated build creates a new Release with the APK attached.
- [ ] **Todo:** Mark completed tasks in `todo.md`.

## 3. UI/UX Consistency
- [ ] **Navigation:** Back button behavior must be consistent and context-aware.
- [ ] **Mini-Player:** Visibility and navigation from the mini-player must align with the current module.

## 4. Maintenance Rituals
- [ ] **DRY Principles:** Avoid code duplication across modules.
- [ ] **Resources:** Use standard Material 3 components and shared icons.
