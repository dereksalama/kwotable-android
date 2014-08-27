package com.dereksalama.kwotabl;

/**
 * Data Object for quotes from server
 */
public class QuoteResponseData {

    public QuoteResponseData() {} // for gson

    public QuoteResponseData(String author, String quote) {
        this.quote = quote;
        this.author = author;
    }

    private String quote;

    private String author;

    /**
     * @return the quote
     */
    public String getQuote() {
        return quote;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    @Override
    public String toString() {
        return quote + " - " + author;
    }

}
