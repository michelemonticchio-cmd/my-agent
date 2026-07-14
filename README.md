# My Agent

Conversational AI agent with a Spring Boot backend, built from scratch to
demonstrate full-stack Java + AI integration skills — not just a chat wrapper.

## Stack
- Java 25, Spring Boot, Spring Data JPA
- H2 (dev) → PostgreSQL (planned)
- Anthropic LLM API

## Features
- [x] `POST /api/chat` endpoint — sends a message to Claude and returns the reply
- [x] Conversation memory (history persisted to DB)
- [x] Function calling / tools (agent loop with tool_use)
- [ ] JWT authentication
- [ ] Chat frontend

## Configuration
Requires the `ANTHROPIC_API_KEY` environment variable.
