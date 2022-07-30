package indigo

import kotlin.system.exitProcess

enum class Suits(val suit: String) {
    CLUB("♣"),
    DIAMOND("♦"),
    HEART("♥"),
    SPADE("♠"),
}

enum class Ranks(val rank: String, val points: Int = 0) {
    KING("K", 1),
    QUEEN("Q", 1),
    JACK("J", 1),
    TEN("10", 1),
    NINE("9"),
    EIGHT("8"),
    SEVEN("7"),
    SIX("6"),
    FIVE("5"),
    FOUR("4"),
    THREE("3"),
    TWO("2"),
    ACE("A", 1)
}

class Card(val rank: String, val suit: String) {
    override fun toString(): String {
        return "$rank$suit"
    }
}

open class Deck {
    var cardDeck: MutableList<Card> = mutableListOf()

    // function build up a new deck of 52 cards using enum class Suits & Ranks
    fun initialize() {
        cardDeck = mutableListOf()
        for (suit in Suits.values()) {
            for (rank in Ranks.values()) {
                cardDeck.add(Card(Ranks.valueOf("$rank").rank, Suits.valueOf("$suit").suit))
            }
        }
    }

    fun shuffle() {
        cardDeck.shuffle()
    }
}

class Table {
    val playingDeck: Deck = Deck()
    val cardsOnTable = mutableListOf<Card>()

    // function initialize new, shuffled deck of playing cards and put 4 first cards on the table
    fun initCards() {
        playingDeck.initialize()
        playingDeck.shuffle()
        for (i in 0..3) {
            cardsOnTable.add(playingDeck.cardDeck[i])
        }
        playingDeck.cardDeck.removeAll(cardsOnTable)
        print("Initial cards on the table: ")
        printDeck(cardsOnTable)
        println()
    }
}

open class Player {
    val hand: MutableList<Card> = mutableListOf()
    val allWonCards: MutableList<Card> = mutableListOf()
    val lastWonCards: MutableList<Card> = mutableListOf()
    var points = 0
    var didWinLast: Boolean? = null

    fun countPoints() {
        for (i in lastWonCards.indices) {
            points += when (lastWonCards[i].rank) {
                "A" -> Ranks.ACE.points
                "K" -> Ranks.KING.points
                "Q" -> Ranks.QUEEN.points
                "J" -> Ranks.JACK.points
                "10" -> Ranks.TEN.points
                else -> continue
            }
        }
    }

    fun printCardsInHand(): String {
        val string = StringBuilder()
        for (i in hand.indices) {
            string.append("${i + 1})${hand[i]} ")
        }
        return string.toString()
    }
}

class CPU : Player() {
    val candidateCards: MutableList<Card> = mutableListOf()
}

