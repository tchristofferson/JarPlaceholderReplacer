package com.tchristofferson.jarplaceholderreplacer;

import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class PlaceholderReplacer {

    public static byte[] replace(JarFile jarFile, Placeholder... placeholders) throws IOException {
        if (placeholders.length == 0)
            throw new IllegalArgumentException("placeholders size cannot be 0!");

        return replace(jarFile, Arrays.asList(placeholders));
    }

    public static byte[] replace(JarFile jarFile, Collection<Placeholder> placeholders) throws IOException {
        Map<String, List<Placeholder>> placeholderMap = placeholders.stream()
            .collect(Collectors.groupingBy(Placeholder::getClassPath));

        Enumeration<JarEntry> entries = jarFile.entries();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String classPath = entry.getName();

                // Skip entries that are not class files
                if (!classPath.endsWith(".class") || !placeholderMap.containsKey(classPath)) {
                    copyEntry(jarFile, jarOutputStream, entry);//copy file to new jar as is
                    continue;
                }

                List<Placeholder> classPlaceholders = placeholderMap.get(classPath);

                try (InputStream entryInputStream = jarFile.getInputStream(entry)) {
                    byte[] modifiedClassBytes = modifyClass(entryInputStream, classPlaceholders);
                    saveModifiedClass(jarOutputStream, entry, modifiedClassBytes);
                }
            }
        }

        return outputStream.toByteArray();
    }

    private static byte[] modifyClass(InputStream classInputStream, Collection<Placeholder> placeholders) throws IOException {
        ClassReader classReader = new ClassReader(classInputStream);
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

                            for (Placeholder placeholder : placeholders) {
                                if (value.contains(placeholder.getToReplace())) {
                                    cst = value.replace(placeholder.getToReplace(), placeholder.getReplacement());
                                    break;//Only replaces one placeholder for each string, so break
                                }
                            }
                        }

                        super.visitLdcInsn(cst);
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);

        return classWriter.toByteArray();
    }

    private static void copyEntry(JarFile jarFile, JarOutputStream jarOutputStream, JarEntry entry) throws IOException {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            JarEntry newEntry = new JarEntry(entry.getName());
            jarOutputStream.putNextEntry(newEntry);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                jarOutputStream.write(buffer, 0, bytesRead);
            }
            jarOutputStream.closeEntry();
        }
    }

    private static void saveModifiedClass(JarOutputStream jarOutputStream, JarEntry entry, byte[] modifiedClassBytes) throws IOException {
        JarEntry newEntry = new JarEntry(entry.getName());
        jarOutputStream.putNextEntry(newEntry);
        jarOutputStream.write(modifiedClassBytes);
        jarOutputStream.closeEntry();
    }

}
