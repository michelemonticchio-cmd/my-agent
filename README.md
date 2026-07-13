# My Agent

Agente AI con backend Spring Boot: analizza repository GitHub tramite
LLM con function calling.

## Stack
- Java 25, Spring Boot, Spring Data JPA
- H2 (dev) → PostgreSQL (previsto)
- API LLM Anthropic

## Funzionalità
- [x] Endpoint `POST /api/chat` — invia un messaggio a Claude e restituisce la risposta
- [ ] Memoria conversazionale (cronologia su DB)
- [ ] Function calling / tool
- [ ] Autenticazione JWT
- [ ] Frontend chat

## Configurazione
Richiede la variabile d'ambiente `ANTHROPIC_API_KEY`.
