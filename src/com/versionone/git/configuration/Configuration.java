package com.versionone.git.configuration;

import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@XmlRootElement(name="Configuration")
public class Configuration {
    @XmlElement(name = "LocalDirectory")
    private String localDirectory;
    @XmlElement(name = "PollIntervalInSeconds")
    private int pollIntervalInSeconds;
    @XmlElement(name = "VersionOneConnection")
    private VersionOneConnection versionOneConnection;
    @XmlElement(name = "GitConnection")
    @XmlElementWrapper(name = "GitConnections")
    private List<GitConnection> gitConnections;
    @XmlElement(name = "ChangeSet")
    private ChangeSet changeSet;

    private static Configuration configuration;
    private static final Logger LOG = Logger.getLogger("GitIntegration");

    private Configuration() {
    }

    /***
     * Load configuration.
     * @return configuration
     */
    public static Configuration getInstance() {
        return getInstance("configuration.xml");
    }

    /***
     * Load specify configuration. 
     * @param fullPathToConfig full path to config file (include file)
     * @return configuration
     */
    public static Configuration getInstance(String fullPathToConfig) {
        if (configuration == null) {
            configuration = loadConfiguration(fullPathToConfig);
        }
        return configuration;
    }

    public static void reset() {
        configuration = null;
    }

    private static Configuration loadConfiguration(String fileName) {
        Configuration config = null;
        InputStream stream = null;
        try {
            final Class<Configuration> thisClass = Configuration.class;
            JAXBContext jc = JAXBContext.newInstance(thisClass);

            Unmarshaller um = jc.createUnmarshaller();
            stream = new FileInputStream(fileName);
            config = (Configuration) um.unmarshal(stream);
        } catch (JAXBException ex) {
            LOG.warn("Can't use configuration", ex);
        } catch (FileNotFoundException ex) {
            LOG.warn("Can't load configuration.xml", ex);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                    // Do nothing
                }
            }
        }
        return config;
    }

    public String getLocalDirectory() {
        return localDirectory;
    }

    public int getPollIntervalInSeconds() {
        return pollIntervalInSeconds;
    }

    public VersionOneConnection getVersionOneConnection() {
        return versionOneConnection;
    }

    public List<GitConnection> getGitConnections() {
        return gitConnections;
    }

    public ChangeSet getChangeSet() {
        return changeSet;
    }
}
