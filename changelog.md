# Changelog

## [v0.0.2] - 2026-04-18

### Added
- Full UI for test creation without JSON input, using RadioButton/CheckBox for answers
- Support for multiple correct answers per question via `multiple` field
- Practice assignment creation with unit tests
- Isolated Docker container execution for student code submissions
- Unit tests for `SessionManager` and `ApiException`

### Fixed
- GitHub OAuth flow now returns to login screen on failed 2FA or unexpected dashboard redirect
- Maximized window state is preserved during scene navigation
- Dialog windows are now centered on screen
- Fixed answer format on test submission (index-based instead of text-based)
- Fixed `submitTest` backend bug: `correctAnswers.get(String.valueOf(i))`

### Refactored
- Removed duplicate controllers from `pages` package, keeping only `dialogs`
- Removed `CodeMirrorEditor` WebView ACE editor, replaced with `TextArea`
- `ApiException` now automatically parses JSON error messages
- `ApiClient` now explicitly sets UTF-8 encoding for all requests and responses
- `CreateAssignmentController` synchronized with FXML field IDs

### Build
- Removed unused dependencies: `jackson-databind`, `controlsfx`, `bootstrapfx-core`
- Removed unused `requires` from `module-info.java`

---

## [v1.0-SNAPSHOT] - initial release
- release