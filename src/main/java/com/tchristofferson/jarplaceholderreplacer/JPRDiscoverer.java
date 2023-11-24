package com.tchristofferson.jarplaceholderreplacer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JPRDiscoverer {

    public static List<PlaceholderLocationInfo> discover(JarFile jarFile, String... placeholders) {
        return discover(jarFile, Arrays.asList(placeholders));
    }

    public static List<PlaceholderLocationInfo> discover(JarFile jarFile, Collection<String> placeholders) {
        //key=class
        Map<String, PlaceholderLocationInfo> discoveredPlaceholders = new HashMap<>();
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (!entry.getName().endsWith(".class"))
                continue;

            ClassReader classReader;

            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                classReader = new ClassReader(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to get input stream for jar file!", e);
            }

            //Use set for uniqueness
            Iterator<String> searchablePlaceholdersIterator = new HashSet<>(placeholders).iterator();
            classReader.accept(new ClassVisitor(Opcodes.ASM7) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM7, methodVisitor) {
                        @Override
                        public void visitLdcInsn(Object cst) {
                            if (cst instanceof String) {
                                while (searchablePlaceholdersIterator.hasNext()) {
                                    String placeholder = searchablePlaceholdersIterator.next();

                                    //If placeholder is found in String
                                    if (((String) cst).contains(placeholder)) {
                                        PlaceholderLocationInfo info = discoveredPlaceholders
                                            .computeIfAbsent(placeholder, clazz -> new PlaceholderLocationInfo(entry.getName()));

                                        info.addPlaceholder(placeholder);
                                        //Remove to increase search speed. Once found in class remove form iterator
                                        searchablePlaceholdersIterator.remove();
                                    }
                                }
                            }

                            super.visitLdcInsn(cst);
                        }
                    };
                }
            }, ClassReader.SKIP_FRAMES);
        }

        return new ArrayList<>(discoveredPlaceholders.values());
    }
}
