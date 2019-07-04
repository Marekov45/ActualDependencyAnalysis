package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.components.AnalysisPlugin;
import de.hhn.it.wolves.domain.*;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ActualDependencyAnalyserPlugin implements AnalysisPlugin {

    private static final String PLUGIN_NAME = "Actual dependency analyser";
    private static final Logger logger = LoggerFactory.getLogger(ActualDependencyAnalyserPlugin.class.getName());
    private static final String CONFIG_FILE_NAME = "/actualdependency.properties";
    private static final String POM_FILE = "/pom.xml";
    private static final String JSON_FILE = "/package.json";
    private File workingDirectory;

    private FrameworkManager frameworkManager;
    private Properties properties;

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

    public AnalysisResult analyseRepository(RepositoryInformation repositoryInformation) {
        switch (repositoryInformation.getProgrammingLanguage()) {
            case JAVA:

                InvocationRequest request = new DefaultInvocationRequest();
                File pomFile = new File(repositoryInformation.getLocalDownloadPath() + POM_FILE);
                if (!pomFile.exists()) {
                    logger.info("We could not a find a pom file for this project, thus it will be excluded from the analysis.");
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                request.setPomFile(pomFile);
                //  request.setGoals(Collections.singletonList("dependency:analyze"));
                request.setGoals(Collections.singletonList("dependency:list"));

                Invoker invoker = new DefaultInvoker();

                ArrayList<String> mavenPluginOutput = new ArrayList<>();
                List<Artifact> artifacts = new ArrayList<>();
                try {

                    invoker.setOutputHandler(new InvocationOutputHandler() {
                        @Override
                        public void consumeLine(String line) throws IOException {
                            //       if (line.startsWith("[WARNING] Unused declared") || line.startsWith("[WARNING]    ")) {
                            if (line.startsWith("[INFO]    ")) {
                                mavenPluginOutput.add(line);
                            }
                        }
                    });
                    invoker.execute(request);
                } catch (MavenInvocationException e) {
                    e.printStackTrace();
                }

                //   String mavenText = "[WARNING] Unused declared dependencies found:";
                //   if (!mavenPluginOutput.contains(mavenText)) {
                //       logger.info("This project has no unused declared dependencies.");
                //   return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                //   }
                //   for (int i = getUnusedDependencyIndex(mavenPluginOutput, mavenText) + 1; i < mavenPluginOutput.size(); i++) {
                for (int i = 0; i < mavenPluginOutput.size(); i++) {
                    artifacts.add(buildArtifactFromString(mavenPluginOutput, i));
                }
                return new MavenDependencyAnalysisResult(repositoryInformation, getUniqueName(), artifacts);

            case JAVA_SCRIPT:
                File packageJSONFile = new File(repositoryInformation.getLocalDownloadPath() + JSON_FILE);
                if (!packageJSONFile.exists()) {
                    logger.info("We could not a find a package.json file for this project, thus it will be excluded from the analysis.");
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                // ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "depcheck");
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "npm list --depth 0");
                pb.directory(repositoryInformation.getLocalDownloadPath());
                pb.redirectErrorStream(true);
                Process p = null;
                try {
                    p = pb.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedReader input = new BufferedReader((new InputStreamReader(p.getInputStream())));
                ArrayList<String> allDependencies = new ArrayList<>();
                String line = null;
                while (true) {
                    try {
                        //if (!((line = input.readLine()) != null)) break;
                        line = input.readLine();
                        if (line == null) {
                            break;
                        } else if (line.contains("--")) {
                            allDependencies.add(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //   allDependencies.add(line);

                }
                ArrayList<String> nodeDependencies = new ArrayList<>();
                for (int i = 0; i < allDependencies.size(); i++) {
                    nodeDependencies.add(getNameOfDependency(allDependencies, i));
                }

                //  String depcheckText = "Missing dependencies";
                //  for (int i = 0; i < getUnusedDependencyIndex(allDependencies, depcheckText); i++) {
                //      unusedDependencies.add(allDependencies.get(i));
                //   }

                //  unusedDependencies.remove("Unused dependencies");
                //  unusedDependencies.remove("Unused devDependencies");
                return new NodeDependencyAnalysisResult(repositoryInformation, getUniqueName(), nodeDependencies);
        }

        logger.error("This part should never been reached");
        return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
    }

    private String getNameOfDependency(ArrayList<String> allDependencies, int i) {
        String line = allDependencies.get(i);
        String splitValues[] = line.split("\\s");
        String dependency = splitValues[1];
        return dependency;
    }


    private int getUnusedDependencyIndex(ArrayList<String> output, String startingPoint) {
        String text = startingPoint;
        int index = 0;
        int counter = 0;
        for (String start : output) {
            if (start.equals(text)) {
                index = counter;
            }
            counter++;
        }
        return index;
    }

    private Artifact buildArtifactFromString(ArrayList<String> pluginOutput, int unusedDependencyIndex) {
        String line = pluginOutput.get(unusedDependencyIndex);
        String splitValues[] = line.split(":|\\s+");
        String groupId = splitValues[1];
        String artifactId = splitValues[2];
        String type = splitValues[3];
        String version = splitValues[4];
        String scope = splitValues[5];
        Artifact artifact = new DefaultArtifact(groupId, artifactId, version, scope, type, null, new DefaultArtifactHandler());
        return artifact;

    }


    public ProgrammingLanguage[] getLanguageThisPluginIsDesignedFor() {
        return new ProgrammingLanguage[]{ProgrammingLanguage.JAVA, ProgrammingLanguage.JAVA_SCRIPT};
    }


    public String getUniqueName() {
        return PLUGIN_NAME;
    }

    public void cleanUp() {

    }
}
