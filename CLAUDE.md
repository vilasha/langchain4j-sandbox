# Claude Code Configuration

## Model Preference
Always use `claude-sonnet-4-6` unless a task explicitly requires Opus-level reasoning.

---

## Developer Profile

**Maria** — Senior Backend Engineer, 16 years of experience, AZ-204 certified.  
Strong Java-first orientation with active focus on enterprise AI engineering (LangChain4j, RAG pipelines, LLM integration).  
Former math competition winner — values precision, correctness, and clean reasoning.

---

## Java Coding Conventions (Non-negotiable)

- **No Hibernate / no JPA** — use `JdbcClient` or plain JDBC templates for all database interaction
- **Migrations via Flyway only**
- **POJOs as records** with Lombok `@Builder`
- **Explicit types everywhere** — never use `var`
- **No magic / implicit frameworks** — prefer explicit wiring over convention-over-configuration
- Follow standard Java naming conventions strictly

Example of preferred style:
```java
@Builder
public record PolicyDocument(
    UUID id,
    String filename,
    String content,
    LocalDateTime uploadedAt
) {}
```
---

## Commit Style

Use **Conventional Commits** format:
- `feat:` new features
- `fix:` bug fixes
- `refactor:` code restructuring without behavior change
- `chore:` build/tooling changes
- `docs:` documentation only

Always review `git diff` before writing the commit message — derive the message from actual changes, not assumptions.

---

## General Preferences

- **Be direct and precise** — no filler, no excessive explanation of obvious things
- **Show diffs before applying** — always confirm before modifying files
- **Prefer atomic commits** — one logical change per commit
- **When suggesting improvements**, number them so individual ones can be selected
- **When in doubt about intent**, ask one focused clarifying question rather than making assumptions