package com.redhat.lightblue.rest.crud.tck;

import com.redhat.lightblue.config.CrudConfiguration;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.rest.RestConfiguration;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Make each test have its own classloader which can help to isolate each test case (for example static fields)
 *
 * TODO Maybe remove the Arquilian and use the Runner isntead
 */
public class FilterClassloaderForEachClassTest extends BlockJUnit4ClassRunner {
    public FilterClassloaderForEachClassTest(Class<?> clazz) throws InitializationError {
        super(getFromTestClassloader(clazz));
    }

    public static class TestClassLoader extends URLClassLoader {

        public static final String PACKAGE_FILTER = "com.redhat.lightblue.";

        public TestClassLoader() {
            super(((URLClassLoader)getSystemClassLoader()).getURLs());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith(PACKAGE_FILTER)) {
                return super.findClass(name);
            }
            return super.loadClass(name);
        }

        @Override
        public URL getResource(String name) {
            String newName = name;
            if(name != null){
                if(name.startsWith(MetadataConfiguration.FILENAME)){
                    newName = LightblueInit.lastInstance.metadataConfigurationPath;
                }else if(name.startsWith(CrudConfiguration.FILENAME)){
                    newName = LightblueInit.lastInstance.crudConfigurationPath;
                }else if(name.startsWith(RestConfiguration.DATASOURCE_FILENAME)){
                    newName = LightblueInit.lastInstance.restConfigurationPath;
                }else if(name.startsWith("config.properties")){
                    newName = LightblueInit.lastInstance.configPropertiesPath;
                }
            }
            return super.getResource(newName);
        }
    }

    public static <Z> Z newInstanceFromTestClassloader(Class<Z> clazz) throws InitializationError {
        try {
            return (Z) getFromTestClassloader(clazz).newInstance();
        } catch (InstantiationException | IllegalAccessException e ) {
            throw new InitializationError(e);
        }
    }

    public static Class<?> getFromTestClassloader(Class<?> clazz) throws InitializationError {
        ClassLoader testClassLoader = new TestClassLoader();
        Thread.currentThread().setContextClassLoader(testClassLoader);
        return getFromClassloader(clazz, testClassLoader);
    }

    public static Class<?> getFromClassloader(Class<?> clazz, ClassLoader classLoader) throws InitializationError {
        try {
            return Class.forName(clazz.getName(), true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

}