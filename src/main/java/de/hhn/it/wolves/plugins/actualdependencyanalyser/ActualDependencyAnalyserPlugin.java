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
    private static final String CONFIG_FILE_NAME = "/mavendependency.properties";
    private static final String POM_FILE = "/pom.xml";
    private File workingDirectory;
    private Properties properties;

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

    public AnalysisResult analyseRepository(RepositoryInformation repositoryInformation) {
        List<Artifact> artifacts = new ArrayList<>();
        switch (repositoryInformation.getProgrammingLanguage()) {
            case JAVA:

                InvocationRequest request = new DefaultInvocationRequest();
                File pomFile = new File(repositoryInformation.getLocalDownloadPath() + POM_FILE);
                if (!pomFile.exists()) {
                    logger.info("We could not a find a pom file for this project, thus it will be excluded from the analysis.");
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                request.setPomFile(pomFile);
                request.setGoals(Collections.singletonList("dependency:analyze"));

                Invoker invoker = new DefaultInvoker();

                try {
                    //   invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));
                    invoker.setOutputHandler(new InvocationOutputHandler() {
                        @Override
                        public void consumeLine(String line) throws IOException {
                            if (line.startsWith("[WARNING]    "))
                                //  System.out.println(line);
                                artifacts.add((buildArtifactFromString(line)));

                        }
                    });
                    invoker.execute(request);
                } catch (MavenInvocationException e) {
                    e.printStackTrace();
                }
                break;
            case JAVA_SCRIPT:
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "depcheck");
                pb.directory(repositoryInformation.getLocalDownloadPath());
                pb.redirectErrorStream(true);
                Process p = null;
                try {
                    p = pb.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedReader input = new BufferedReader((new InputStreamReader(p.getInputStream())));
                ArrayList<String> dependencies = new ArrayList<>();  // Welchen Typ sollte die dependencies für NodeJS am besten haben?
                String line = null;
                while (true) {
                    try {
                        if (!((line = input.readLine()) != null)) break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (line.startsWith("*")) {
                        String split[] = line.split("\\s");
                        String dependency = split[1];
                        dependencies.add(dependency);
                    }
                }
                break;
        }

        // hier werden momentan nur die maven dependencies übergeben die vom Typ Artifact sind
        return new ActualDependencyAnalysisResult(repositoryInformation, getUniqueName(), artifacts);
    }

    private Artifact buildArtifactFromString(String line) {
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
