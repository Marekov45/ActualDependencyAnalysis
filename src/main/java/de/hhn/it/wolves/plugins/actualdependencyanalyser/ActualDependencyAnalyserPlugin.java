package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.components.AnalysisPlugin;
import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.FrameworkManager;
import de.hhn.it.wolves.domain.ProgrammingLanguage;
import de.hhn.it.wolves.domain.RepositoryInformation;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class ActualDependencyAnalyserPlugin implements AnalysisPlugin {

    private static final String PLUGIN_NAME = "Actual dependency analyser";
    private static final Logger logger = LoggerFactory.getLogger(ActualDependencyAnalyserPlugin.class.getName());
    private static final String CONFIG_FILE_NAME = "/mavendependency.properties";
    private File workingDirectory;
    private Properties properties;

    public AnalysisResult analyseRepository(RepositoryInformation repositoryInformation) {
        return null;
    }

    public ProgrammingLanguage[] getLanguageThisPluginIsDesignedFor() {
        return new ProgrammingLanguage[]{ProgrammingLanguage.JAVA, ProgrammingLanguage.JAVA_SCRIPT};
    }

    public void init(FrameworkManager frameworkManager) {
        workingDirectory = new File(frameworkManager.getWorkingFolderForPlugins().getAbsolutePath() + "/" + PLUGIN_NAME + "/");
        logger.info("Checking the properties...");
        File configFile = new File(workingDirectory.getAbsolutePath() + CONFIG_FILE_NAME);
        if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
            logger.error("We could not create the working directory for " + getUniqueName() + "! Aborting initialisation. Please note: The plugin will not work correctly!");
        }
        if (!configFile.exists()) {
            logger.info("We could not find a properties file for the actual dependency checker! We are generating the default one.");
            try {
                URL url = getClass().getResource("/mavendependency.properties");
                FileUtils.copyURLToFile(url, configFile);
            } catch (IOException e) {
                logger.error("Unexpected Error occurred", e);
                System.exit(999);
            }
        }
        properties = new Properties();
        try {
            properties.load(new FileInputStream(configFile));
        } catch (IOException e) {
            logger.error("Could not read the config file!");
        }
    }

    public String getUniqueName() {
        return PLUGIN_NAME;
    }

    public void cleanUp() {

    }
}
