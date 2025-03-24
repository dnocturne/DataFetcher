package me.dnoc.listeners;

import me.dnoc.DataFetcher;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This test class focuses on verifying the exact SQL query used in the VanishListener.
 */
@ExtendWith(MockitoExtension.class)
public class VanishListenerSpecificQueryTest {

    @Mock
    private DataFetcher mockPlugin;

    @Mock
    private VanishStatusChangeEvent mockEvent;

    @Mock
    private IUser mockUser;

    @Captor
    private ArgumentCaptor<String> queryCaptor;

    @Captor
    private ArgumentCaptor<Object[]> paramsCaptor;

    private VanishListener listener;

    private static final String TEST_PLAYER_NAME = "TestPlayer";
    private static final String EXPECTED_QUERY = "UPDATE players SET online = ? WHERE username = ?";

    @BeforeEach
    public void setUp() {
        listener = new VanishListener(mockPlugin);
        
        // Set up common test scenario
        when(mockEvent.getAffected()).thenReturn(mockUser);
        when(mockUser.getName()).thenReturn(TEST_PLAYER_NAME);
    }

    @Test
    public void testExactQueryWhenVanishing() throws SQLException {
        // Arrange
        when(mockEvent.getValue()).thenReturn(true); // Player is vanishing

        // Act
        listener.onVanishStatusChange(mockEvent);

        // Assert the exact query and parameters
        verify(mockPlugin).executeUpdate(queryCaptor.capture(), paramsCaptor.capture());
        
        // Verify the query string
        assertEquals(EXPECTED_QUERY, queryCaptor.getValue(), "The SQL query should match the expected update statement");
        
        // Verify the parameters (0 for online status when vanished, and player name)
        Object[] capturedParams = paramsCaptor.getValue();
        assertEquals(0, capturedParams[0], "First parameter should be 0 when vanishing");
        assertEquals(TEST_PLAYER_NAME, capturedParams[1], "Second parameter should be the player name");
    }

    @Test
    public void testExactQueryWhenBecomingVisible() throws SQLException {
        // Arrange
        when(mockEvent.getValue()).thenReturn(false); // Player is becoming visible

        // Act
        listener.onVanishStatusChange(mockEvent);

        // Assert the exact query and parameters
        verify(mockPlugin).executeUpdate(queryCaptor.capture(), paramsCaptor.capture());
        
        // Verify the query string
        assertEquals(EXPECTED_QUERY, queryCaptor.getValue(), "The SQL query should match the expected update statement");
        
        // Verify the parameters (1 for online status when visible, and player name)
        Object[] capturedParams = paramsCaptor.getValue();
        assertEquals(1, capturedParams[0], "First parameter should be 1 when becoming visible");
        assertEquals(TEST_PLAYER_NAME, capturedParams[1], "Second parameter should be the player name");
    }
} 