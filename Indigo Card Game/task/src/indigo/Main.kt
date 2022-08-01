package indigo

import kotlin.system.exitProcess

const val MIN_CARDS_WHEN_CAN_BE_OFFSUITED = 4
const val NUMBER_OF_TURNS = 6
const val NUMBER_OF_DEALS = 4

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
// 1. All ranks should have points declared. So in the result 101-109 lines can be simplified. Tip: use sumOf method

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

// Deck class can be simplified to:
//class Deck {
//    var cardDeck: List<Card> = initialize()//
//
//    private fun initialize(): List<Card> {
//        return Suits.values().map{ suit -> Ranks.values().map{rank -> Card(rank.rank, suit.suit)}}.flatten().shuffled()
//    }
//}
// 1. Initialize cardDeck on object creation.
// 2. Simplifies initialize method.
// 3. Shuffle method is not needed any more.
// 4. No need to use open keyword. Open is not needed here.
// 5. Mutable state removed.


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
// 1. playingDeck should be injected into Table constructor. Not created inside body class. Avoid such a behavior.
// 2. Mutable state should be removed.
// 3. 3 is magic number here.

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
// 1. didWinLast could be renamed to wonLast. Also, this variable might be not nullable.

    fun printCardsInHand(): String {
        val string = StringBuilder()
        for (i in hand.indices) {
            string.append("${i + 1})${hand[i]} ")
        }
        return string.toString()
    }
}

class CPU : Player() {
    // List of cards which can win the cards on the table
    val candidateCards: MutableList<Card> = mutableListOf()

    fun printHand(): String {
        val string = StringBuilder()
        for (i in hand.indices) {
            string.append("${hand[i]} ")
        }
        return string.toString()
    }
}

