# My Agent

Conversational AI agronomy assistant for Salento (Puglia, Italy), with a Spring
Boot backend built from scratch — not a wrapper around a managed AI platform.
The agent talks to the Anthropic API directly, keeps its own conversation
memory, and extends its knowledge through custom tools (function calling).

## Domain

The assistant helps with the crops most common in Salento: olive, grapevine,
citrus, fig and almond. It combines three kinds of information:
- **Live weather data** (Open-Meteo), to reason about upcoming conditions
- **The current date**, to reason about the crop's seasonal growth phase
  (flowering, harvest, planting/grafting window, etc.)
- **A curated local knowledge base** of diseases and pests (e.g. Xylella
  fastidiosa, olive fruit fly), including known treatment care-adjustments
  and timing caveats, to ground its answers in verified information instead
  of relying only on the model's own training data

All three tools are exposed to the same agent loop, and a system prompt
instructs the model to combine them on its own — e.g. diagnosing a disease,
then factoring in the season and the forecast before suggesting (or
adapting) a treatment — without any hardcoded orchestration logic in Java.

Symptoms can be described in words or shown in one or more photos: a dedicated
endpoint accepts multiple image uploads, and Claude's own vision recognizes
visible symptoms directly, feeding them into the same diagnosis tool used for
text.

Users can track individual plants over time: each plant gets its own
dedicated conversation (with an optional plantation/grove label), so its full
history — symptoms, diagnoses, treatments, outcomes — naturally informs later
advice, without any separate "memory" data model beyond the conversation
itself.

## Architecture highlights
- **Agent loop with tool calling**: `ClaudeService` drives a loop that keeps
  calling the Anthropic API and executing tools until the model returns a
  final text answer (bounded by a max-iteration safety limit); each turn can
  contain multiple `tool_use` blocks, executed in order
- **System-prompt-driven orchestration**: a single system prompt establishes
  the agent's persona and nudges it to combine tools (weather, season,
  diagnosis) when it genuinely improves the advice — no hardcoded
  if-this-then-that logic decides when tools get combined
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
- **JWT authentication**: a stateless `JwtAuthFilter` validates a bearer token
  on every request; each conversation is tied to its owning user, and access
  to another user's conversation is rejected
- **Per-plant conversations as memory**: an unused `Conversation.title` field
  became the basis for per-plant "case files" — no new data model needed,
  since full message history was already persisted
- **Rate limiting + paid unlock**: a fixed 2-hour window caps free requests
  per user; a real Stripe Checkout session (test mode) unlocks unlimited
  requests for the rest of the current window, confirmed via a
  signature-verified webhook rather than trusting the client redirect

## Tools available
| Tool | Purpose | Data source |
|---|---|---|
| `che_ore_sono` | Current date/time | System clock |
| `get_weather_forecast` | Multi-day weather forecast | Open-Meteo API |
| `diagnose_plant_disease` | Disease/pest lookup by symptoms, with treatment care-adjustments and timing caveats | Curated local JSON knowledge base |

## Stack
- Java 25, Spring Boot, Spring Data JPA, Spring Security
- H2 (dev) → PostgreSQL (planned)
- Anthropic LLM API
- JJWT for token issuance/validation
- Stripe (test mode) for payments
- Static HTML/CSS/JS chat frontend (no build step)

## Features
- [x] `POST /api/chat` endpoint — sends a message to Claude and returns the reply
- [x] `POST /api/chat/image` endpoint — multipart image upload with an optional
      text caption, for visual symptom diagnosis
- [x] Conversation memory (history persisted to DB)
- [x] Function calling / tools (agent loop with tool_use)
- [x] Multi-turn parallel tool calls (multiple `tool_use` blocks per turn)
- [x] Multimodal input (multiple plant photos per message)
- [x] JWT authentication (register/login, per-user conversation ownership)
- [x] Chat frontend (static HTML/CSS/JS, login screen included)
- [x] Per-plant conversation sidebar (named "case files", optional plantation grouping)
- [x] Rate limiting (free requests per 2-hour window) with Stripe-based unlock
- [x] Account panel (username, registration date, live quota status)
- [ ] Docker / streaming responses

## Configuration
Requires the `ANTHROPIC_API_KEY`, `JWT_SECRET`, `STRIPE_SECRET_KEY` and
`STRIPE_WEBHOOK_SECRET` environment variables.
