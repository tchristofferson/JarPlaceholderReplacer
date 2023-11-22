package com.tchristofferson.jarplaceholderreplacer;

public class Placeholder {

    private final String text;//Text to find and replace
    private final String replacement;//The replacement text

    public Placeholder(String text, String replacement) {
        this.text = text;
        this.replacement = replacement;
    }

    public String getText() {
        return text;
    }

    public String getReplacement() {
        return replacement;
    }
}
