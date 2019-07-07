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

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
        String seperator = ";";
        List<String> lines = new ArrayList<>();
        lines.add("Dependency;Version;Unused");
        boolean foundUnused = false;
        if ((statisticInformation instanceof MavenDependencyStatisticInformation)) {

            for (Artifact artifact : ((MavenDependencyStatisticInformation) statisticInformation).getAllForwardedMavenDependencies()) {
                StringBuilder sb = new StringBuilder(artifact.getArtifactId());
                sb.append(seperator).append(artifact.getVersion());
                // check if the repo has any unused dependencies
                if (!((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().isEmpty()) {
                    for (Artifact artifact2 : ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies()) {
                        if (artifact2.equals(artifact)) {
                            sb.append(seperator).append("X");
                        }
                    }
                    foundUnused = true;
                }
                lines.add(sb.toString());
            }
        } else if ((statisticInformation instanceof NodeDependencyStatisticInformation)) {
            for (String str : ((NodeDependencyStatisticInformation) statisticInformation).getAllForwardedNodeDependencies()) {
                String allSplitValues[] = str.split("@");
                String dependency = allSplitValues[0];
                String version = allSplitValues[1];
                StringBuilder sb = new StringBuilder(dependency);
                sb.append(seperator).append(version);
                if (!((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies().isEmpty()) {
                    for (String str2 : ((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies()) {
                        String unusedSplitValues[] = str2.split("\\s");
                        if (unusedSplitValues[1].equals(dependency)) {
                            sb.append(seperator).append("X");
                        }
                    }
                    foundUnused = true;
                }
                lines.add(sb.toString());

            }
        } else {
            logger.error("I got an statistic information that I am not designed for!");
            return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file.getAbsolutePath() + "/" + s + ".csv"));
            if (foundUnused) {
                writer = new BufferedWriter(new FileWriter(file.getAbsolutePath() + "/[UNUSED] " + s + ".csv"));
            }
            for (String string : lines) {
                writer.write(string);
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("We could not write the report for this specific repository!", e);
        } finally {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("We could not close the buffered writer. Maybe data will be lost or not generated!");
                }
        }


        return new GeneratedStatisticInformation(statisticInformation, file, getUniqueName());
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
