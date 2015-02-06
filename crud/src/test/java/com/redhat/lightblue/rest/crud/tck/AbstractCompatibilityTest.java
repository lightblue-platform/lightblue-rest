package com.redhat.lightblue.rest.crud.tck;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.assertEquals;

@RunWith(FilterClassloaderForEachClassTest.class)
public class AbstractCompatibilityTest {


    // This is required as the JUnit needs at least one test when a class is annotated with @RunWith
    @Test
    public void testRequired(){assertEquals(0,0);}

    /*
    generic util methods here
     */
}
