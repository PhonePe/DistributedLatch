# Contributing to Distributed Latch

Thank you for considering contributing to Distributed Latch! We welcome contributions from the community.

## How to Contribute

### Reporting Bugs

- Check the [issue tracker](https://github.com/PhonePe/distributed-latch/issues) to see if the bug has already been reported.
- If not, open a new issue with a clear title, description, steps to reproduce, and expected vs actual behavior.

### Suggesting Features

- Open an issue describing the feature, its use case, and why it would benefit the project.

### Submitting Changes

1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/<your-username>/distributed-latch.git
   cd distributed-latch
   ```
3. **Create a branch** for your change:
   ```bash
   git checkout -b feature/my-change
   ```
4. **Make your changes** and add tests for new functionality.
5. **Run the tests** to make sure everything passes:
   ```bash
   mvn clean verify
   ```
6. **Commit** your changes with a clear message:
   ```bash
   git commit -m "Add feature: brief description"
   ```
7. **Push** to your fork and open a **Pull Request** against the `main` branch.

### Code Style

- Follow the existing code style in the project.
- Use the `.editorconfig` settings provided in the repository.
- Ensure Lombok annotations are used consistently.
- All new Java files must include the Apache 2.0 license header.

### Code Coverage

- All new code should have accompanying unit tests.
- Aim for high code coverage — the project uses JaCoCo for coverage reporting.
- Run coverage locally:
  ```bash
  mvn clean verify
  ```
  Coverage reports are generated at `target/site/jacoco/index.html`.

### Pull Request Guidelines

- Keep PRs focused and small — one feature or fix per PR.
- Include a clear description of what the change does and why.
- Ensure all existing tests pass.
- Add tests for any new functionality.
- Update documentation (README, docs/) if the change affects user-facing behavior.

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).

