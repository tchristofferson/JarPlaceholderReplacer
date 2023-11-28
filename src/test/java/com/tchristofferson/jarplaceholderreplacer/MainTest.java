package com.tchristofferson.jarplaceholderreplacer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTest {

    private static final String JAR = "basicTesting.jar";

    @Test
    public void testDiscover() throws IOException {
        URL url = getClass().getClassLoader().getResource(JAR);
        List<PlaceholderLocationInfo> placeholderLocationInfoList;

        try (JarFile jarFile = new JarFile(url.getPath())) {
            placeholderLocationInfoList = PlaceholderDiscoverer.discover(jarFile, "%__USER__%", "%__NONCE__%");
        }

        Map<String, PlaceholderLocationInfo> expectedMap = Stream.of(
            new PlaceholderLocationInfo("com/tchristofferson/basictesting/Main.class", "%__USER__%"),
            new PlaceholderLocationInfo("com/tchristofferson/basictesting/ASMTestClass.class", "%__USER__%", "%__NONCE__%"),
            new PlaceholderLocationInfo("com/tchristofferson/basictesting/testpkg/ASMTestClass2.class", "%__USER__%", "%__NONCE__%")
        ).collect(Collectors.toMap(PlaceholderLocationInfo::getClassName, Function.identity()));

        assertEquals(placeholderLocationInfoList.size(), expectedMap.size(), "Expected result and actual lists are different sizes!");

        for (PlaceholderLocationInfo placeholderLocationInfo : placeholderLocationInfoList) {
            PlaceholderLocationInfo expectedInfo = expectedMap.get(placeholderLocationInfo.getClassName());

            if (expectedInfo == null)
                throw new IllegalStateException("Unexpected placeholder class found!");

            assertEquals(placeholderLocationInfo.getContainedPlaceholders(), expectedInfo.getContainedPlaceholders());
        }
    }
}
