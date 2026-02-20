package com.autodarts.league.controller

import com.autodarts.league.domain.Player
import com.autodarts.league.dto.*
import com.autodarts.league.repository.PlayerRepository
import com.autodarts.league.service.TournamentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/players")
class PlayerController(
    private val playerRepository: PlayerRepository
) {

    @GetMapping
    fun getAllPlayers(): List<PlayerResponse> =
        playerRepository.findAll().map { PlayerResponse.from(it) }

    @PostMapping
    fun createPlayer(@RequestBody request: CreatePlayerRequest): ResponseEntity<PlayerResponse> {
        // Return existing player if username already registered,
        // updating their autodartsUserId if we now have it and they don't.
        val existing = playerRepository.findByAutodartsUsername(request.autodartsUsername)
        if (existing != null) {
            if (existing.autodartsUserId == null && request.autodartsUserId != null) {
                val updated = playerRepository.save(existing.copy(autodartsUserId = request.autodartsUserId))
                return ResponseEntity.ok(PlayerResponse.from(updated))
            }
            return ResponseEntity.ok(PlayerResponse.from(existing))
        }

        val player = playerRepository.save(
            Player(
                displayName = request.displayName.ifBlank { request.autodartsUsername },
                autodartsUsername = request.autodartsUsername,
                autodartsUserId = request.autodartsUserId
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(PlayerResponse.from(player))
    }

    @GetMapping("/{id}")
    fun getPlayer(@PathVariable id: Long): ResponseEntity<PlayerResponse> {
        val player = playerRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Player not found") }
        return ResponseEntity.ok(PlayerResponse.from(player))
    }

    @DeleteMapping("/{id}")
    fun deletePlayer(@PathVariable id: Long): ResponseEntity<Void> {
        playerRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}

@RestController
@RequestMapping("/api/tournaments")
class TournamentController(
    private val tournamentService: TournamentService
) {

    @GetMapping
    fun getAllTournaments(): List<TournamentResponse> =
        tournamentService.getAllTournaments().map { TournamentResponse.from(it) }

    @PostMapping
    fun createTournament(@RequestBody request: CreateTournamentRequest): ResponseEntity<TournamentResponse> {
        val tournament = tournamentService.createTournament(
            name = request.name,
            gameMode = request.gameMode,
            legsPerMatch = request.legsPerMatch,
            roundsPerPlayer = request.roundsPerPlayer,
            playerIds = request.playerIds
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(TournamentResponse.from(tournament))
    }

    @GetMapping("/{id}")
    fun getTournament(@PathVariable id: Long): ResponseEntity<TournamentResponse> =
        ResponseEntity.ok(TournamentResponse.from(tournamentService.getTournament(id)))

    @DeleteMapping("/{id}")
    fun deleteTournament(@PathVariable id: Long): ResponseEntity<Void> {
        tournamentService.deleteTournament(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/start")
    fun startTournament(@PathVariable id: Long): ResponseEntity<TournamentResponse> =
        ResponseEntity.ok(TournamentResponse.from(tournamentService.startTournament(id)))

    @GetMapping("/{id}/fixtures")
    fun getFixtures(@PathVariable id: Long): ResponseEntity<List<FixtureResponse>> =
        ResponseEntity.ok(tournamentService.getFixtures(id).map { FixtureResponse.from(it) })

    @GetMapping("/{id}/fixtures/round/{roundNumber}")
    fun getFixturesByRound(
        @PathVariable id: Long,
        @PathVariable roundNumber: Int
    ): ResponseEntity<List<FixtureResponse>> =
        ResponseEntity.ok(tournamentService.getFixturesByRound(id, roundNumber).map { FixtureResponse.from(it) })

    @GetMapping("/{id}/standings")
    fun getStandings(@PathVariable id: Long): ResponseEntity<List<StandingsResponse>> =
        ResponseEntity.ok(tournamentService.getStandings(id).map { StandingsResponse.from(it) })

    @GetMapping("/{id}/next-fixture")
    fun getNextPendingFixture(@PathVariable id: Long): ResponseEntity<FixtureResponse?> {
        val fixture = tournamentService.getNextPendingFixture(id)
        return if (fixture != null) ResponseEntity.ok(FixtureResponse.from(fixture))
        else ResponseEntity.noContent().build()
    }
}

@RestController
@RequestMapping("/api/fixtures")
class FixtureController(
    private val tournamentService: TournamentService
) {

    @PostMapping("/{id}/start")
    fun startFixture(@PathVariable id: Long): ResponseEntity<FixtureResponse> =
        ResponseEntity.ok(FixtureResponse.from(tournamentService.setFixtureInProgress(id)))

    @PostMapping("/{id}/result")
    fun submitResult(
        @PathVariable id: Long,
        @RequestBody request: SubmitResultRequest
    ): ResponseEntity<FixtureResponse> {
        val fixture = tournamentService.submitResult(
            fixtureId = id,
            homeLegsWon = request.homeLegsWon,
            awayLegsWon = request.awayLegsWon,
            homePlayerAverage = request.homePlayerAverage,
            awayPlayerAverage = request.awayPlayerAverage,
            autodartsGameId = request.autodartsGameId
        )
        return ResponseEntity.ok(FixtureResponse.from(fixture))
    }
}

/**
 * Receives automatic game results from the browser extension content script.
 * Matches the completed Autodarts game to a pending league fixture by player usernames.
 */
@RestController
@RequestMapping("/api/autodarts")
class AutodartsIntegrationController(
    private val tournamentService: TournamentService
) {

    @PostMapping("/game-result")
    fun handleGameResult(@RequestBody request: AutodartsGameResultRequest): ResponseEntity<Any> {
        val fixture = tournamentService.submitAutodartsGameResult(request)
        return if (fixture != null) {
            ResponseEntity.ok(FixtureResponse.from(fixture))
        } else {
            ResponseEntity.ok(mapOf("matched" to false, "message" to "No matching pending fixture found"))
        }
    }
}
