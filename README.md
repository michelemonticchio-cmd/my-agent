# My Agent

Conversational AI agronomy assistant for Salento (Puglia, Italy), with a Spring
Boot backend built from scratch — not a wrapper around a managed AI platform.
The agent talks to the Anthropic API directly, keeps its own conversation
memory, and extends its knowledge through custom tools (function calling).

## Domain

The assistant helps with the crops most common in Salento: olive, grapevine,
citrus, fig and almond. It combines two kinds of information:
- **Live weather data** (Open-Meteo), to reason about upcoming conditions
- **A curated local knowledge base** of diseases and pests (e.g. Xylella
  fastidiosa, olive fruit fly), to ground its answers in verified information
  instead of relying only on the model's own training data

Because both tools are exposed to the same agent loop, the model can combine
them on its own — e.g. diagnosing a disease and factoring in the forecast
before suggesting a treatment — without any hardcoded orchestration logic.

## Architecture highlights
- **Agent loop with tool calling**: `ClaudeService` drives a loop that keeps
  calling the Anthropic API and executing tools until the model returns a
  final text answer (bounded by a max-iteration safety limit)
- **Tool auto-discovery**: any `@Component implements Tool` is picked up
  automatically by `ToolRegistry` via Spring's constructor injection of
  `List<Tool>` — adding a tool never requires touching the registry or the
  service
- **Custom exception boundary**: technical failures (HTTP errors, empty
  responses) are translated into a single `LlmException` contract, so the
  rest of the app depends on one stable failure mode instead of client
  implementation details
- **Conversation memory**: the Anthropic API is stateless, so `ClaudeService`
  reloads the full message history from the database on every call

## Tools available
| Tool | Purpose | Data source |
|---|---|---|
| `che_ore_sono` | Current date/time | System clock |
| `get_weather_forecast` | Multi-day weather forecast | Open-Meteo API |
| `diagnose_plant_disease` | Disease/pest lookup by symptoms | Curated local JSON knowledge base |

## Stack
- Java 25, Spring Boot, Spring Data JPA
- H2 (dev) → PostgreSQL (planned)
- Anthropic LLM API

## Features
- [x] `POST /api/chat` endpoint — sends a message to Claude and returns the reply
- [x] Conversation memory (history persisted to DB)
- [x] Function calling / tools (agent loop with tool_use)
- [ ] Multi-turn parallel tool calls (currently one tool per iteration)
- [ ] Multimodal input (plant photos)
- [ ] JWT authentication
- [ ] Chat frontend
- [ ] Docker / streaming responses

## Configuration
Requires the `ANTHROPIC_API_KEY` environment variable.
