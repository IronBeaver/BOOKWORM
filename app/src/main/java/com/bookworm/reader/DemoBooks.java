package com.bookworm.reader;

/** Original fictional samples; no imported content. */
public final class DemoBooks {
    public static final class Book {
        public final String title, author, description;
        public final int color;
        Book(String title, String author, String description, int color) {
            this.title = title; this.author = author;
            this.description = description; this.color = color;
        }
    }
    private static final Book[] BOOKS = {
        new Book("The Lantern Orchard", "Mira Alder", "A keeper of forgotten lanterns discovers an orchard that blooms only after sunset.", 0xff644331),
        new Book("Maps of Quiet Rain", "Tobin Vale", "An apprentice cartographer follows the changing streets of a city built around the weather.", 0xff304d60),
        new Book("The Clockmaker's Meadow", "Elin Moss", "A village clockmaker finds that the seasons have begun arriving out of order.", 0xff4c5537),
        new Book("Letters from Ember Bay", "Soren Wren", "Unsent letters bring two strangers together in a small harbour town.", 0xff673c40),
        new Book("A Pocketful of North", "Nessa Flint", "A wandering courier carries a compass that points toward whatever its owner has lost.", 0xff42456a),
        new Book("When the Rooftops Bloom", "Oren Thistle", "Neighbours turn their city's rooftops into gardens, one unlikely friendship at a time.", 0xff3b5a50)
    };
    private DemoBooks() { }
    public static int count() { return BOOKS.length; }
    public static Book get(int index) { return BOOKS[index]; }
}
