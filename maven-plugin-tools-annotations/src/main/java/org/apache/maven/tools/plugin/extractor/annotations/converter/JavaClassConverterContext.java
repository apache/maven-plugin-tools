/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.tools.plugin.extractor.annotations.converter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import org.apache.maven.tools.plugin.extractor.annotations.JavaSourceModel;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference.MemberType;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.apache.maven.tools.plugin.javadoc.JavadocReference;

/** {@link ConverterContext} backed by JavaParser source declarations and symbol resolution. */
public class JavaClassConverterContext implements ConverterContext {

    /** The Mojo class whose documentation is being generated. */
    private final TypeDeclaration<?> mojoClass;

    /** The class declaring the converted Javadoc, possibly a superclass of the Mojo class. */
    private final TypeDeclaration<?> declaringClass;

    private final Node locationNode;
    private final JavaSourceModel sourceModel;
    private final Map<String, MojoAnnotatedClass> mojoAnnotatedClasses;

    /** The link generator, or {@code null} when no Javadoc site is configured. */
    private final JavadocLinkGenerator linkGenerator;

    private final int lineNumber;
    private final Map<String, Object> attributes = new HashMap<>();

    public JavaClassConverterContext(
            TypeDeclaration<?> mojoClass,
            JavaSourceModel sourceModel,
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
            JavadocLinkGenerator linkGenerator,
            int lineNumber) {
        this(mojoClass, mojoClass, mojoClass, sourceModel, mojoAnnotatedClasses, linkGenerator, lineNumber);
    }

    public JavaClassConverterContext(
            TypeDeclaration<?> mojoClass,
            TypeDeclaration<?> declaringClass,
            Node locationNode,
            JavaSourceModel sourceModel,
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
            JavadocLinkGenerator linkGenerator,
            int lineNumber) {
        this.mojoClass = mojoClass;
        this.declaringClass = declaringClass;
        this.locationNode = locationNode;
        this.sourceModel = sourceModel;
        this.mojoAnnotatedClasses = mojoAnnotatedClasses;
        this.linkGenerator = linkGenerator;
        this.lineNumber = lineNumber;
    }

    @Override
    public Optional<String> getModuleName() {
        return sourceModel.getModuleName(getPackageName());
    }

    @Override
    public String getPackageName() {
        return resolve(mojoClass).getPackageName();
    }

    @Override
    public String getLocation() {
        return sourceModel.getLocation(locationNode, lineNumber);
    }

    /** Returns whether {@code reference} identifies the Mojo class or one of its ancestors. */
    @Override
    public boolean isReferencedBy(FullyQualifiedJavadocReference reference) {
        ResolvedReferenceTypeDeclaration declaration = resolve(mojoClass);
        if (isClassReferencedByReference(declaration, reference)) {
            return true;
        }
        return declaration.getAllAncestors().stream()
                .map(ResolvedReferenceType::getTypeDeclaration)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(type -> isClassReferencedByReference(type, reference));
    }

    private static boolean isClassReferencedByReference(
            ResolvedReferenceTypeDeclaration declaration, FullyQualifiedJavadocReference reference) {
        return declaration.getPackageName().equals(reference.getPackageName().orElse(""))
                && declaration.getClassName().equals(reference.getClassName().orElse(""));
    }

    @Override
    public boolean canGetUrl() {
        return linkGenerator != null;
    }

