package com.example.backend.utils;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UmlGenerator {

    public static void main(String[] args) {
        // On utilise System.getProperty("user.dir") pour être sûr de partir de la racine du module backend
        String projectRoot = System.getProperty("user.dir");
        
        // Chemin vers les sources (en s'assurant de gérer le dossier 'backend' si nécessaire)
        File srcDir = new File(projectRoot, "src/main/java");
        if (!srcDir.exists()) {
            // Si lancé depuis la racine du projet global au lieu du dossier backend
            srcDir = new File(projectRoot, "backend/src/main/java");
        }

        String packageName = "com.example.backend";
        // Destination demandée : DevOps1_EDT_Intelligent/doc/uml/diagram_classes.plantuml
        // On remonte de 'backend' vers la racine pour atteindre 'doc'
        File outputFile = new File(srcDir.getParentFile().getParentFile().getParentFile(), "../doc/uml/diagram_classes.puml");
        
        try {
            List<Class<?>> classes = findAllClasses(srcDir.getAbsolutePath(), packageName);
            
            if (classes.isEmpty()) {
                System.out.println("Erreur : Aucune classe trouvée dans " + srcDir.getAbsolutePath());
                return;
            }

            generatePlantUml(classes, outputFile.getAbsolutePath());
            System.out.println("Succès : " + classes.size() + " classes analysées.");
            System.out.println("Fichier généré : " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Class<?>> findAllClasses(String sourcePath, String packageName) throws Exception {
        Path root = Paths.get(sourcePath, packageName.replace('.', '/'));
        if (!Files.exists(root)) return Collections.emptyList();

        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                .filter(p -> p.toString().endsWith(".java"))
                .map(p -> {
                    // Convertit le chemin du fichier en nom de classe (ex: com.example.backend.model.User)
                    String path = p.toString();
                    String classPart = path.substring(path.indexOf("com" + File.separator + "example"));
                    String className = classPart.replace(File.separator, ".").replace(".java", "");
                    try {
                        return Class.forName(className);
                    } catch (Throwable e) {
                        // On ignore les classes qui ne peuvent pas être chargées (ex: interfaces externes)
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }

    public static void generatePlantUml(List<Class<?>> classes, String outputPath) {
        File file = new File(outputPath);
        file.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("@startuml\n");
            writer.println("!theme plain");
            writer.println("top to bottom direction");
            writer.println("skinparam linetype ortho\n");

            // Définition des classes
            for (Class<?> clazz : classes) {
                String type = clazz.isInterface() ? "interface" : (clazz.isEnum() ? "enum" : "class");
                String stereotype = clazz.isInterface() ? " << interface >>" : (clazz.isEnum() ? " << enumeration >>" : "");
                
                writer.println(type + " " + clazz.getSimpleName() + stereotype + " {");
                writer.println("  + " + clazz.getSimpleName() + "(): ");

                if (clazz.isEnum()) {
                    for (Object c : clazz.getEnumConstants()) writer.println("  + " + c.toString());
                } else {
                    // Champs
                    for (Field f : clazz.getDeclaredFields()) {
                        if (!f.isSynthetic() && !Modifier.isStatic(f.getModifiers())) {
                            writer.println("  - " + f.getName() + ": " + f.getType().getSimpleName());
                        }
                    }
                    // Propriétés (Simulation Getters)
                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.getName().startsWith("get") && m.getParameterCount() == 0 && !m.getName().equals("getClass")) {
                            String prop = m.getName().substring(3);
                            prop = prop.substring(0, 1).toLowerCase() + prop.substring(1);
                            writer.println("   " + prop + ": " + m.getReturnType().getSimpleName());
                        }
                    }
                }
                writer.println("}\n");
            }

            // Relations
            for (Class<?> clazz : classes) {
                if (clazz.getSuperclass() != null && classes.contains(clazz.getSuperclass())) 
                    writer.println(clazz.getSuperclass().getSimpleName() + " <|-- " + clazz.getSimpleName());
                for (Class<?> iface : clazz.getInterfaces())
                    if (classes.contains(iface)) writer.println(iface.getSimpleName() + " <|.. " + clazz.getSimpleName());

                for (Field f : clazz.getDeclaredFields()) {
                    Class<?> target = f.getType();
                    String card = "1";
                    if (Collection.class.isAssignableFrom(target)) {
                        card = "*";
                        Type gType = f.getGenericType();
                        if (gType instanceof ParameterizedType) {
                            Type actualType = ((ParameterizedType) gType).getActualTypeArguments()[0];
                            if (actualType instanceof Class) target = (Class<?>) actualType;
                        }
                    }
                    if (classes.contains(target)) {
                        writer.println(String.format("%s \"1\" *-[#595959,plain]-> \"%s\\n%s\" %s", 
                            clazz.getSimpleName(), f.getName(), card, target.getSimpleName()));
                    }
                }
            }
            writer.println("\n@enduml");
            // Ajoute cette ligne pour autoriser des images de grande taille (ex: 8192 pixels)
            writer.println("!pragma useVerticalIf on"); // Optionnel : optimise l'espace
            writer.println("skinparam dpi 300");        // Optionnel : pour une meilleure qualité
            writer.println("allow_mixing");             // Optionnel
            writer.println("set maxMessageSize 8192");  // Pour les diagrammes de séquence
            writer.println("skinparam nodesep 50");     // Espace entre les boîtes
            writer.println("skinparam ranksep 50");    // Espace entre les niveaux
        } catch (Exception e) { e.printStackTrace(); }
    }
}