# AutoDarts League Manager

Simple Spring Boot backend for managing local darts league tournaments.

## Run It

```bash
gradle bootRun
```

That's it! The app starts on `http://localhost:8080`

## Requirements

- Java 17 or higher
- Gradle 7.0 or higher

## Quick Test

### 1. Create Players

```bash
curl -X POST http://localhost:8080/api/players -H "Content-Type: application/json" -d '{"displayName": "Haris", "autodartsUsername": "haris_darts"}'
curl -X POST http://localhost:8080/api/players -H "Content-Type: application/json" -d '{"displayName": "Amir", "autodartsUsername": "amir_darts"}'
curl -X POST http://localhost:8080/api/players -H "Content-Type: application/json" -d '{"displayName": "Selma", "autodartsUsername": "selma_darts"}'
curl -X POST http://localhost:8080/api/players -H "Content-Type: application/json" -d '{"displayName": "Emir", "autodartsUsername": "emir_darts"}'
```

### 2. Create Tournament

```bash
curl -X POST http://localhost:8080/api/tournaments -H "Content-Type: application/json" -d '{
  "name": "Friday Night League",
  "gameMode": "501",
  "legsPerMatch": 3,
  "roundsPerPlayer": 2,
  "playerIds": [1, 2, 3, 4]
}'
```

### 3. Get Fixtures

```bash
curl http://localhost:8080/api/tournaments/1/fixtures
```

### 4. Submit Result

```bash
curl -X POST http://localhost:8080/api/fixtures/1/result -H "Content-Type: application/json" -d '{
  "homeLegsWon": 2,
  "awayLegsWon": 1
}'
```

### 5. View Standings

```bash
curl http://localhost:8080/api/tournaments/1/standings
```

## H2 Console

Access the database at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:file:./data/autodarts-league`
- Username: `sa`
- Password: (leave blank)

## Tournament Configuration

- `roundsPerPlayer = 1`: Single round-robin (6 matches for 4 players)
- `roundsPerPlayer = 2`: Double round-robin (12 matches for 4 players)
- `roundsPerPlayer = 3`: Triple round-robin (18 matches for 4 players)

## What's Next

- Build Angular frontend
- Build browser extension to capture AutoDarts results
https://api.autodarts.io/gs/v0/lobbies/019c7ace-b9ae-792a-bcda-b6a24af8e562/invitations/141d569b-7513-41d1-ac18-02913190aeb3