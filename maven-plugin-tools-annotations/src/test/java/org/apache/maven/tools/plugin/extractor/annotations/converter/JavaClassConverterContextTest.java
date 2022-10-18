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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Objects;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import org.apache.maven.tools.plugin.extractor.annotations.converter.test.CurrentClass;
import org.apache.maven.tools.plugin.extractor.annotations.converter.test.OtherClass;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.MojoAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.apache.maven.tools.plugin.javadoc.JavadocReference;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference.MemberType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaClassConverterContextTest
{

    private ConverterContext context;

    private final String currentPackageName;

    private final JavaProjectBuilder builder;

    private final JavaClass contextClass;

    private JavadocLinkGenerator linkGenerator;

    private URI javadocBaseUri;
    
    public JavaClassConverterContextTest()
        throws URISyntaxException
    {
        builder = new JavaProjectBuilder();
        builder.addSourceFolder( new File("src/test/java") );
        
        contextClass = builder.getClassByName( CurrentClass.class.getName() );
        currentPackageName = contextClass.getPackageName();
        javadocBaseUri = new URI("http://localhost/apidocs");
        linkGenerator = new JavadocLinkGenerator( javadocBaseUri, "11" );
        context = new JavaClassConverterContext( contextClass, builder, Collections.emptyMap(), linkGenerator, 10 );
    }

    @Test
    void testResolveReference()
        throws URISyntaxException
    {
        // test fully qualified unresolvable reference
        assertThrows( IllegalArgumentException.class,
                      () -> context.resolveReference( JavadocReference.parse( "my.package.InvalidClass" ) ) );
        
        // test unresolvable reference
        assertThrows( IllegalArgumentException.class,
                      () -> context.resolveReference( JavadocReference.parse( "InvalidClass" ) ) );
        
        // test resolvable reference
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName,
                                                          "OtherClass", false ),
                      context.resolveReference( ( JavadocReference.parse( OtherClass.class.getName() ) ) ) );
        
        // already fully resolved class
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "OtherClass", false ),
                                                        context.resolveReference( ( JavadocReference.parse( OtherClass.class.getName() ) ) ) );
        // already fully resolved package
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, false ),
                                                        context.resolveReference( ( JavadocReference.parse( currentPackageName ) ) ) );
        
        // Class from java's standard import "java.lang"
        assertEquals( new FullyQualifiedJavadocReference( "java.lang", "String", true ),
                      context.resolveReference( JavadocReference.parse( "String" ) ) );
        
        // nested class from import
        assertEquals( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test.other",
                                                          "OtherClassOtherPackage.EmbeddedEnum", false ),
                      context.resolveReference( ( JavadocReference.parse( "OtherClassOtherPackage.EmbeddedEnum" ) ) ) );
    }

    @Test
    void testResolveReferenceWithMembers()
    {
        // field
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "field1", MemberType.FIELD, false ),
                      context.resolveReference( ( JavadocReference.parse( "#field1" ) ) ) );
        // field from super class
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "SuperClass", "superField1", MemberType.FIELD, false ),
                      context.resolveReference( ( JavadocReference.parse( "#superField1" ) ) ) );
        // method
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "noParamMethod()", MemberType.METHOD, false ),
                                                        context.resolveReference( ( JavadocReference.parse( "#noParamMethod()" ) ) ) );
        // method without parentheses
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "noParamMethod()", MemberType.METHOD, false ),
                                                        context.resolveReference( ( JavadocReference.parse( "#noParamMethod" ) ) ) );
       
        // method with unresolved java.lang argument
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "simpleParamMethod(java.lang.Integer)", MemberType.METHOD, false ),
                                                          context.resolveReference( ( JavadocReference.parse( "#simpleParamMethod(Integer)" ) ) ) );
        
        // method with unresolved java.lang argument with name
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "simpleParamMethod(java.lang.Integer)", MemberType.METHOD, false ),
                                                         context.resolveReference( ( JavadocReference.parse( "#simpleParamMethod(Integer value)" ) ) ) );

        // method with primitive arguments
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "complexParamMethod(int,org.apache.maven.tools.plugin.extractor.annotations.converter.test.other.OtherClassOtherPackage.EmbeddedEnum)", MemberType.METHOD, false ),
                                                         context.resolveReference( ( JavadocReference.parse( "#complexParamMethod(int value1, OtherClassOtherPackage.EmbeddedEnum value2)" ) ) ) );
        // method with array arguments
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "arrayParamMethod(int[],java.lang.String[][][])", MemberType.METHOD, false ),
                context.resolveReference( ( JavadocReference.parse( "#arrayParamMethod(int[], String[][][])" ) ) ) );
        
        // method with generic arguments
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "genericsParamMethod(java.util.Collection,java.util.function.BiConsumer)", MemberType.METHOD, false ),
                context.resolveReference( ( JavadocReference.parse( "#genericsParamMethod(Collection something, java.util.function.BiConsumer function)" ) ) ) );
        
        // method with unresolvable type
        assertThrows( IllegalArgumentException.class,
                () -> context.resolveReference( ( JavadocReference.parse( "#genericsParamMethod(Collection something, BiConsumer function)" ) ) ) );
        // constructor
        assertEquals( new FullyQualifiedJavadocReference( currentPackageName, "CurrentClass", "CurrentClass()", MemberType.CONSTRUCTOR, false ),
                                                        context.resolveReference( ( JavadocReference.parse( "#CurrentClass()" ) ) ) );

    }

    @Test
    void testGetUrl()
        throws URISyntaxException
    {
        MojoAnnotationContent mojoAnnotationContent = new MojoAnnotationContent();
        mojoAnnotationContent.name("other-goal");
        MojoAnnotatedClass mojoAnnotatedClass = new MojoAnnotatedClass().setMojo( mojoAnnotationContent );
        context = new JavaClassConverterContext( contextClass, builder, Collections.singletonMap( "org.apache.maven.tools.plugin.extractor.annotations.converter.test.OtherClass", mojoAnnotatedClass ), linkGenerator, 10 );
        // TODO: link to current class without member?
        //assertEquals( new URI( "" ),
        //              context.getUrl( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter",
        //                                                                  "JavaClassConverterContextTest" ) ) );

        // field reference not leaving context
        assertEquals( new URI( null, null, "field1" ),
                      context.getUrl( new FullyQualifiedJavadocReference( currentPackageName,
                                                                          "CurrentClass", "field1", MemberType.FIELD, false ) ) );

        // field reference in another class
        assertEquals( javadocBaseUri.resolve( new URI( null, "org/apache/maven/tools/plugin/extractor/annotations/converter/test/other/OtherClassOtherPackage.html", "field1" ) ),
                      context.getUrl( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test.other",
                                                                          "OtherClassOtherPackage", "field1", MemberType.FIELD, false ) ) );

        // field reference in another mojo
        assertEquals( new URI( null, "./other-goal-mojo.html", "field1" ),
                      context.getUrl( new FullyQualifiedJavadocReference( currentPackageName,
                                                                          "OtherClass", "field1", MemberType.FIELD, false ) ) );
        
        // method reference in current context need to point to regular javadoc as mojo documentation does not include methods
        assertEquals( javadocBaseUri.resolve( new URI( null, "org/apache/maven/tools/plugin/extractor/annotations/converter/test/CurrentClass.html", "noParamMethod()" ) ),
                      context.getUrl( new FullyQualifiedJavadocReference( currentPackageName,
                                                                          "CurrentClass", "noParamMethod()", MemberType.METHOD, false ) ) );
        
        // method reference with arguments

        
        // constructor reference in current context need to point to regular javadoc as mojo documentation does not include constructors
        assertEquals( javadocBaseUri.resolve( new URI( null, "org/apache/maven/tools/plugin/extractor/annotations/converter/test/CurrentClass.html", "CurrentClass()" ) ),
                      context.getUrl( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", 
                                                                          "CurrentClass", "CurrentClass()", MemberType.METHOD, false ) ) );

        // package reference
        assertEquals( javadocBaseUri.resolve( new URI( null, "org/apache/maven/tools/plugin/extractor/annotations/converter/test/package-summary.html", null ) ),
                      context.getUrl( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", false ) ) );

    }

    @Test
    void testClassContext()
    {
        assertTrue( context.isReferencedBy( new FullyQualifiedJavadocReference( currentPackageName,  "CurrentClass", false ) ) );
        assertTrue( context.isReferencedBy( new FullyQualifiedJavadocReference( currentPackageName,  "SuperClass", false ) ) );
        assertTrue( context.isReferencedBy( new FullyQualifiedJavadocReference( currentPackageName,  "SuperClass", "superField1", MemberType.FIELD, false ) ) );
        assertFalse( context.isReferencedBy( new FullyQualifiedJavadocReference( currentPackageName,  "OtherClass", false ) ) );
        assertEquals( currentPackageName, context.getPackageName() );
        assertEquals( "src/test/java/org/apache/maven/tools/plugin/extractor/annotations/converter/test/CurrentClass.java:10",
                      context.getLocation() );
    }
    
    @Test
    void testGetStaticFieldValue()
    {
        assertEquals( "\"STATIC 1\"", context.getStaticFieldValue( new FullyQualifiedJavadocReference( currentPackageName,  "OtherClass", "STATIC_1", MemberType.FIELD, false ) ) );
        assertEquals( "\"STATIC 2\"", context.getStaticFieldValue( new FullyQualifiedJavadocReference( currentPackageName,  "OtherClass", "STATIC_2", MemberType.FIELD, false ) ) );
        // although not explicitly stated, never used for value javadoc tag, as this only supports string constants
        assertEquals( "3l", context.getStaticFieldValue( new FullyQualifiedJavadocReference( currentPackageName,  "OtherClass", "STATIC_3", MemberType.FIELD, false ) ) );
        FullyQualifiedJavadocReference reference = new FullyQualifiedJavadocReference( currentPackageName,  "OtherClass", "field1", MemberType.FIELD, false );
        assertThrows( IllegalArgumentException.class, () -> context.getStaticFieldValue( reference ) );
    }
}