    @Override
    public URI getUrl(FullyQualifiedJavadocReference reference) {
        try {
            if (isReferencedBy(reference)
                    && MemberType.FIELD == reference.getMemberType().orElse(null)) {
                // Fields in the current Mojo link to parameter anchors on the same page.
                return new URI(null, null, reference.getMember().orElse(null));
            }
            Optional<String> fqClassName = reference.getFullyQualifiedClassName();
            if (fqClassName.isPresent()) {
                MojoAnnotatedClass mojoAnnotatedClass = mojoAnnotatedClasses.get(fqClassName.get());
                if (mojoAnnotatedClass != null
                        && mojoAnnotatedClass.getMojo() != null
                        && (!reference.getLabel().isPresent()
                                || MemberType.FIELD == reference.getMemberType().orElse(null))) {
                    // Fields and whole-class references to another Mojo link to that Mojo's page.
                    return new URI(
                            null,
                            "./" + mojoAnnotatedClass.getMojo().name() + "-mojo.html",
                            reference.getMember().orElse(null));
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error constructing a valid URL", e);
        }
        if (linkGenerator == null) {
            throw new IllegalStateException("No Javadoc Sites given to create URLs to");
        }
        return linkGenerator.createLink(reference);
    }

    @Override
    public FullyQualifiedJavadocReference resolveReference(JavadocReference reference) {
        Optional<FullyQualifiedJavadocReference> resolved;
        // First try the reference exactly as written, which handles fully qualified names.
        if (reference.getPackageNameClassName().isPresent()) {
            String name = reference.getPackageNameClassName().get();
            resolved = resolveNamedReference(name, reference.getMember(), reference.getLabel());
            if (resolved.isPresent()) {
                return resolved.get();
            }
        }

        // Search member-only references in the current class or interface first, then its ancestors.
        if (reference.getMember().isPresent()
                && !reference.getPackageNameClassName().isPresent()) {
            resolved = resolveMember(resolve(declaringClass), reference.getMember(), reference.getLabel(), true);
            if (resolved.isPresent()) {
                return resolved.get();
            }
        } else if (reference.getPackageNameClassName().isPresent()) {
            // Resolve simple and nested type names using Javadoc's lookup order.
            String name = reference.getPackageNameClassName().get();
            for (String candidate : classNameCandidates(name)) {
                resolved = resolveNamedReference(candidate, reference.getMember(), reference.getLabel());
                if (resolved.isPresent()) {
                    return resolved.get();
                }
            }
        }
        throw new IllegalArgumentException("Could not resolve javadoc reference " + reference);
    }

    private Optional<FullyQualifiedJavadocReference> resolveNamedReference(
            String name, Optional<String> member, Optional<String> label) {
        Optional<ResolvedReferenceTypeDeclaration> type = sourceModel.resolveType(name);
        if (type.isPresent()) {
            return resolveMember(type.get(), member, label, true);
        }
        // Package references cannot contain members.
        if (!member.isPresent() && sourceModel.hasPackage(name)) {
            return Optional.of(new FullyQualifiedJavadocReference(name, label, !sourceModel.isInternalPackage(name)));
        }
        return Optional.empty();
    }

    private List<String> classNameCandidates(String name) {
        List<String> candidates = new ArrayList<>();
        ResolvedReferenceTypeDeclaration declaration = resolve(declaringClass);
        // Search order: current package, implicit java.lang import, then explicit imports in declaration order.
        candidates.add(declaration.getPackageName() + "." + name);
        candidates.add("java.lang." + name);
        List<ImportDeclaration> imports = declaringClass
                .findCompilationUnit()
                .<List<ImportDeclaration>>map(unit -> new ArrayList<>(unit.getImports()))
                .orElseGet(Collections::emptyList);
        for (ImportDeclaration importDeclaration : imports) {
            if (importDeclaration.isStatic()) {
                continue;
            }
            String importName = importDeclaration.getNameAsString();
            if (importDeclaration.isAsterisk()) {
                candidates.add(importName + "." + name);
            } else if (name.equals(simpleName(importName))) {
                candidates.add(importName);
            } else if (name.startsWith(simpleName(importName) + ".")) {
                // An imported outer type may prefix a nested-type reference.
                candidates.add(
                        importName + name.substring(simpleName(importName).length()));
            }
        }
        return candidates;
    }

    private Optional<FullyQualifiedJavadocReference> resolveMember(
            ResolvedReferenceTypeDeclaration type,
            Optional<String> member,
            Optional<String> label,
            boolean includeAncestors) {
        if (!member.isPresent()) {
            return Optional.of(toReference(type, Optional.empty(), Optional.empty(), label));
        }

        String memberText = member.get();
        // Resolve ambiguous member text as a field, method, then constructor.
        Optional<ResolvedFieldDeclaration> field = findField(type, memberText, includeAncestors);
        if (field.isPresent()) {
            return Optional.of(toReference(
                    field.get().declaringType().asReferenceType(), member, Optional.of(MemberType.FIELD), label));
        }

        String methodName = methodName(memberText);
        Optional<List<String>> parameterTypes = parameterTypes(memberText);
        Optional<ResolvedMethodDeclaration> method = findMethod(type, methodName, parameterTypes, includeAncestors);
        if (method.isPresent()) {
            return Optional.of(toReference(
                    method.get().declaringType(),
                    Optional.of(canonicalMember(method.get())),
                    Optional.of(MemberType.METHOD),
                    label));
        }

        if (methodName.equals(type.getName())) {
            Optional<ResolvedConstructorDeclaration> constructor = findConstructor(type, parameterTypes);
            if (constructor.isPresent()) {
                return Optional.of(toReference(
                        type,
                        Optional.of(canonicalMember(constructor.get())),
                        Optional.of(MemberType.CONSTRUCTOR),
                        label));
            }
        }
        return Optional.empty();
    }

    private Optional<ResolvedFieldDeclaration> findField(
            ResolvedReferenceTypeDeclaration type, String name, boolean includeAncestors) {
        List<ResolvedFieldDeclaration> fields = includeAncestors ? type.getAllFields() : type.getDeclaredFields();
        return fields.stream().filter(field -> field.getName().equals(name)).findFirst();
    }

    private Optional<ResolvedMethodDeclaration> findMethod(
            ResolvedReferenceTypeDeclaration type,
            String name,
            Optional<List<String>> parameterTypes,
            boolean includeAncestors) {
        List<ResolvedReferenceTypeDeclaration> hierarchy = new ArrayList<>();
        hierarchy.add(type);
        if (includeAncestors) {
            type.getAllAncestors().stream()
                    .map(ResolvedReferenceType::getTypeDeclaration)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(hierarchy::add);
        }
        for (ResolvedReferenceTypeDeclaration declaration : hierarchy) {
            Optional<ResolvedMethodDeclaration> method = declaration.getDeclaredMethods().stream()
                    .filter(candidate -> candidate.getName().equals(name))
                    .filter(candidate -> matches(candidate, parameterTypes))
                    .findFirst();
            if (method.isPresent()) {
                return method;
            }
        }
        return Optional.empty();
    }

    private Optional<ResolvedConstructorDeclaration> findConstructor(
            ResolvedReferenceTypeDeclaration type, Optional<List<String>> parameterTypes) {
        return type.getConstructors().stream()
                .filter(candidate -> matches(candidate, parameterTypes))
                .findFirst();
    }

    private boolean matches(ResolvedMethodLikeDeclaration declaration, Optional<List<String>> parameterTypes) {
        if (!parameterTypes.isPresent()) {
            return true;
        }
        if (declaration.getNumberOfParams() != parameterTypes.get().size()) {
            return false;
        }
        for (int index = 0; index < declaration.getNumberOfParams(); index++) {
            String actual = declaration.getParam(index).getType().erasure().describe();
            if (!actual.equals(parameterTypes.get().get(index))) {
                return false;
            }
        }
        return true;
    }

    private Optional<List<String>> parameterTypes(String member) {
        int opening = member.indexOf('(');
        int closing = member.lastIndexOf(')');
        if (opening < 0 && closing < 0) {
            // Without parentheses, match the first overload found, mirroring javadoc.
            return Optional.empty();
        }
        if (opening < 0 || closing < opening) {
            throw new IllegalArgumentException("Found opening without closing parentheses or vice versa in " + member);
        }
        String arguments = member.substring(opening + 1, closing).trim();
        if (arguments.isEmpty()) {
            return Optional.of(Collections.emptyList());
        }
        List<String> result = new ArrayList<>();
        for (String argument : splitArguments(arguments)) {
            result.add(resolveParameterType(stripArgumentName(argument.trim())));
        }
        return Optional.of(result);
    }

    private static List<String> splitArguments(String arguments) {
        List<String> result = new ArrayList<>();
        int genericDepth = 0;
        int start = 0;
        for (int index = 0; index < arguments.length(); index++) {
            char ch = arguments.charAt(index);
            if (ch == '<') {
                genericDepth++;
            } else if (ch == '>') {
                genericDepth--;
            } else if (ch == ',' && genericDepth == 0) {
                result.add(arguments.substring(start, index));
                start = index + 1;
            }
        }
        result.add(arguments.substring(start));
        return result;
    }

    private static String stripArgumentName(String argument) {
        int genericDepth = 0;
        for (int index = argument.length() - 1; index >= 0; index--) {
            char ch = argument.charAt(index);
            if (ch == '>') {
                genericDepth++;
            } else if (ch == '<') {
                genericDepth--;
            } else if (Character.isWhitespace(ch) && genericDepth == 0) {
                return argument.substring(0, index).trim();
            }
        }
        return argument;
    }

    private String resolveParameterType(String typeName) {
        String normalized = eraseGenerics(typeName.replace("...", "[]"));
        int dimensions = 0;
        while (normalized.endsWith("[]")) {
            dimensions++;
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        String resolved;
        if (isPrimitive(normalized)) {
            resolved = normalized;
        } else {
            List<String> candidates = new ArrayList<>();
            candidates.add(normalized);
            candidates.addAll(classNameCandidates(normalized));
            resolved = candidates.stream()
                    .filter(candidate -> sourceModel.resolveType(candidate).isPresent())
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalArgumentException("Found unresolvable method argument type " + typeName));
        }
        StringBuilder result = new StringBuilder(resolved);
        for (int index = 0; index < dimensions; index++) {
            result.append("[]");
        }
        return result.toString();
    }

    private static String eraseGenerics(String value) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '<') {
                depth++;
            } else if (ch == '>') {
                depth--;
            } else if (depth == 0) {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static boolean isPrimitive(String name) {
        return "boolean".equals(name)
                || "byte".equals(name)
                || "char".equals(name)
                || "double".equals(name)
                || "float".equals(name)
                || "int".equals(name)
                || "long".equals(name)
                || "short".equals(name);
    }

    private FullyQualifiedJavadocReference toReference(
            ResolvedReferenceTypeDeclaration type,
            Optional<String> member,
            Optional<MemberType> memberType,
            Optional<String> label) {
        return new FullyQualifiedJavadocReference(
                type.getPackageName(),
                Optional.of(type.getClassName()),
                member,
                memberType,
                label,
                !sourceModel.isInternal(type));
    }

    @Override
    public String getStaticFieldValue(FullyQualifiedJavadocReference reference) {
        String fqcn = reference
                .getFullyQualifiedClassName()
                .orElseThrow(() ->
                        new IllegalArgumentException("Given reference does not specify a fully qualified class name!"));
        String fieldName = reference
                .getMember()
                .orElseThrow(() -> new IllegalArgumentException("Given reference does not specify a member!"));
        TypeDeclaration<?> type = sourceModel
                .getType(fqcn)
                .orElseThrow(() -> new IllegalArgumentException("Could not find source class " + fqcn));
        for (FieldDeclaration field : type.getFields()) {
            Optional<com.github.javaparser.ast.body.VariableDeclarator> variable = field.getVariables().stream()
                    .filter(candidate -> candidate.getNameAsString().equals(fieldName))
                    .findFirst();
            if (variable.isPresent()) {
                if (!field.isStatic()) {
                    throw new IllegalArgumentException(
                            "Field with name " + fieldName + " in class " + fqcn + " is not static");
                }
                return variable.get()
                        .getInitializer()
                        .map(initializer -> initializer.toString())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Field with name " + fieldName + " in class " + fqcn + " has no initializer"));
            }
        }
        throw new IllegalArgumentException("Could not find field with name " + fieldName + " in class " + fqcn);
    }

    @Override
    public URI getInternalJavadocSiteBaseUrl() {
        return linkGenerator.getInternalJavadocSiteBaseUrl();
    }

    private static ResolvedReferenceTypeDeclaration resolve(TypeDeclaration<?> declaration) {
        return declaration.resolve();
    }

    private static String methodName(String member) {
        int opening = member.indexOf('(');
        return opening < 0 ? member : member.substring(0, opening);
    }

    private static String canonicalMember(ResolvedMethodLikeDeclaration declaration) {
        return declaration.getName() + "("
                + declaration.formalParameterTypes().stream()
                        .map(type -> type.erasure().describe())
                        .collect(Collectors.joining(","))
                + ")";
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T setAttribute(String name, T value) {
        return (T) attributes.put(name, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String name, Class<T> clazz, T defaultValue) {
        return (T) attributes.getOrDefault(name, defaultValue);
    }
}
