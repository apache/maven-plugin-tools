package org.apache.maven.tools.plugin.extractor.annotations.converter;

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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.builder.TypeAssembler;
import com.thoughtworks.qdox.library.ClassNameLibrary;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaModule;
import com.thoughtworks.qdox.model.JavaPackage;
import com.thoughtworks.qdox.model.JavaType;
import com.thoughtworks.qdox.parser.structs.TypeDef;
import com.thoughtworks.qdox.type.TypeResolver;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.apache.maven.tools.plugin.javadoc.JavadocReference;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference.MemberType;

/** {@link ConverterContext} based on QDox's {@link JavaClass} and {@link JavaProjectBuilder}. */
public class JavaClassConverterContext
    implements ConverterContext
{

    final JavaClass mojoClass; // this is the mojo's class

    final JavaClass declaringClass; // this may be a super class of the mojo's class

    final JavaProjectBuilder javaProjectBuilder;

    final Map<String, MojoAnnotatedClass> mojoAnnotatedClasses;

    final JavadocLinkGenerator linkGenerator; // may be null in case nothing was configured

    final int lineNumber;

    final Optional<JavaModule> javaModule;
    
    final Map<String, Object> attributes;

    public JavaClassConverterContext( JavaClass mojoClass, JavaProjectBuilder javaProjectBuilder,
                                      Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
                                      JavadocLinkGenerator linkGenerator, int lineNumber )
    {
        this( mojoClass, mojoClass, javaProjectBuilder, mojoAnnotatedClasses, linkGenerator, lineNumber );
    }

    public JavaClassConverterContext( JavaClass mojoClass, JavaClass declaringClass,
                                      JavaProjectBuilder javaProjectBuilder,
                                      Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
                                      JavadocLinkGenerator linkGenerator, int lineNumber )
    {
        this.mojoClass = mojoClass;
        this.declaringClass = declaringClass;
        this.javaProjectBuilder = javaProjectBuilder;
        this.mojoAnnotatedClasses = mojoAnnotatedClasses;
        this.linkGenerator = linkGenerator;
        this.lineNumber = lineNumber;
        this.attributes = new HashMap<>();

        javaModule =
            mojoClass.getJavaClassLibrary().getJavaModules().stream().filter( 
                m -> m.getDescriptor().getExports().stream().anyMatch( 
                    e -> e.getSource().getName().equals( getPackageName() ) 
                ) )
            .findFirst();
    }

    @Override
    public Optional<String> getModuleName()
    {
        // https://github.com/paul-hammant/qdox/issues/113, module name is not exposed
        return javaModule.map( JavaModule::getName );
    }

    @Override
    public String getPackageName()
    {
        return mojoClass.getPackageName();
    }

    @Override
    public String getLocation()
    {
        try
        {
            URL url = declaringClass.getSource().getURL();
            if ( url == null ) // url is not always available, just emit FQCN in that case
            {
                return declaringClass.getPackageName() + declaringClass.getSimpleName() + ":" + lineNumber;
            }
            return Paths.get( "" ).toUri().relativize( url.toURI() ) + ":" + lineNumber;
        }
        catch ( URISyntaxException e )
        {
            return declaringClass.getSource().getURL() + ":" + lineNumber;
        }
    }

    /**
     * @param reference
     * @return true in case either the current context class or any of its super classes are referenced
     */
    @Override
    public boolean isReferencedBy( FullyQualifiedJavadocReference reference )
    {
        JavaClass javaClassInHierarchy = this.mojoClass;
        while ( javaClassInHierarchy != null )
        {
            if ( isClassReferencedByReference( javaClassInHierarchy, reference ) )
            {
                return true;
            }
            // check implemented interfaces
            for ( JavaClass implementedInterfaces : javaClassInHierarchy.getInterfaces() )
            {
                if ( isClassReferencedByReference( implementedInterfaces, reference ) )
                {
                    return true;
                }
            }
            javaClassInHierarchy = javaClassInHierarchy.getSuperJavaClass();
        }
        return false;
    }

    private static boolean isClassReferencedByReference( JavaClass javaClass, FullyQualifiedJavadocReference reference )
    {
        return javaClass.getPackageName().equals( reference.getPackageName().orElse( "" ) )
            && javaClass.getSimpleName().equals( reference.getClassName().orElse( "" ) );
    }

    
    @Override
    public boolean canGetUrl()
    {
        return linkGenerator != null;
    }

    @Override
    public URI getUrl( FullyQualifiedJavadocReference reference )
    {
        try
        {
            if ( isReferencedBy( reference ) && MemberType.FIELD == reference.getMemberType().orElse( null ) )
            {
                // link to current goal's parameters
                return new URI( null, null, reference.getMember().orElse( null ) ); // just an anchor if same context
            }
            Optional<String> fqClassName = reference.getFullyQualifiedClassName();
            if ( fqClassName.isPresent() )
            {
                MojoAnnotatedClass mojoAnnotatedClass = mojoAnnotatedClasses.get( fqClassName.get() );
                if ( mojoAnnotatedClass != null && mojoAnnotatedClass.getMojo() != null
                    && ( !reference.getLabel().isPresent()
                        || MemberType.FIELD == reference.getMemberType().orElse( null ) ) )
                {
                    // link to other mojo (only for fields = parameters or without member)
                    return new URI( null, "./" + mojoAnnotatedClass.getMojo().name() + "-mojo.html",
                                    reference.getMember().orElse( null ) );
                }
            }
        }
        catch ( URISyntaxException e )
        {
            throw new IllegalStateException( "Error constructing a valid URL", e ); // should not happen
        }
        if ( linkGenerator == null )
        {
            throw new IllegalStateException( "No Javadoc Sites given to create URLs to" );
        }
        return linkGenerator.createLink( reference );
    }

    @Override
    public FullyQualifiedJavadocReference resolveReference( JavadocReference reference )
    {
        Optional<FullyQualifiedJavadocReference> resolvedName;
        // is it already fully qualified?
        if ( reference.getPackageNameClassName().isPresent() )
        {
            resolvedName =
                resolveMember( reference.getPackageNameClassName().get(), reference.getMember(), reference.getLabel() );
            if ( resolvedName.isPresent() )
            {
                return resolvedName.get();
            }
        }
        // is it a member only?
        if ( reference.getMember().isPresent() && !reference.getPackageNameClassName().isPresent() )
        {
            // search order for not fully qualified names:
            // 1. The current class or interface (only for members)
            resolvedName = resolveMember( declaringClass, reference.getMember(), reference.getLabel() );
            if ( resolvedName.isPresent() )
            {
                return resolvedName.get();
            }
            // 2. Any enclosing classes and interfaces searching the closest first (only members)
            for ( JavaClass nestedClass : declaringClass.getNestedClasses() )
            {
                resolvedName = resolveMember( nestedClass, reference.getMember(), reference.getLabel() );
                if ( resolvedName.isPresent() )
                {
                    return resolvedName.get();
                }
            }
            // 3. Any superclasses and superinterfaces, searching the closest first. (only members)
            JavaClass superClass = declaringClass.getSuperJavaClass();
            while ( superClass != null )
            {
                resolvedName = resolveMember( superClass, reference.getMember(), reference.getLabel() );
                if ( resolvedName.isPresent() )
                {
                    return resolvedName.get();
                }
                superClass = superClass.getSuperJavaClass();
            }
        }
        else
        {
            String packageNameClassName = reference.getPackageNameClassName().get();
            // 4. The current package
            resolvedName = resolveMember( declaringClass.getPackageName() + "." + packageNameClassName,
                                          reference.getMember(), reference.getLabel() );
            if ( resolvedName.isPresent() )
            {
                return resolvedName.get();
            }
            // 5. Any imported packages, classes, and interfaces, searching in the order of the import statement.
            List<String> importNames = new ArrayList<>();
            importNames.add( "java.lang.*" ); // default import
            importNames.addAll( declaringClass.getSource().getImports() );
            for ( String importName : importNames )
            {
                if ( importName.endsWith( ".*" ) )
                {
                    resolvedName = resolveMember( importName.replace( "*", packageNameClassName ),
                                                  reference.getMember(), reference.getLabel() );
                    if ( resolvedName.isPresent() )
                    {
                        return resolvedName.get();
                    }
                }
                else
                {
                    if ( importName.endsWith( packageNameClassName ) )
                    {
                        resolvedName = resolveMember( importName, reference.getMember(), reference.getLabel() );
                        if ( resolvedName.isPresent() )
                        {
                            return resolvedName.get();
                        }
                    }
                    else
                    {
                        // ends with prefix of reference (nested class name)
                        int firstDotIndex = packageNameClassName.indexOf( "." );
                        if ( firstDotIndex > 0
                            && importName.endsWith( packageNameClassName.substring( 0, firstDotIndex ) ) )
                        {
                            resolvedName =
                                resolveMember( importName, packageNameClassName.substring( firstDotIndex + 1 ),
                                               reference.getMember(), reference.getLabel() );
                            if ( resolvedName.isPresent() )
                            {
                                return resolvedName.get();
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException( "Could not resolve javadoc reference " + reference );
    }

    @Override
    public String getStaticFieldValue( FullyQualifiedJavadocReference reference )
    {
        String fqcn = reference.getFullyQualifiedClassName().orElseThrow(
            () -> new IllegalArgumentException( "Given reference does not specify a fully qualified class name!" ) );
        String fieldName = reference.getMember().orElseThrow(
            () -> new IllegalArgumentException( "Given reference does not specify a member!" ) );
        JavaClass javaClass = javaProjectBuilder.getClassByName( fqcn );
        JavaField javaField = javaClass.getFieldByName( fieldName );
        if ( javaField == null )
        {
            throw new IllegalArgumentException( "Could not find field with name " + fieldName + " in class " + fqcn );
        }
        if ( !javaField.isStatic() )
        {
            throw new IllegalArgumentException( "Field with name " + fieldName + " in class " + fqcn
                                                + " is not static" );
        }
        return javaField.getInitializationExpression();
    }

    @Override
    public URI getInternalJavadocSiteBaseUrl()
    {
        return linkGenerator.getInternalJavadocSiteBaseUrl();
    }

    private Optional<FullyQualifiedJavadocReference> resolveMember( String fullyQualifiedPackageNameClassName,
                                                                    Optional<String> member, Optional<String> label )
    {
        return resolveMember( fullyQualifiedPackageNameClassName, "", member, label );
    }

    private Optional<FullyQualifiedJavadocReference> resolveMember( String fullyQualifiedPackageNameClassName,
                                                                    String nestedClassName, Optional<String> member,
                                                                    Optional<String> label )
    {
        JavaClass javaClass = javaProjectBuilder.getClassByName( fullyQualifiedPackageNameClassName );
        if ( !isClassFound( javaClass ) )
        {
            JavaPackage javaPackage = javaProjectBuilder.getPackageByName( fullyQualifiedPackageNameClassName );
            if ( javaPackage == null || !nestedClassName.isEmpty() )
            {
                // is it a nested class?
                int lastIndexOfDot = fullyQualifiedPackageNameClassName.lastIndexOf( '.' );
                if ( lastIndexOfDot > 0 )
                {
                    String newNestedClassName = nestedClassName;
                    if ( !newNestedClassName.isEmpty() )
                    {
                        newNestedClassName += '.';
                    }
                    newNestedClassName += fullyQualifiedPackageNameClassName.substring( lastIndexOfDot + 1 );
                    return resolveMember( fullyQualifiedPackageNameClassName.substring( 0, lastIndexOfDot ),
                                          newNestedClassName, member, label );
                }
                return Optional.empty();
            }
            else
            {
                // reference to java package never has a member
                return Optional.of( new FullyQualifiedJavadocReference( javaPackage.getName(), label,
                                                                        isExternal( javaPackage ) ) );
            }
        }
        else
        {
            if ( !nestedClassName.isEmpty() )
            {
                javaClass = javaClass.getNestedClassByName( nestedClassName );
                if ( javaClass == null )
                {
                    return Optional.empty();
                }
            }

            return resolveMember( javaClass, member, label );
        }
    }

    private boolean isExternal( JavaClass javaClass )
    {
        return isExternal( javaClass.getPackage() );
    }
    
    private boolean isExternal( JavaPackage javaPackage )
    {
        return !javaPackage.getJavaClassLibrary().equals( mojoClass.getJavaClassLibrary() );
    }

    private Optional<FullyQualifiedJavadocReference> resolveMember( JavaClass javaClass, Optional<String> member,
                                                                    Optional<String> label )
    {
        final Optional<MemberType> memberType;
        Optional<String> resolvedMember = member;
        if ( member.isPresent() )
        {
            // member is either field...
            if ( javaClass.getFieldByName( member.get() ) == null )
            {
                // ...is method...
                List<JavaType> parameterTypes = getParameterTypes( member.get() );
                String methodName = getMethodName( member.get() );
                if ( javaClass.getMethodBySignature( methodName, parameterTypes ) == null )
                {
                    // ...or is constructor
                    if ( ( !methodName.equals( javaClass.getSimpleName() ) )
                        || ( javaClass.getConstructor( parameterTypes ) == null ) )
                    {
                        return Optional.empty();
                    }
                    else
                    {
                        memberType = Optional.of( MemberType.CONSTRUCTOR );
                    }
                }
                else
                {
                    memberType = Optional.of( MemberType.METHOD );
                }
                // reconstruct member with fully qualified names but leaving out the argument names
                StringBuilder memberBuilder = new StringBuilder( methodName );
                memberBuilder.append( "(" );
                memberBuilder.append( parameterTypes.stream().map( JavaType::getFullyQualifiedName )
                                      .collect( Collectors.joining( "," ) ) );
                memberBuilder.append( ")" );
                resolvedMember = Optional.of( memberBuilder.toString() );
            }
            else
            {
                memberType = Optional.of( MemberType.FIELD );
            }
        }
        else
        {
            memberType = Optional.empty();
        }
        String className = javaClass.getCanonicalName().substring( javaClass.getPackageName().length() + 1 );
        return Optional.of( new FullyQualifiedJavadocReference( javaClass.getPackageName(), Optional.of( className ),
                                                                resolvedMember, memberType, label,
                                                                isExternal( javaClass ) ) );
    }

    private static boolean isClassFound( JavaClass javaClass )
    {
        // this is never null due to using the ClassNameLibrary in the builder
        // but every instance of ClassNameLibrary basically means that the class was not found
        return !( javaClass.getJavaClassLibrary() instanceof ClassNameLibrary );
    }

    // https://github.com/paul-hammant/qdox/issues/104
    private List<JavaType> getParameterTypes( String member )
    {
        List<JavaType> parameterTypes = new ArrayList<>();
        // TypeResolver.byClassName() always resolves types as non existing inner class
        TypeResolver typeResolver =
            TypeResolver.byClassName( declaringClass.getPackageName(), declaringClass.getJavaClassLibrary(),
                                      declaringClass.getSource().getImports() );

        // method parameters are optionally enclosed by parentheses
        int indexOfOpeningParenthesis = member.indexOf( '(' );
        int indexOfClosingParenthesis = member.indexOf( ')' );
        final String signatureArguments;
        if ( indexOfOpeningParenthesis >= 0 && indexOfClosingParenthesis > 0
            && indexOfClosingParenthesis > indexOfOpeningParenthesis )
        {
            signatureArguments = member.substring( indexOfOpeningParenthesis + 1, indexOfClosingParenthesis );
        }
        else if ( indexOfOpeningParenthesis == -1 && indexOfClosingParenthesis >= 0
            || indexOfOpeningParenthesis >= 0 && indexOfOpeningParenthesis == -1 )
        {
            throw new IllegalArgumentException( "Found opening without closing parentheses or vice versa in "
                + member );
        }
        else
        {
            // If any method or constructor is entered as a name with no parentheses, such as getValue,
            // and if there is no field with the same name, then the javadoc command still creates a
            // link to the method. If this method is overloaded, then the javadoc command links to the
            // first method its search encounters, which is unspecified
            // (Source: https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#JSWOR654)
            return Collections.emptyList();
        }
        for ( String parameter : signatureArguments.split( "," ) )
        {
            // strip off argument name, only type is relevant
            String canonicalParameter = parameter.trim();
            int spaceIndex = canonicalParameter.indexOf( ' ' );
            final String typeName;
            if ( spaceIndex > 0 )
            {
                typeName = canonicalParameter.substring( 0, spaceIndex ).trim();
            }
            else
            {
                typeName = canonicalParameter;
            }
            if ( !typeName.isEmpty() )
            {
                String rawTypeName = getRawTypeName( typeName );
                // already check here for unresolvable types due to https://github.com/paul-hammant/qdox/issues/111
                if ( typeResolver.resolveType( rawTypeName ) == null )
                {
                    throw new IllegalArgumentException( "Found unresolvable method argument type in " + member );
                }
                TypeDef typeDef = new TypeDef( getRawTypeName( typeName ) );
                int dimensions = getDimensions( typeName );
                JavaType javaType = TypeAssembler.createUnresolved( typeDef, dimensions, typeResolver );

                parameterTypes.add( javaType );
            }
        }
        return parameterTypes;
    }

    private static int getDimensions( String type )
    {
        return (int) type.chars().filter( ch -> ch == '[' ).count();
    }

    private static String getRawTypeName( String typeName )
    {
        // strip dimensions
        int indexOfOpeningBracket = typeName.indexOf( '[' );
        if ( indexOfOpeningBracket >= 0 )
        {
            return typeName.substring( 0, indexOfOpeningBracket );
        }
        else
        {
            return typeName;
        }
    }

    private static String getMethodName( String member )
    {
        // name is separated from arguments either by '(' or spans the full member
        int indexOfOpeningParentheses = member.indexOf( '(' );
        if ( indexOfOpeningParentheses == -1 )
        {
            return member;
        }
        else
        {
            return member.substring( 0, indexOfOpeningParentheses );
        }
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T setAttribute( String name, T value )
    {
        return (T) attributes.put( name, value );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T getAttribute( String name, Class<T> clazz, T defaultValue )
    {
        return (T) attributes.getOrDefault( name, defaultValue );
    }
}