// 1. No need to define printHand method again. It is defined already in parent class Player.

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
        whatIsOnTablePrinter()
        ripLastCards()
        awardLast3Points()
        println("Game Over")
    }
    // 4deals * (6+6cards per deal) = 48 + 4 first cards on table = 52
    private fun mainGameLoop() {
        if (isPlayerBegins) {
            repeat(NUMBER_OF_DEALS) {
                dealCards()
                repeat(NUMBER_OF_TURNS) {
                    playerTurn()
                    _CPUTurn()
                }
            }
            // CPU begins
        } else {
            repeat(4) { // magic number
                dealCards()
                repeat(6) { // magic number
                    _CPUTurn()
                    playerTurn()
                }
            }
        }
    }

    private fun whatIsOnTablePrinter() {
        if (table.cardsOnTable.isEmpty()) {
            println("No cards on the table")
        } else {
            println("${table.cardsOnTable.size} cards on the table, and the top card is ${table.cardsOnTable.last()}")
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
            player1.points += 3 // magicNumbers
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
        for (i in 0..11 step 2) { // magic Numbers
            player1.hand.add(table.playingDeck.cardDeck[i]) // try to create inside Player class method "dealCard"
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
    // 1. You should not interpret if player is CPU by veryfing class type. You could add isCpu flag to Player class.

    // puts card on the table and delete it from the hand
    private fun putCardOnTable(player: Player, element: Card) {
        table.cardsOnTable.add(element)
        player.hand.remove(element)
    }

    private fun smartCPU() { // too many ifs in this method
        if (table.cardsOnTable.isEmpty()) {
            playSameSuitsRanksOrRandomCard(player2, player2.hand)
        } else {
            chooseCandidateCards()
            if (player2.candidateCards.size > 1) {
                playSameSuitsRanksOrRandomCard(player2, player2.candidateCards)
                takeCardsToAllWonCardsAndReturnScore(player2, player1)
            } else {
                if (player2.candidateCards.isNotEmpty()) {
                    putCardOnTable(player2, player2.candidateCards.random())
                    println("Computer plays ${table.cardsOnTable.last()}")
                    takeCardsToAllWonCardsAndReturnScore(player2, player1)
                } else {
                    playSameSuitsRanksOrRandomCard(player2, player2.hand)
                }
            }
        }
        player2.candidateCards.clear()
    }

    private fun playSameSuitsRanksOrRandomCard(player: Player, cards: MutableList<Card>) {
        if (cards.size > MIN_CARDS_WHEN_CAN_BE_OFFSUITED) {
            playRandomCardWithTheSameSuit(player, cards)
        } else {
            if (checkIfThereAreSameSuitedCards(cards)) {
                playRandomCardWithTheSameSuit(player, cards)
            } else if (checkIfThereAreSameRankedCards(cards)) {
                playRandomCardWithTheSameRank(player, cards)
            } else {
                putCardOnTable(player, cards.random())
                println("Computer plays ${table.cardsOnTable.last()}")
            }

        }
    }

    //chooses if there are any cards which can win the cards in the table and adds them to candidateCards
    private fun chooseCandidateCards() {
        for (i in player2.hand.indices) {
            if (player2.hand[i].rank == table.cardsOnTable.last().rank || player2.hand[i].suit == table.cardsOnTable.last().suit) {
                player2.candidateCards.add(player2.hand[i])
            } else continue
        }
    }
// checks if there are at least 2 cards in hand with the same suit and plays the second one

    private fun checkIfThereAreSameSuitedCards(cards: MutableList<Card>): Boolean {
        var counter = 0
        for (suit in Suits.values()) {
            counter = 0
            for (i in cards.indices) {
                if (suit.suit == cards[i].suit) {
                    if (counter < 1) {
                        counter++
                    } else {
                        return true
                    }
                } else continue
            }
        }
        return false
    }

    private fun playRandomCardWithTheSameSuit(player: Player, cards: MutableList<Card>) {
        var counter = 0
        for (suit in Suits.values()) {
            counter = 0
            for (i in cards.indices) {
                if (suit.suit == cards[i].suit) {
                    if (counter < 1) {
                        counter++
                    } else {
                        putCardOnTable(player, cards[i])
                        println("Computer plays ${table.cardsOnTable.last()}")
                        return
                    }
                } else continue
            }
        }
    }

    private fun checkIfThereAreSameRankedCards(cards: MutableList<Card>): Boolean {
        var counter = 0
        for (rank in Ranks.values()) {
            counter = 0
            for (i in cards.indices) {
                if (rank.rank == cards[i].rank) {
                    if (counter < 1) {
                        counter++
                    } else {
                        return true
                    }
                } else continue
            }
        }
        return false
    }

    // checks if there are at least 2 cards in hand with the same rank and plays the second one
    private fun playRandomCardWithTheSameRank(player: Player, cards: MutableList<Card>) {
        var counter = 0
        for (rank in Ranks.values()) {
            counter = 0
            for (i in cards.indices) {
                if (rank.rank == cards[i].rank) {
                    if (counter < 1) {
                        counter++
                    } else {
                        putCardOnTable(player, cards[i])
                        println("Computer plays ${table.cardsOnTable.last()}")
                        return
                    }
                } else continue
            }
        }
    }
     // playRandomCardWithTheSameRank &&  checkIfThereAreSameRankedCards && playRandomCardWithTheSameSuit && checkIfThereAreSameSuitedCardshas most of the logic the same. Try to simplify and create more generic method.


    // checks if there are any cards on the table and if suits are equal, if they are, puts card, which was chosen by the player on the table
    // takes cards to player's lastWonCards, counts score and prints it, takes cards to allWonCards and clears lastWonCards
    //prints output and marks player as last winner by boolean variable, if they are not, puts chosen card on the table
    private fun playCardAndCheckResult_Player(input: Int) { // snake_case mixed with CamelCase
        if (table.cardsOnTable.isNotEmpty() && (player1.hand[input - 1].suit == table.cardsOnTable.last().suit || player1.hand[input - 1].rank == table.cardsOnTable.last().rank)){ // this conditionn should be moved to separate method
            putCardOnTable(player1 ,player1.hand[input - 1])
            takeCardsToAllWonCardsAndReturnScore(player1, player2)
        } else {
            putCardOnTable(player1, player1.hand[input - 1])
        }
    }

    private fun playerTurn() {
        whatIsOnTablePrinter()
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

    private fun _CPUTurn() { // should start with lowercase. Also there is no need to use _ underscore in begining of this method name.
        whatIsOnTablePrinter()
        println(player2.printHand())
        smartCPU()
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


// General Notes:
// You should avoid if possible using MutableLists. Try to use Immutable Lists.
// Try to create short methods, doing only one thing.
// Try to create some tests using JUnit
// Move methods strictly connected to Objects to classes. For example if method modifies player state most probably it should be placed inside Player class.
// Your program contains a lot of ifs/elseifs/else.
// Consider adding winner flag to Game class and recreate new Game object on every new game.