class Game(
    private val player1: Player,
    private val player2: CPU,
    private val table: Table,
    private val isPlayerBegins: Boolean
) {

    fun startGame() {
        table.initCards()
        //if player begins
        mainGameLoop()
        if (table.cardsOnTable.isEmpty()) {
            println("No cards on the table")
        } else {
            println("${table.cardsOnTable.size} cards on the table, and the top card is ${table.cardsOnTable.last()}")
        }
        ripLastCards()
        awardLast3Points()
        println("Game Over")
    }
    // 4deals * (6+6cards per deal) = 48 + 4 first cards on table = 52
    private fun mainGameLoop() {
        if (isPlayerBegins) {
            repeat(4) {
                dealCards()
                repeat(6) {
                    playerTurn()
                    _CPUTurn()
                }
            }
            // CPU begins
        } else {
            repeat(4) {
                dealCards()
                repeat(6) {
                    _CPUTurn()
                    playerTurn()
                }
            }
        }
    }

    private fun ripLastCards() {
        when {
            player1.didWinLast == true -> {
                takeCardsToAllWonCards(player1, player2)
            }
            player2.didWinLast == true -> {
                takeCardsToAllWonCards(player2, player1)
            }
            player1.didWinLast == null && player2.didWinLast == null -> {
                if (isPlayerBegins) {
                    takeCardsToAllWonCards(player1, player2)
                } else {
                    takeCardsToAllWonCards(player2, player1)
                }
            }
        }
    }

    private fun awardLast3Points() {
        if (player1.allWonCards.size > player2.allWonCards.size) {
            player1.points += 3
            printScore(player1, player2)
        } else if (player2.allWonCards.size > player1.allWonCards.size) {
            player2.points += 3
            printScore(player1, player2)
        } else {
            if (isPlayerBegins) {
                player1.points += 3
                printScore(player1, player2)
            } else {
                player2.points += 3
                printScore(player1, player2)
            }
        }
    }

    // puts alternately 6 cards in player's and CPU's hand
    private fun dealCards() {
        for (i in 0..11 step 2) {
            player1.hand.add(table.playingDeck.cardDeck[i])
            player2.hand.add(table.playingDeck.cardDeck[i + 1])
        }
        table.playingDeck.cardDeck.removeAll(player1.hand)
        table.playingDeck.cardDeck.removeAll(player2.hand)
    }

    // moves cards from the table to winner's allWonCards, also counts and prints the score, and marks boolean variables
    // for winner as true and for loser as false

    private fun takeCardsToAllWonCards(winner: Player, loser: Player) {
        winner.lastWonCards.addAll(table.cardsOnTable)
        table.cardsOnTable.clear()
        winner.countPoints()
        winner.allWonCards.addAll(winner.lastWonCards)
        winner.lastWonCards.clear()
        winner.didWinLast = true
        loser.didWinLast = false
    }

    private fun printScore(player1: Player, player2: CPU) {
        println(
            "Score: Player ${player1.points} - Computer ${player2.points}\n" +
                    "Cards: Player ${player1.allWonCards.size} - Computer ${player2.allWonCards.size}"
        )
    }

    private fun takeCardsToAllWonCardsAndReturnScore(winner: Player, loser: Player) {
        takeCardsToAllWonCards(winner, loser)
        if (winner !is CPU) println("Player wins cards") else println("Computer wins cards")
        println(
            "Score: Player ${player1.points} - Computer ${player2.points}\n" +
                    "Cards: Player ${player1.allWonCards.size} - Computer ${player2.allWonCards.size}"
        )
    }

    // checks if there are any cards on the table and if suits are equal,
    // if they are, puts the top card on the table
    // takes cards to CPU's lastWonCards, counts score and prints it, takes cards to allWonCards and clears lastWonCards
    //prints output and marks CPU as last winner by boolean variable, if they are not, puts top card on the table
    private fun playCardAndCheckResult_CPU() {
        if (table.cardsOnTable.isNotEmpty() && (player2.hand.last().suit == table.cardsOnTable.last().suit || player2.hand.last().rank == table.cardsOnTable.last().rank)) {
            table.cardsOnTable.add(player2.hand.last())
            player2.hand.remove(player2.hand.last())
            println("Computer plays ${table.cardsOnTable.last()}")
            takeCardsToAllWonCardsAndReturnScore(player2,player1)
        } else {
            table.cardsOnTable.add(player2.hand.last())
            player2.hand.remove(player2.hand.last())
            println("Computer plays ${table.cardsOnTable.last()}")
        }
    }

    // checks if there are any cards on the table and if suits are equal, if they are, puts card, which was chosen by the player on the table
    // takes cards to player's lastWonCards, counts score and prints it, takes cards to allWonCards and clears lastWonCards
    //prints output and marks player as last winner by boolean variable, if they are not, puts chosen card on the table
    private fun playCardAndCheckResult_Player(input: Int) {
        if (table.cardsOnTable.isNotEmpty() && (player1.hand[input - 1].suit == table.cardsOnTable.last().suit || player1.hand[input - 1].rank == table.cardsOnTable.last().rank)){
            table.cardsOnTable.add(player1.hand[input - 1])
            player1.hand.removeAt(input - 1)
            takeCardsToAllWonCardsAndReturnScore(player1, player2)
        } else {
            table.cardsOnTable.add(player1.hand[input - 1])
            player1.hand.removeAt(input - 1)
        }
    }

    private fun playerTurn() {
        if (table.cardsOnTable.isEmpty()) {
            println("No cards on the table")
        } else {
            println("${table.cardsOnTable.size} cards on the table, and the top card is ${table.cardsOnTable.last()}")
        }
        println("Cards in hand: " + player1.printCardsInHand())
        var chosenCard: Any? = null
        loop@ do {
            try {
                println("Choose a card to play (1-${player1.hand.lastIndex + 1}):")
                chosenCard = ifExit(readln()).toInt()
            } catch (e: Exception) {
                continue@loop
            }
        } while (chosenCard !in 1..player1.hand.size)
        playCardAndCheckResult_Player(chosenCard.toString().toInt())
        println()

    }

    private fun _CPUTurn() {
        if (table.cardsOnTable.isEmpty()) {
            println("No cards on the table")
        } else {
            println("${table.cardsOnTable.size} cards on the table, and the top card is ${table.cardsOnTable.last()}")
        }
        println("Cards in hand: " + player2.printCardsInHand())
        playCardAndCheckResult_CPU()
        println()
    }
}

fun main() {
    println("Indigo Card Game")
    var isPlayerBegins: Boolean
    do {
        println("Play first?")
        val isFirstGame = readln().lowercase()
        isPlayerBegins = whoStarts(isFirstGame)
    } while (isFirstGame != "yes" && isFirstGame != "no")
    val newGame = Game(Player(), CPU(), Table(), isPlayerBegins)
    newGame.startGame()
}

fun whoStarts(action: String): Boolean {
    return when (action) {
        "yes" -> true
        "no" -> false
        "exit" -> exit()
        else -> true
    }
}

fun printDeck(deck: MutableList<Card>) {
    val deckAsString = StringBuilder()
    for (i in deck.indices) {
        deckAsString.append(deck[i].rank + deck[i].suit + " ")
    }
    return println(deckAsString.toString())
}

fun exit(): Nothing {
    println("Bye")
    exitProcess(0)
}

fun ifExit(input: String): String {
    if (input == "exit") {
        println("Game Over")
        exitProcess(0)
    }
    return input
}


