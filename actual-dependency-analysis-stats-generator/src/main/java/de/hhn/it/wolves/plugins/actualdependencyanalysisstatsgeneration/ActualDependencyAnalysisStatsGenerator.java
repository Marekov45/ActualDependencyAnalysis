package de.hhn.it.wolves.plugins.actualdependencyanalysisstatsgeneration;

import de.hhn.it.wolves.components.StatisticGeneratorPlugin;
import de.hhn.it.wolves.domain.FrameworkManager;
import de.hhn.it.wolves.domain.GeneratedStatisticInformation;
import de.hhn.it.wolves.domain.StatisticInformation;
import de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing.MavenDependencyStatisticInformation;
import de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing.NodeDependencyStatisticInformation;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class ActualDependencyAnalysisStatsGenerator implements StatisticGeneratorPlugin {

    private static final String PLUGIN_NAME = "Actual dependency statistic generator";
    private static final Logger logger = LoggerFactory.getLogger(ActualDependencyAnalysisStatsGenerator.class.getName());
    private static final String CONFIG_FILE_NAME = "/actualdependency.properties";
    private File workingDirectory;

    private FrameworkManager frameworkManager;
    private Properties properties;

    @Override
    public GeneratedStatisticInformation generateStatistic(File file, String s, StatisticInformation statisticInformation) {
        if ((statisticInformation instanceof MavenDependencyStatisticInformation)) {
            //  for (Artifact artifact : ((MavenDependencyStatisticInformation) statisticInformation).getForwardedMavenDependencies()) {
            //     }

        }
        return null;
    }

    @Override
    public Class[] getStatisticInformationsThisPluginIsDesignedFor() {
        return new Class[]{MavenDependencyStatisticInformation.class, NodeDependencyStatisticInformation.class};
    }

    @Override
    public void init(FrameworkManager frameworkManager) {
        this.frameworkManager = frameworkManager;

        synchronized (frameworkManager) {
            workingDirectory = new File(frameworkManager.getWorkingFolderForPlugins().getAbsolutePath() + "/" + PLUGIN_NAME + "/");
            logger.info("Checking the properties...");
            File configFile = new File(workingDirectory.getAbsolutePath() + CONFIG_FILE_NAME);
            if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
                logger.error("We could not create the working directory for " + getUniqueName() + "! Aborting initialisation. Please note: The plugin will not work correctly!");
            }
            if (!configFile.exists()) {
                logger.info("We could not find a properties file for the actual dependency checker! We are generating the default one.");
                try {
                    URL url = getClass().getResource("/actualdependency.properties");
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
    }

    @Override
    public String getUniqueName() {
        return PLUGIN_NAME;
    }

    @Override
    public void cleanUp() {

    }
}
