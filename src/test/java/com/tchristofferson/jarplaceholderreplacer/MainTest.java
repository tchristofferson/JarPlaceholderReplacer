package com.tchristofferson.jarplaceholderreplacer;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
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

    @Test
    public void testReplace() throws IOException {
        List<Placeholder> placeholders = Arrays.asList(
            new Placeholder("com/tchristofferson/basictesting/Main.class", "%__USER__%", "tchristofferson"),
            new Placeholder("com/tchristofferson/basictesting/ASMTestClass.class", "%__USER__%", "tchristofferson"),
            new Placeholder("com/tchristofferson/basictesting/ASMTestClass.class", "%__NONCE__%", String.valueOf(System.currentTimeMillis())),
            new Placeholder("com/tchristofferson/basictesting/testpkg/ASMTestClass2.class", "%__USER__%", "tchristofferson"),
            new Placeholder("com/tchristofferson/basictesting/testpkg/ASMTestClass2.class", "%__NONCE__%", String.valueOf(System.currentTimeMillis()))
        );

        byte[] bytes = PlaceholderReplacer.replace(new JarFile(getClass().getClassLoader().getResource(JAR).getPath()), placeholders);
        JarInputStream jarInputStream = new JarInputStream(new ByteArrayInputStream(bytes));

        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (!jarEntry.getName().endsWith(".class"))
                continue;

            ByteArrayOutputStream entryBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = jarInputStream.read(buffer)) != -1) {
                entryBytes.write(buffer, 0, bytesRead);
            }

            ClassReader classReader = new ClassReader(entryBytes.toByteArray());
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
            classReader.accept(new ClassVisitor(Opcodes.ASM9, classWriter) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, methodVisitor) {
                        @Override
                        public void visitLdcInsn(Object cst) {
                            if (cst instanceof String) {
                                String value = (String) cst;

                                if (value.contains("%__USER__%") || value.contains("%__NONCE__%"))
                                    throw new RuntimeException("Placeholder wasn't replaced! Perhaps a new class file was modified or added?");
                            }

                            super.visitLdcInsn(cst);
                        }
                    };
                }
            }, ClassReader.SKIP_FRAMES);
        }
    }
}
