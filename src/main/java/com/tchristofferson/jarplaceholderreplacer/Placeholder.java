package com.tchristofferson.jarplaceholderreplacer;

public class Placeholder {

    private final String classPath;
    private final String toReplace;
    private final String replacement;

    public Placeholder(String classPath, String toReplace, String replacement) {
        this.classPath = classPath;
        this.toReplace = toReplace;
        this.replacement = replacement;
    }

    public String getClassPath() {
        return classPath;
    }

    public String getToReplace() {
        return toReplace;
    }

    public String getReplacement() {
        return replacement;
    }
}
