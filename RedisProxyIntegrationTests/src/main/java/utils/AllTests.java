package utils;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import http.HttpIntegrationTest;

@RunWith(Suite.class)
@SuiteClasses({HttpIntegrationTest.class})
public class AllTests {
  
}