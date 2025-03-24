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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test for VanishListener that simulates a more realistic environment.
 * This test verifies the behavior of the VanishListener with real events.
 */
@ExtendWith(MockitoExtension.class)
public class VanishListenerIntegrationTest {

    @Mock
    private DataFetcher mockPlugin;

    @Captor
    private ArgumentCaptor<String> queryCaptor;

    @Captor
    private ArgumentCaptor<Object[]> paramsCaptor;

    private VanishListener listener;

    @BeforeEach
    public void setUp() {
        // Create the listener with our mocked plugin
        listener = new VanishListener(mockPlugin);
    }

    @Test
    public void testCompleteVanishFlow() throws SQLException {
        // Create mocked vanish event
        IUser mockUser = mock(IUser.class);
        when(mockUser.getName()).thenReturn("IntegrationTestPlayer");
        
        VanishStatusChangeEvent vanishEvent = mock(VanishStatusChangeEvent.class);
        when(vanishEvent.getAffected()).thenReturn(mockUser);
        when(vanishEvent.getValue()).thenReturn(true); // Player is vanishing
        
        // Act - Simulate the event being fired
        listener.onVanishStatusChange(vanishEvent);
        
        // Assert - Verify SQL query was executed correctly
        verify(mockPlugin).executeUpdate(queryCaptor.capture(), paramsCaptor.capture());
        
        // Verify query and parameters
        assertEquals("UPDATE players SET online = ? WHERE username = ?", queryCaptor.getValue());
        Object[] params = paramsCaptor.getValue();
        assertEquals(0, params[0]); // Online status set to 0 when vanishing
        assertEquals("IntegrationTestPlayer", params[1]);
    }
    
    @Test
    public void testBecomingVisibleFlow() throws SQLException {
        // Create mocked vanish event for becoming visible
        IUser mockUser = mock(IUser.class);
        when(mockUser.getName()).thenReturn("IntegrationTestPlayer");
        
        VanishStatusChangeEvent vanishEvent = mock(VanishStatusChangeEvent.class);
        when(vanishEvent.getAffected()).thenReturn(mockUser);
        when(vanishEvent.getValue()).thenReturn(false); // Player is becoming visible
        
        // Act - Simulate the event being fired
        listener.onVanishStatusChange(vanishEvent);
        
        // Assert - Verify SQL query was executed correctly
        verify(mockPlugin).executeUpdate(queryCaptor.capture(), paramsCaptor.capture());
        
        // Verify query and parameters
        assertEquals("UPDATE players SET online = ? WHERE username = ?", queryCaptor.getValue());
        Object[] params = paramsCaptor.getValue();
        assertEquals(1, params[0]); // Online status set to 1 when becoming visible
        assertEquals("IntegrationTestPlayer", params[1]);
    }
} 