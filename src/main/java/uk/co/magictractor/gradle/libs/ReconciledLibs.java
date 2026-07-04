/**
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.magictractor.gradle.libs;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.internal.DynamicObjectAware;
import org.gradle.api.plugins.ExtensionsSchema;
import org.gradle.api.plugins.ExtensionsSchema.ExtensionSchema;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reflect.TypeOf;
import org.gradle.internal.extensibility.DefaultExtensionContainer;
import org.gradle.internal.extensibility.DefaultExtensionsSchema;
import org.gradle.internal.extensibility.ExtensibleDynamicObject;
import org.gradle.internal.instantiation.InstanceGenerator;
import org.gradle.internal.metaobject.AbstractDynamicObject;
import org.gradle.internal.metaobject.DynamicObject;

import groovy.lang.MissingPropertyException;

// Could Use DefaultNamedDomainObjectCollection<T>? That handles the DynamicObject goodness.
// MethodMixIn might suffice - see DefaultDependencyHandler
public class ReconciledLibs implements DynamicObjectAware {

    // or BeanDynamicObject?

    /*
     * class uk.co.magictractor.gradle.libs.DynamicThingie$Dyno cannot be cast
     * to class org.gradle.internal.extensibility.ExtensibleDynamicObject
     * (uk.co.magictractor.gradle.libs.DynamicThingie$Dyno is in unnamed module
     * of loader org.gradle.internal.classloader.
     * VisitableURLClassLoader$InstrumentingVisitableURLClassLoader @52c7d549;
     * org.gradle.internal.extensibility.ExtensibleDynamicObject is in unnamed
     * module of loader
     * org.gradle.internal.classloader.VisitableURLClassLoader @685f4c2e)
     */
    // so needs to be ExtensibleDynamicObject? see DefaultProject
    //private final DynamicObject dynamicObject = new Dyno();

    private ExtensibleDynamicObject dynamicObject;
    private Provider<Integer> javaVersion;

    private int dynoCall = 0;
    private ProviderFactory providerFactory;

    /*
     * class org.gradle.internal.metaobject.BeanDynamicObject cannot be cast to
     * class org.gradle.internal.extensibility.ExtensibleDynamicObject
     * (org.gradle.internal.metaobject.BeanDynamicObject and
     * org.gradle.internal.extensibility.ExtensibleDynamicObject are in unnamed
     * module of loader
     * org.gradle.internal.classloader.VisitableURLClassLoader @685f4c2e)
     */
    //private final DynamicObject dynamicObject = new BeanDynamicObject(this);

    @Inject
    public ReconciledLibs(Provider<Integer> javaVersion, InstanceGenerator instanceGenerator, ProviderFactory providerFactory) {
        //        dynamicObject = new ExtensibleDynamicObject(this, getClass(), instanceGenerator) {
        //            @Override
        //            public Object getProperty(String name) throws MissingPropertyException {
        //                // not called?? probably need to look at the extension container...
        //                System.out.println(">>> getProperty(): " + javaVersion + "  " + javaVersion.isPresent());
        //                Object p = super.getProperty(name);
        //                return p;
        //            }
        //
        //            // ExtensionContainer is interface so could wrap...
        //            // or (better) pass different impl to the ExtensibleDynamicObject constructor
        //            @Override
        //            public ExtensionContainer getExtensions() {
        //                System.out.println(">>> getExtensions(): " + javaVersion + "  " + javaVersion.isPresent());
        //                return super.getExtensions();
        //            }
        //        };

        //dynamicObject = new ExtensibleDynamicObject(this, new BeanDynamicObject(this, getClass()), new CustomContainer(instanceGenerator));

        this.providerFactory = providerFactory;
        dynamicObject = new ExtensibleDynamicObject(this, getClass(), instanceGenerator);

        // Yes!! - now look at passing in the java version...
        dynamicObject.getExtensions().add("jsoup", providerFactory.provider(() -> lookup("jsoup")));

        System.out.println("dyno constructor: " + javaVersion + "  " + javaVersion.isPresent());
        this.javaVersion = javaVersion;
    }

    // No. Cannot defer via a lookup like dynamicObject.getExtensions().add("jsoup", this::lookup);
    //
    //    Cannot convert the provided notation to an object of type Dependency: uk.co.magictractor.gradle.libs.DynamicThingie$$Lambda/0x00000000bd6a78d8@11367c85.
    //    The following types/formats are supported:
    //      - String or CharSequence values, for example 'org.gradle:gradle-core:1.0'.
    //      - Maps, for example [group: 'org.gradle', name: 'gradle-core', version: '1.0'].
    //      - FileCollections, for example files('some.jar', 'someOther.jar').
    //      - Projects, for example project(':some:project:path').
    //      - ClassPathNotation, for example gradleApi().
    //
    private String lookup(String key) {
        System.out.println("dyno lookup(): " + javaVersion + "  " + javaVersion.isPresent());
        return "org.jsoup:jsoup:1.21.2";
    }

    @Override
    public DynamicObject getAsDynamicObject() {
        // all calls from decorated getExtension()...
        //        System.out.println();
        //        System.out.println("getAsDynamicObject()  #" + ++dynoCall);
        //        new RuntimeException().printStackTrace(System.out);

        // Hmm. Called four times, not present until fourth...
        System.out.println("dyno getAsDynamicObject(): " + javaVersion + "  " + javaVersion.isPresent());

        if (javaVersion.isPresent()) {
            // Too late if I do this...
            //dynamicObject.getExtensions().add("jsoup", "org.jsoup:jsoup:1.21.2");
        }

        // if (dynamicObject == null) {
        //     dynamicObject = new ExtensibleDynamicObject(this, getClass(), null);
        // }
        return dynamicObject;
    }

    private class Dyno extends AbstractDynamicObject {

        @Override
        public String getDisplayName() {
            return "libs";
        }

        @Override
        public Object getProperty(String name) throws MissingPropertyException {
            System.out.println("getProperty(" + name + ")");
            return super.getProperty(name);
        }

    }

    private class CustomContainer extends DefaultExtensionContainer {

        public CustomContainer(InstanceGenerator instanceGenerator) {
            super(instanceGenerator);
        }

        @Override
        public ExtensionsSchema getExtensionsSchema() {
            ExtensionsSchema def = super.getExtensionsSchema();
            Iterator<ExtensionSchema> defIter = def.iterator();
            if (!"ext".equals(defIter.next().getName())) {
                throw new IllegalStateException("Expected ext");
            }
            if (defIter.hasNext()) {
                throw new IllegalStateException("Expected only ext, found " + defIter.next());
            }

            ExtensionsSchema mine = DefaultExtensionsSchema.create(List.of(new MyExtensionSchema()));

            System.out.println(">>> getExtensionsSchema() -> " + mine);
            return mine;
        }

        @Override
        public Object getByName(String name) {
            // No!!! JavaVersion is not present yet...
            System.out.println(">>> getByName(" + name + ")  " + javaVersion.isPresent());
            return super.getByName(name);
        }

        @Override
        public Object findByName(String name) {
            System.out.println(">>> findByName(" + name + ")");
            return super.findByName(name);
        }

        @Override
        public Object propertyMissing(String name) {
            System.out.println(">>> propertyMissing(" + name + ")");
            return super.propertyMissing(name);
        }

    }

    private static final class MyExtensionSchema implements ExtensionsSchema.ExtensionSchema {

        @Override
        public String getName() {
            return "jsoup";
        }

        @Override
        public TypeOf<?> getPublicType() {
            return TypeOf.typeOf(String.class);
        }
    }

}
