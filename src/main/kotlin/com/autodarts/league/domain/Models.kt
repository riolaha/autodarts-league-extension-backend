package com.autodarts.league.domain

import jakarta.persistence.*
import java.time.LocalDateTime

enum class TournamentStatus {
    CREATED, IN_PROGRESS, COMPLETED, ENDED
}

enum class FixtureStatus {
    PENDING, IN_PROGRESS, COMPLETED
}

@Entity
@Table(name = "tournament")
data class Tournament(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    val name: String,
    val gameMode: String = "501",
    val legsPerMatch: Int = 3,
    val roundsPerPlayer: Int = 2,
    val pointsWin: Int = 3,
    val pointsDraw: Int = 1,
    val pointsLoss: Int = 0,
    
    @Enumerated(EnumType.STRING)
    var status: TournamentStatus = TournamentStatus.CREATED,
    
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "player")
data class Player(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    val displayName: String,
    val autodartsUsername: String,
    /** Autodarts account UUID — populated when player has a real account */
    val autodartsUserId: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "tournament_player")
data class TournamentPlayer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    val tournament: Tournament,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    val player: Player,
    
    val seed: Int
)

@Entity
@Table(name = "fixture")
data class Fixture(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    val tournament: Tournament,
    
    val roundNumber: Int,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "home_player_id")
    val homePlayer: Player,
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "away_player_id")
    val awayPlayer: Player,
    
    var homeLegsWon: Int? = null,
    var awayLegsWon: Int? = null,

    var homePlayerAverage: Double? = null,
    var awayPlayerAverage: Double? = null,
    var autodartsGameId: String? = null,

    @Enumerated(EnumType.STRING)
    var status: FixtureStatus = FixtureStatus.PENDING,

    var playedAt: LocalDateTime? = null
)
