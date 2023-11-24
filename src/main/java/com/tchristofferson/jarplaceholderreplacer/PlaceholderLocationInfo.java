package com.tchristofferson.jarplaceholderreplacer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlaceholderLocationInfo {

    private final String className;
    private final Set<String> containedPlaceholders;

    public PlaceholderLocationInfo(String className) {
        this.className = className;
        this.containedPlaceholders = new HashSet<>();
    }

    public String getClassName() {
        return className;
    }

    public Set<String> getContainedPlaceholders() {
        return Collections.unmodifiableSet(containedPlaceholders);
    }

    public void addPlaceholder(String placeholder) {
        containedPlaceholders.add(placeholder);
    }
}
