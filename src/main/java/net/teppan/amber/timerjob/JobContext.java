package net.teppan.amber.timerjob;

import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;

/**
 * Context interface for TimerJob execution environment.
 * 
 * <p>Provides access to logging, configuration properties, and other
 * execution environment resources needed by TimerJob implementations.</p>
 * 
 * @since 1.0
 * @author Juanita Development
 */
public interface JobContext {
    
    /**
     * Returns the logger instance for this context.
     * 
     * @return SLF4J logger instance, never null
     */
    Logger getLogger();
    
    /**
     * Returns the job name if available.
     * 
     * @return job name or empty if not set
     */
    Optional<String> getJobName();
    
    /**
     * Returns all properties associated with this context.
     * 
     * @return properties instance or empty properties if none set
     */
    Properties getProperties();
    
    /**
     * Returns a property value by name.
     * 
     * @param name property name, must not be null
     * @return property value or empty if not found
     * @throws IllegalArgumentException if name is null
     */
    Optional<String> getProperty(String name);
    
    /**
     * Returns a property value by name with a default value.
     * 
     * @param name property name, must not be null
     * @param defaultValue default value to return if property not found
     * @return property value or default value if not found
     * @throws IllegalArgumentException if name is null
     */
    String getProperty(String name, String defaultValue);
    
    /**
     * Sets a property value.
     * 
     * @param name property name, must not be null
     * @param value property value, null values will remove the property
     * @throws IllegalArgumentException if name is null
     */
    void setProperty(String name, String value);
} 