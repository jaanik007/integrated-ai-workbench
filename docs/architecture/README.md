# Base Plugin architecture

Status: PARTIAL

The project uses one Gradle module with source roots separated by responsibility:

- `base/frontend`: IntelliJ Tool Window and presentation code.
- `base/shared`: immutable contracts shared across UI and backend boundaries.
- `base/backend`: model registry, runtime extension points, grounding, prompting, security and evaluation boundaries.

The Base Plugin has no bundled inference engine, model binary or cloud provider. `LocalInferenceAdapter` is the future
integration point for a user-selected local runtime. The current Tool Window only reports the bootstrap state.
