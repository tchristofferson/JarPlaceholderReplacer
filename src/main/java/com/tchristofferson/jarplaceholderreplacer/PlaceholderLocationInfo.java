package com.tchristofferson.jarplaceholderreplacer;

import java.util.*;

public class PlaceholderLocationInfo {

    private final String className;
    private final Set<String> containedPlaceholders;

    public PlaceholderLocationInfo(String className) {
        this.className = className;
        this.containedPlaceholders = new HashSet<>();
    }

    PlaceholderLocationInfo(String className, String... containedPlaceholders) {
        this.className = className;
        this.containedPlaceholders = new HashSet<>();
        this.containedPlaceholders.addAll(Arrays.asList(containedPlaceholders));
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

    @Override
    public String toString() {
        return "PlaceholderLocationInfo{" +
            "className='" + className + '\'' +
            ", containedPlaceholders=" + containedPlaceholders +
            '}';
    }
}
