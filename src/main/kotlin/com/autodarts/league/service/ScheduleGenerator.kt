package com.autodarts.league.service

import com.autodarts.league.domain.Fixture
import com.autodarts.league.domain.Player
import com.autodarts.league.domain.Tournament
import org.springframework.stereotype.Service

@Service
class ScheduleGenerator {
    
    fun generateSchedule(tournament: Tournament, players: List<Player>, roundsPerPlayer: Int): List<Fixture> {
        require(players.size >= 2) { "Need at least 2 players" }
        require(roundsPerPlayer >= 1) { "Need at least 1 round per player" }
        
        val fixtures = mutableListOf<Fixture>()
        val n = players.size
        var currentRoundStart = 1
        
        val baseFixtures = generateSingleRoundRobin(tournament, players, startRound = 1)
        fixtures.addAll(baseFixtures)
        currentRoundStart = n
        
        for (iteration in 2..roundsPerPlayer) {
            if (iteration % 2 == 0) {
                val reversedFixtures = reverseFixtures(tournament, baseFixtures, startRound = currentRoundStart)
                fixtures.addAll(reversedFixtures)
            } else {
                val repeatFixtures = baseFixtures.map { fixture ->
                    Fixture(
                        tournament = tournament,
                        roundNumber = currentRoundStart + (fixture.roundNumber - 1),
                        homePlayer = fixture.homePlayer,
                        awayPlayer = fixture.awayPlayer
                    )
                }
                fixtures.addAll(repeatFixtures)
            }
            currentRoundStart += (n - 1)
        }
        
        return fixtures
    }
    
    private fun generateSingleRoundRobin(
        tournament: Tournament, 
        players: List<Player>,
        startRound: Int
    ): List<Fixture> {
        val fixtures = mutableListOf<Fixture>()
        val n = players.size
        
        val teams: MutableList<Player?> = ArrayList(players)
        if (n % 2 != 0) teams.add(null)
        
        val numRounds = teams.size - 1
        val halfSize = teams.size / 2
        
        for (round in 0 until numRounds) {
            for (i in 0 until halfSize) {
                val home = teams[i]
                val away = teams[teams.size - 1 - i]
                
                if (home != null && away != null) {
                    fixtures.add(
                        Fixture(
                            tournament = tournament,
                            roundNumber = startRound + round,
                            homePlayer = home,
                            awayPlayer = away
                        )
                    )
                }
            }
            
            if (round < numRounds - 1) {
                val temp = teams[teams.size - 1]
                for (i in teams.size - 1 downTo 2) {
                    teams[i] = teams[i - 1]
                }
                teams[1] = temp
            }
        }
        
        return fixtures
    }
    
    private fun reverseFixtures(
        tournament: Tournament,
        fixtures: List<Fixture>,
        startRound: Int
    ): List<Fixture> {
        return fixtures.mapIndexed { index, fixture ->
            Fixture(
                tournament = tournament,
                roundNumber = startRound + (fixture.roundNumber - 1),
                homePlayer = fixture.awayPlayer,
                awayPlayer = fixture.homePlayer
            )
        }
    }
}
