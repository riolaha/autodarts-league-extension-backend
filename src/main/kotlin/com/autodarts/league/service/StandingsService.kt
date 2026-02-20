package com.autodarts.league.service

import com.autodarts.league.domain.Player
import com.autodarts.league.repository.FixtureRepository
import org.springframework.stereotype.Service

data class StandingsEntry(
    val player: Player,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val legsFor: Int,
    val legsAgainst: Int,
    val legsDifference: Int,
    val points: Int
)

@Service
class StandingsService(
    private val fixtureRepository: FixtureRepository
) {
    
    fun calculateStandings(tournamentId: Long, pointsWin: Int = 3, pointsDraw: Int = 1, pointsLoss: Int = 0): List<StandingsEntry> {
        val completedFixtures = fixtureRepository.findCompletedFixtures(tournamentId)
        
        val playerStats = mutableMapOf<Long, MutableMap<String, Int>>()
        val playerMap = mutableMapOf<Long, Player>()
        
        completedFixtures.forEach { fixture ->
            val homeLegs = fixture.homeLegsWon ?: 0
            val awayLegs = fixture.awayLegsWon ?: 0
            
            processPlayerStats(playerStats, playerMap, fixture.homePlayer, homeLegs, awayLegs, pointsWin, pointsDraw, pointsLoss)
            processPlayerStats(playerStats, playerMap, fixture.awayPlayer, awayLegs, homeLegs, pointsWin, pointsDraw, pointsLoss)
        }
        
        return playerStats.map { (playerId, stats) ->
            val player = playerMap[playerId]!!
            StandingsEntry(
                player = player,
                played = stats["played"] ?: 0,
                wins = stats["wins"] ?: 0,
                draws = stats["draws"] ?: 0,
                losses = stats["losses"] ?: 0,
                legsFor = stats["legsFor"] ?: 0,
                legsAgainst = stats["legsAgainst"] ?: 0,
                legsDifference = (stats["legsFor"] ?: 0) - (stats["legsAgainst"] ?: 0),
                points = stats["points"] ?: 0
            )
        }.sortedWith(
            compareByDescending<StandingsEntry> { it.points }
                .thenByDescending { it.legsDifference }
                .thenByDescending { it.legsFor }
        )
    }
    
    private fun processPlayerStats(
        playerStats: MutableMap<Long, MutableMap<String, Int>>,
        playerMap: MutableMap<Long, Player>,
        player: Player,
        legsFor: Int,
        legsAgainst: Int,
        pointsWin: Int,
        pointsDraw: Int,
        pointsLoss: Int
    ) {
        val playerId = player.id!!
        playerMap[playerId] = player
        
        val stats = playerStats.getOrPut(playerId) {
            mutableMapOf(
                "played" to 0,
                "wins" to 0,
                "draws" to 0,
                "losses" to 0,
                "legsFor" to 0,
                "legsAgainst" to 0,
                "points" to 0
            )
        }
        
        stats["played"] = (stats["played"] ?: 0) + 1
        stats["legsFor"] = (stats["legsFor"] ?: 0) + legsFor
        stats["legsAgainst"] = (stats["legsAgainst"] ?: 0) + legsAgainst
        
        when {
            legsFor > legsAgainst -> {
                stats["wins"] = (stats["wins"] ?: 0) + 1
                stats["points"] = (stats["points"] ?: 0) + pointsWin
            }
            legsFor == legsAgainst -> {
                stats["draws"] = (stats["draws"] ?: 0) + 1
                stats["points"] = (stats["points"] ?: 0) + pointsDraw
            }
            else -> {
                stats["losses"] = (stats["losses"] ?: 0) + 1
                stats["points"] = (stats["points"] ?: 0) + pointsLoss
            }
        }
    }
}
