package com.autodarts.league.dto

import com.autodarts.league.domain.Fixture
import com.autodarts.league.domain.FixtureStatus
import com.autodarts.league.domain.Player
import com.autodarts.league.domain.Tournament
import com.autodarts.league.service.StandingsEntry
import java.time.LocalDateTime

data class CreateTournamentRequest(
    val name: String,
    val gameMode: String = "501",
    val legsPerMatch: Int = 3,
    val roundsPerPlayer: Int = 2,
    val playerIds: List<Long>
)

data class CreatePlayerRequest(
    val displayName: String,
    val autodartsUsername: String,
    val autodartsUserId: String? = null
)

data class SubmitResultRequest(
    val homeLegsWon: Int,
    val awayLegsWon: Int,
    val homePlayerAverage: Double? = null,
    val awayPlayerAverage: Double? = null,
    val autodartsGameId: String? = null
)

/** Sent by the browser extension content script when an Autodarts game finishes. */
data class AutodartsGameResultRequest(
    val homePlayerUsername: String,
    val awayPlayerUsername: String,
    /** Autodarts account UUIDs — non-null for real accounts, null for local/guest games */
    val homePlayerUserId: String? = null,
    val awayPlayerUserId: String? = null,
    val homeLegsWon: Int,
    val awayLegsWon: Int,
    val homePlayerAverage: Double? = null,
    val awayPlayerAverage: Double? = null,
    val autodartsGameId: String? = null
)

data class TournamentResponse(
    val id: Long,
    val name: String,
    val gameMode: String,
    val legsPerMatch: Int,
    val roundsPerPlayer: Int,
    val status: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(tournament: Tournament) = TournamentResponse(
            id = tournament.id!!,
            name = tournament.name,
            gameMode = tournament.gameMode,
            legsPerMatch = tournament.legsPerMatch,
            roundsPerPlayer = tournament.roundsPerPlayer,
            status = tournament.status.name,
            createdAt = tournament.createdAt
        )
    }
}

data class PlayerResponse(
    val id: Long,
    val displayName: String,
    val autodartsUsername: String,
    val autodartsUserId: String? = null
) {
    companion object {
        fun from(player: Player) = PlayerResponse(
            id = player.id!!,
            displayName = player.displayName,
            autodartsUsername = player.autodartsUsername,
            autodartsUserId = player.autodartsUserId
        )
    }
}

data class FixtureResponse(
    val id: Long,
    val tournamentId: Long,
    val roundNumber: Int,
    val homePlayer: PlayerResponse,
    val awayPlayer: PlayerResponse,
    val homeLegsWon: Int?,
    val awayLegsWon: Int?,
    val homePlayerAverage: Double?,
    val awayPlayerAverage: Double?,
    val autodartsGameId: String?,
    val status: FixtureStatus,
    val playedAt: LocalDateTime?
) {
    companion object {
        fun from(fixture: Fixture) = FixtureResponse(
            id = fixture.id!!,
            tournamentId = fixture.tournament.id!!,
            roundNumber = fixture.roundNumber,
            homePlayer = PlayerResponse.from(fixture.homePlayer),
            awayPlayer = PlayerResponse.from(fixture.awayPlayer),
            homeLegsWon = fixture.homeLegsWon,
            awayLegsWon = fixture.awayLegsWon,
            homePlayerAverage = fixture.homePlayerAverage,
            awayPlayerAverage = fixture.awayPlayerAverage,
            autodartsGameId = fixture.autodartsGameId,
            status = fixture.status,
            playedAt = fixture.playedAt
        )
    }
}

data class StandingsResponse(
    val player: PlayerResponse,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val legsFor: Int,
    val legsAgainst: Int,
    val legsDifference: Int,
    val points: Int
) {
    companion object {
        fun from(entry: StandingsEntry) = StandingsResponse(
            player = PlayerResponse.from(entry.player),
            played = entry.played,
            wins = entry.wins,
            draws = entry.draws,
            losses = entry.losses,
            legsFor = entry.legsFor,
            legsAgainst = entry.legsAgainst,
            legsDifference = entry.legsDifference,
            points = entry.points
        )
    }
}
