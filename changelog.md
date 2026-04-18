# Changelog

## [v0.0.2] - 2026-04-18

### Added
- Full UI for test creation without JSON input, using RadioButton/CheckBox for answers ([2a0dc86](https://github.com/fxhxyz4/AssessX/commit/2a0dc86))
- Support for multiple correct answers per question via `multiple` field ([2a0dc86](https://github.com/fxhxyz4/AssessX/commit/2a0dc86))
- Practice assignment creation with unit tests ([2a0dc86](https://github.com/fxhxyz4/AssessX/commit/2a0dc86))
- Isolated Docker container execution for student code submissions ([2a0dc86](https://github.com/fxhxyz4/AssessX/commit/2a0dc86))
- Unit tests for `SessionManager` and `ApiException` ([7306051](https://github.com/fxhxyz4/AssessX/commit/7306051))

### Fixed
- GitHub OAuth flow now returns to login screen on failed 2FA or unexpected dashboard redirect ([62eb21e](https://github.com/fxhxyz4/AssessX/commit/62eb21e))
- Maximized window state is preserved during scene navigation ([62eb21e](https://github.com/fxhxyz4/AssessX/commit/62eb21e))
- Dialog windows are now centered on screen ([2a0dc86](https://github.com/fxhxyz4/AssessX/commit/2a0dc86))
- Fixed answer format on test submission ([2a0dc86](https://github.com/fxhxyz4/AssessX/commit/2a0dc86))
- Fixed `submitTest` backend bug: `correctAnswers.get(String.valueOf(i))` ([2a0dc86](https://github.com/fxhxyz4/AssessX/commit/2a0dc86))
- Removed ui package from module-info ([fcadcc6](https://github.com/fxhxyz4/AssessX/commit/fcadcc6))

### Refactored
- Removed duplicate controllers from `pages` package ([f626f7e](https://github.com/fxhxyz4/AssessX/commit/f626f7e))
- Removed `CodeMirrorEditor` WebView ACE editor ([f626f7e](https://github.com/fxhxyz4/AssessX/commit/f626f7e))
- `ApiException` now automatically parses JSON error messages ([1a8ae9a](https://github.com/fxhxyz4/AssessX/commit/1a8ae9a))
- `ApiClient` now explicitly sets UTF-8 encoding ([1a8ae9a](https://github.com/fxhxyz4/AssessX/commit/1a8ae9a))
- `CreateAssignmentController` synchronized with FXML field IDs ([2a0dc86](https://github.com/fxhxyz4/AssessX/commit/2a0dc86))

### Build
- Removed unused dependencies: `jackson-databind`, `controlsfx`, `bootstrapfx-core` ([4db8685](https://github.com/fxhxyz4/AssessX/commit/4db8685))
- Removed unused `requires` from `module-info.java` ([fcadcc6](https://github.com/fxhxyz4/AssessX/commit/fcadcc6))

---

## [v1.0-SNAPSHOT] - initial release
- release
