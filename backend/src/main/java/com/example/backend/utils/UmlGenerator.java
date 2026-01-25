package com.example.backend.utils;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.Entity;

public class UmlGenerator {
    public static void main(String[] args) throws Exception {
        String outputPath = args.length > 0 ? args[0] : "doc/uml/diagram_classes.puml";
        PrintWriter writer = new PrintWriter(new File(outputPath));
        
        // Entête pour le style "professionnel"
        writer.println("@startuml");
        writer.println("!theme plain");
        writer.println("top to bottom direction");
        writer.println("skinparam linetype ortho");
        writer.println("skinparam classAttributeIconSize 0");

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<Class<?>> classes = new HashSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("com.example.backend")) {
            if (bd.getBeanClassName().contains("UmlGenerator")) continue;
            classes.add(Class.forName(bd.getBeanClassName()));
        }

        for (Class<?> clazz : classes) {
            String stereo = "";
            if (clazz.isInterface()) stereo = " << interface >>";
            else if (clazz.isEnum()) stereo = " << enumeration >>";

            writer.println((clazz.isInterface() ? "interface " : "class ") + clazz.getSimpleName() + stereo + " {");
            
            // Affichage des méthodes importantes
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    writer.println("  + " + method.getName() + "()");
                }
            }
            writer.println("}");

            // RELATIONS
            // Héritage
            if (clazz.getSuperclass() != null && classes.contains(clazz.getSuperclass())) {
                writer.println(clazz.getSuperclass().getSimpleName() + " <|-- " + clazz.getSimpleName());
            }
            // Interfaces
            for (Class<?> iface : clazz.getInterfaces()) {
                if (classes.contains(iface)) writer.println(iface.getSimpleName() + " <|.. " + clazz.getSimpleName());
            }
            // Dépendances (Champs)
            for (Field field : clazz.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (classes.contains(fieldType)) {
                    writer.println(clazz.getSimpleName() + " \"1\" *-- \"" + field.getName() + "\" " + fieldType.getSimpleName());
                } else if (Collection.class.isAssignableFrom(fieldType)) {
                    ParameterizedType pt = (ParameterizedType) field.getGenericType();
                    Class<?> actual = (Class<?>) pt.getActualTypeArguments()[0];
                    if (classes.contains(actual)) {
                        writer.println(clazz.getSimpleName() + " \"1\" *-- \"*\" " + actual.getSimpleName());
                    }
                }
            }
        }
        writer.println("@enduml");
        writer.close();
    }
}