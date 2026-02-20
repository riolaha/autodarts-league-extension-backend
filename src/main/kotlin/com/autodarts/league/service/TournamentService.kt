package com.autodarts.league.service

import com.autodarts.league.domain.*
import com.autodarts.league.dto.AutodartsGameResultRequest
import com.autodarts.league.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TournamentService(
    private val tournamentRepository: TournamentRepository,
    private val playerRepository: PlayerRepository,
    private val tournamentPlayerRepository: TournamentPlayerRepository,
    private val fixtureRepository: FixtureRepository,
    private val scheduleGenerator: ScheduleGenerator,
    private val standingsService: StandingsService
) {

    fun getAllTournaments(): List<Tournament> =
        tournamentRepository.findAll().sortedByDescending { it.createdAt }

    fun createTournament(
        name: String,
        gameMode: String = "501",
        legsPerMatch: Int = 3,
        roundsPerPlayer: Int = 2,
        playerIds: List<Long>
    ): Tournament {
        require(playerIds.size >= 2) { "Need at least 2 players" }
        require(roundsPerPlayer >= 1) { "Need at least 1 round per player" }

        val tournament = tournamentRepository.save(
            Tournament(
                name = name,
                gameMode = gameMode,
                legsPerMatch = legsPerMatch,
                roundsPerPlayer = roundsPerPlayer
            )
        )

        val players = playerRepository.findAllById(playerIds)
        players.forEachIndexed { index, player ->
            tournamentPlayerRepository.save(
                TournamentPlayer(
                    tournament = tournament,
                    player = player,
                    seed = index
                )
            )
        }

        val fixtures = scheduleGenerator.generateSchedule(tournament, players, roundsPerPlayer)
        fixtureRepository.saveAll(fixtures)

        return tournament
    }

    fun getTournament(id: Long): Tournament =
        tournamentRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Tournament not found") }

    fun deleteTournament(id: Long) {
        val tournament = getTournament(id)
        fixtureRepository.deleteByTournamentId(tournament.id!!)
        tournamentPlayerRepository.deleteByTournamentId(tournament.id!!)
        tournamentRepository.delete(tournament)
    }

    fun getFixtures(tournamentId: Long): List<Fixture> =
        fixtureRepository.findByTournamentIdOrderByRoundNumberAsc(tournamentId)

    fun getFixturesByRound(tournamentId: Long, roundNumber: Int): List<Fixture> =
        fixtureRepository.findByTournamentIdAndRoundNumber(tournamentId, roundNumber)

    fun getStandings(tournamentId: Long): List<StandingsEntry> {
        val tournament = getTournament(tournamentId)
        return standingsService.calculateStandings(
            tournamentId,
            tournament.pointsWin,
            tournament.pointsDraw,
            tournament.pointsLoss
        )
    }

    fun startTournament(tournamentId: Long): Tournament {
        val tournament = getTournament(tournamentId)
        tournament.status = TournamentStatus.IN_PROGRESS
        return tournamentRepository.save(tournament)
    }

    fun submitResult(
        fixtureId: Long,
        homeLegsWon: Int,
        awayLegsWon: Int,
        homePlayerAverage: Double? = null,
        awayPlayerAverage: Double? = null,
        autodartsGameId: String? = null
    ): Fixture {
        val fixture = fixtureRepository.findById(fixtureId)
            .orElseThrow { IllegalArgumentException("Fixture not found") }

        require(fixture.status != FixtureStatus.COMPLETED) { "Fixture already completed" }

        fixture.homeLegsWon = homeLegsWon
        fixture.awayLegsWon = awayLegsWon
        fixture.homePlayerAverage = homePlayerAverage
        fixture.awayPlayerAverage = awayPlayerAverage
        fixture.autodartsGameId = autodartsGameId
        fixture.status = FixtureStatus.COMPLETED
        fixture.playedAt = LocalDateTime.now()

        val saved = fixtureRepository.save(fixture)

        // Auto-complete tournament when all fixtures are done
        val allFixtures = fixtureRepository.findByTournamentIdOrderByRoundNumberAsc(fixture.tournament.id!!)
        if (allFixtures.all { it.status == FixtureStatus.COMPLETED }) {
            val tournament = fixture.tournament
            tournament.status = TournamentStatus.COMPLETED
            tournamentRepository.save(tournament)
        }

        return saved
    }

    fun setFixtureInProgress(fixtureId: Long): Fixture {
        val fixture = fixtureRepository.findById(fixtureId)
            .orElseThrow { IllegalArgumentException("Fixture not found") }
        fixture.status = FixtureStatus.IN_PROGRESS
        return fixtureRepository.save(fixture)
    }

    fun getNextPendingFixture(tournamentId: Long): Fixture? {
        val fixtures = fixtureRepository.findByTournamentIdOrderByRoundNumberAsc(tournamentId)
        return fixtures.firstOrNull { it.status == FixtureStatus.PENDING || it.status == FixtureStatus.IN_PROGRESS }
    }

    /**
     * Called by the browser extension content script when an Autodarts game finishes.
     * Matches the game to a pending fixture by player Autodarts usernames, then records the result.
     * Prefers UUID matching (real accounts) over username matching (local/guest).
     * When matched by username but a UUID is now known, saves it for future lookups.
     * Returns null if no matching pending fixture was found (game is not part of this tournament).
     */
    fun submitAutodartsGameResult(request: AutodartsGameResultRequest): Fixture? {
        // Try UUID first (real accounts), fall back to username (local/guest games)
        var homePlayer = request.homePlayerUserId?.let { playerRepository.findByAutodartsUserId(it) }
            ?: playerRepository.findByAutodartsUsername(request.homePlayerUsername)
            ?: return null

        var awayPlayer = request.awayPlayerUserId?.let { playerRepository.findByAutodartsUserId(it) }
            ?: playerRepository.findByAutodartsUsername(request.awayPlayerUsername)
            ?: return null

        // If matched by username but we now have their UUID, persist it for faster future lookups
        if (homePlayer.autodartsUserId == null && request.homePlayerUserId != null)
            homePlayer = playerRepository.save(homePlayer.copy(autodartsUserId = request.homePlayerUserId))
        if (awayPlayer.autodartsUserId == null && request.awayPlayerUserId != null)
            awayPlayer = playerRepository.save(awayPlayer.copy(autodartsUserId = request.awayPlayerUserId))

        // Determine which player was "home" in the Autodarts game
        // (UUID match takes priority; fall back to username)
        val isHomeFirst = request.homePlayerUserId?.let { it == homePlayer.autodartsUserId }
            ?: (homePlayer.autodartsUsername.equals(request.homePlayerUsername, ignoreCase = true))

        // Find an active tournament that contains both players
        val activeTournaments = tournamentRepository.findAll()
            .filter { it.status == TournamentStatus.IN_PROGRESS }

        for (tournament in activeTournaments) {
            val participants = tournamentPlayerRepository.findByTournamentId(tournament.id!!)
                .map { it.player.id }

            if (!participants.contains(homePlayer.id) || !participants.contains(awayPlayer.id)) continue

            // Find the pending fixture between these two players (either orientation)
            val fixtures = fixtureRepository.findByTournamentIdOrderByRoundNumberAsc(tournament.id!!)
            val fixture = fixtures.firstOrNull { f ->
                f.status != FixtureStatus.COMPLETED &&
                ((f.homePlayer.id == homePlayer.id && f.awayPlayer.id == awayPlayer.id) ||
                 (f.homePlayer.id == awayPlayer.id && f.awayPlayer.id == homePlayer.id))
            } ?: continue

            // Orient scores to match the fixture's home/away assignment
            val (fixtureHomeLegs, fixtureAwayLegs, fixtureHomeAvg, fixtureAwayAvg) =
                if (fixture.homePlayer.id == homePlayer.id && isHomeFirst) {
                    listOf(request.homeLegsWon, request.awayLegsWon,
                           request.homePlayerAverage, request.awayPlayerAverage)
                } else {
                    listOf(request.awayLegsWon, request.homeLegsWon,
                           request.awayPlayerAverage, request.homePlayerAverage)
                }

            return submitResult(
                fixtureId = fixture.id!!,
                homeLegsWon = fixtureHomeLegs as Int,
                awayLegsWon = fixtureAwayLegs as Int,
                homePlayerAverage = fixtureHomeAvg as Double?,
                awayPlayerAverage = fixtureAwayAvg as Double?,
                autodartsGameId = request.autodartsGameId
            )
        }

        return null
    }
}
