package com.autodarts.league.repository

import com.autodarts.league.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TournamentRepository : JpaRepository<Tournament, Long>

@Repository
interface PlayerRepository : JpaRepository<Player, Long> {
    fun findByAutodartsUsername(username: String): Player?
    fun findByAutodartsUserId(userId: String): Player?
}

@Repository
interface TournamentPlayerRepository : JpaRepository<TournamentPlayer, Long> {
    fun findByTournamentId(tournamentId: Long): List<TournamentPlayer>

    @Modifying
    @Query("DELETE FROM TournamentPlayer tp WHERE tp.tournament.id = :tournamentId")
    fun deleteByTournamentId(tournamentId: Long)
}

@Repository
interface FixtureRepository : JpaRepository<Fixture, Long> {
    fun findByTournamentIdOrderByRoundNumberAsc(tournamentId: Long): List<Fixture>
    fun findByTournamentIdAndRoundNumber(tournamentId: Long, roundNumber: Int): List<Fixture>

    @Modifying
    @Query("DELETE FROM Fixture f WHERE f.tournament.id = :tournamentId")
    fun deleteByTournamentId(tournamentId: Long)

    @Query("""
        SELECT f FROM Fixture f
        WHERE f.tournament.id = :tournamentId
        AND f.status = 'COMPLETED'
    """)
    fun findCompletedFixtures(tournamentId: Long): List<Fixture>
}
