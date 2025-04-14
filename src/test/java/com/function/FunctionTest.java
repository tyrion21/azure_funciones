package com.function;

import com.microsoft.azure.functions.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for Function class.
 */
public class FunctionTest {
    /**
     * Setup test environment variables before each test
     */
    @BeforeEach
    public void setUp() {
        // Set up mock environment variables for testing
        setupMockEnvironmentVariables();
    }

    /**
     * Sets up mock environment variables for Oracle connection
     * These values are for testing only and do not need to connect to a real
     * database
     */
    private void setupMockEnvironmentVariables() {
        Map<String, String> env = new HashMap<>();
        env.put("TNS_NAME", "mock_tns");
        env.put("ORACLE_USER", "mock_user");
        env.put("ORACLE_PASSWORD", "mock_password");
        env.put("ORACLE_WALLET_PATH", "mock_wallet_path");

        try {
            // Use reflection to mock environment variables
            setEnv(env);
        } catch (Exception e) {
            System.err.println("Could not set environment variables: " + e.getMessage());
        }
    }

    // Helper method to set environment variables using reflection
    @SuppressWarnings("unchecked")
    private void setEnv(Map<String, String> newEnv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");

            // Get the private field 'theEnvironment' from ProcessEnvironment class
            java.lang.reflect.Field environmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            environmentField.setAccessible(true);

            // Get the map from the field
            Map<String, String> env = (Map<String, String>) environmentField.get(null);

            // Update the environment with the new values
            env.putAll(newEnv);

            // Get the private field 'theCaseInsensitiveEnvironment' from ProcessEnvironment
            // class
            java.lang.reflect.Field ciEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            ciEnvironmentField.setAccessible(true);

            // Get the case insensitive map
            Map<String, String> ciEnv = (Map<String, String>) ciEnvironmentField.get(null);

            // Update the case insensitive environment with the new values
            ciEnv.putAll(newEnv);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            // Alternative approach if the first method fails (e.g., for Java 9+)
            try {
                Map<String, String> env = System.getenv();
                java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
                field.setAccessible(true);

                // Update the environment
                Map<String, String> modifiableEnv = (Map<String, String>) field.get(env);
                modifiableEnv.putAll(newEnv);
            } catch (Exception e2) {
                // If both approaches fail, simply mock the OracleDBConnection in the test
                System.err.println("Could not set environment variables: " + e2.getMessage());
                System.err.println("Will need to mock OracleDBConnection instead");
            }
        }
    }

    /**
     * Unit test for HttpTriggerJava method.
     */
    @Test
    public void testHttpTriggerJava() throws Exception {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("name", "Azure");
        doReturn(queryParams).when(req).getQueryParameters();

        final Optional<String> queryBody = Optional.empty();
        doReturn(queryBody).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Invoke
        final HttpResponseMessage ret = new Function().run(req, context);

        // Verify
        assertEquals(HttpStatus.OK, ret.getStatus());
    }
}
