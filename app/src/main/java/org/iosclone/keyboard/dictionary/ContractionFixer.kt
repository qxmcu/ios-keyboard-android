package org.iosclone.keyboard.dictionary

/**
 * High-precision dictionary of English contractions and irregular high-frequency phonetic typos.
 * In iOS QuickType, missing apostrophes are autocorrected deterministically with highest confidence.
 */
object ContractionFixer {

    private val contractionMap = mapOf(
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "im" to "I'm",
        "youre" to "you're",
        "theyre" to "they're",
        "weve" to "we've",
        "youve" to "you've",
        "theyve" to "they've",
        "didnt" to "didn't",
        "doesnt" to "doesn't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "hasnt" to "hasn't",
        "havent" to "haven't",
        "hadnt" to "hadn't",
        "couldnt" to "couldn't",
        "wouldnt" to "wouldn't",
        "shouldnt" to "shouldn't",
        "id" to "I'd",
        "ill" to "I'll",
        "youll" to "you'll",
        "theyll" to "they'll",
        "hell" to "he'll",
        "shell" to "she'll",
        "thats" to "that's",
        "whats" to "what's",
        "hows" to "how's",
        "wheres" to "where's",
        "whos" to "who's",
        "theres" to "there's",
        "heres" to "here's",
        "lets" to "let's",
        "itll" to "it'll",
        "thatll" to "that'll",
        "gonna" to "going to",
        "wanna" to "want to",
        "gotta" to "got to",
        "kinda" to "kind of"
    )

    private val commonMisspellings = mapOf(
        "definately" to "definitely",
        "seperate" to "separate",
        "recieve" to "receive",
        "wierd" to "weird",
        "untill" to "until",
        "calender" to "calendar",
        "alot" to "a lot",
        "truely" to "truly",
        "thier" to "their",
        "tomorow" to "tomorrow",
        "tommorrow" to "tomorrow",
        "embarass" to "embarrass",
        "occured" to "occurred",
        "untill" to "until",
        "goverment" to "government",
        "accomodate" to "accommodate",
        "beleive" to "believe",
        "foriegn" to "foreign",
        "neccessary" to "necessary",
        "necessery" to "necessary",
        "reccomend" to "recommend",
        "recomend" to "recommend",
        "unfortunatly" to "unfortunately",
        "teh" to "the",
        "thsi" to "this",
        "wiht" to "with",
        "waht" to "what",
        "taht" to "that",
        "fro" to "for",
        "nad" to "and",
        "adn" to "and",
        "becuase" to "because",
        "beacuse" to "because",
        "shoudl" to "should",
        "woudl" to "would",
        "coudl" to "could",
        "abotu" to "about",
        "pelase" to "please",
        "thansk" to "thanks",
        "peopel" to "people"
    )

    fun getFix(word: String): String? {
        val lower = word.lowercase()
        return contractionMap[lower] ?: commonMisspellings[lower]
    }
}
