package me.dnoc;

import me.dnoc.listeners.VanishListenerIntegrationTest;
import me.dnoc.listeners.VanishListenerSpecificQueryTest;
import me.dnoc.listeners.VanishListenerTest;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Simple test runner for the DataFetcher plugin tests.
 * This can be used to run the tests without Maven if necessary.
 */
public class TestRunner {
    
    public static void main(String[] args) {
        System.out.println("Running tests...");
        
        // Create a request to run specific test classes
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(
                selectClass(VanishListenerSpecificQueryTest.class),
                selectClass(VanishListenerTest.class),
                selectClass(VanishListenerIntegrationTest.class)
            )
            .build();
            
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        
        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        
        System.out.println("\nTest Execution Summary:");
        System.out.println("-----------------------");
        System.out.println("Tests Started: " + summary.getTestsStartedCount());
        System.out.println("Tests Found: " + summary.getTestsFoundCount());
        System.out.println("Tests Succeeded: " + summary.getTestsSucceededCount());
        System.out.println("Tests Failed: " + summary.getTestsFailedCount());
        System.out.println("Tests Skipped: " + summary.getTestsSkippedCount());
        
        // Exit with non-zero code if there were test failures
        if (summary.getTotalFailureCount() > 0) {
            System.exit(1);
        }
    }
} 