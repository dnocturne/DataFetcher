package me.dnoc.listeners;

import me.dnoc.DataFetcher;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VanishListenerTest {

    @Mock
    private DataFetcher mockPlugin;

    @Mock
    private VanishStatusChangeEvent mockEvent;

    @Mock
    private IUser mockUser;

    private VanishListener listener;

    private static final String TEST_PLAYER_NAME = "TestPlayer";

    @BeforeEach
    public void setUp() {
        listener = new VanishListener(mockPlugin);
        
        // Set up common test scenario
        when(mockEvent.getAffected()).thenReturn(mockUser);
        when(mockUser.getName()).thenReturn(TEST_PLAYER_NAME);
    }

    @Test
    public void testPlayerVanishing() throws SQLException {
        // Arrange
        when(mockEvent.getValue()).thenReturn(true); // Player is vanishing

        // Act
        listener.onVanishStatusChange(mockEvent);

        // Assert
        // When player vanishes (event.getValue() == true), online should be set to 0
        verify(mockPlugin).executeUpdate(anyString(), eq(0), eq(TEST_PLAYER_NAME));
        // Note: We can't verify logger.info calls since it's using a static final logger
    }

    @Test
    public void testPlayerBecomingVisible() throws SQLException {
        // Arrange
        when(mockEvent.getValue()).thenReturn(false); // Player is becoming visible

        // Act
        listener.onVanishStatusChange(mockEvent);

        // Assert
        // When player becomes visible (event.getValue() == false), online should be set to 1
        verify(mockPlugin).executeUpdate(anyString(), eq(1), eq(TEST_PLAYER_NAME));
        // Note: We can't verify logger.info calls since it's using a static final logger
    }

    @Test
    public void testSqlExceptionHandling() throws SQLException {
        // Arrange
        when(mockEvent.getValue()).thenReturn(true);
        doThrow(new SQLException("Test SQL exception")).when(mockPlugin).executeUpdate(anyString(), anyInt(), anyString());

        // Act
        listener.onVanishStatusChange(mockEvent);

        // Assert
        // Note: We can't verify logger.severe calls since it's using a static final logger
        // This test mostly ensures that the exception doesn't propagate outside the handler
    }
} 