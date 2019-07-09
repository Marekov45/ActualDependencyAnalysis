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

                ArrayList<String> allMavenDependencies = new ArrayList<>();
                List<Artifact> allArtifacts = new ArrayList<>();
                try {

                    invoker.setOutputHandler(new InvocationOutputHandler() {
                        @Override
                        public void consumeLine(String line) throws IOException {
                            //       if (line.startsWith("[WARNING] Unused declared") || line.startsWith("[WARNING]    ")) {
                            if (line.startsWith("[INFO]    ")) {
                                allMavenDependencies.add(line);
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
                for (int i = 0; i < allMavenDependencies.size(); i++) {
                    allArtifacts.add(buildArtifactFromString(allMavenDependencies, i));
                }

                ArrayList<String> unusedMavenDependencies = new ArrayList<>();
                List<Artifact> unusedArtifacts = new ArrayList<>();
                try {

                    invoker.setOutputHandler(new InvocationOutputHandler() {
                        @Override
                        public void consumeLine(String line) throws IOException {
                            if (line.startsWith("[WARNING] Unused declared") || line.startsWith("[WARNING]    ")) {

                                unusedMavenDependencies.add(line);
                            }
                        }
                    });
                    invoker.execute(request);
                } catch (MavenInvocationException e) {
                    e.printStackTrace();
                }

                String mavenText = "[WARNING] Unused declared dependencies found:";

                // check if repo has unused dependencies
                if (unusedMavenDependencies.contains(mavenText)) {
                    for (int i = getUnusedDependencyIndex(unusedMavenDependencies, mavenText) + 1; i < unusedMavenDependencies.size(); i++) {
                        unusedArtifacts.add(buildArtifactFromString(unusedMavenDependencies, i));
                    }
                }
                return new MavenDependencyAnalysisResult(repositoryInformation, getUniqueName(), allArtifacts, unusedArtifacts);

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
                ArrayList<String> allNodeDependencies = new ArrayList<>();
                String line = null;
                while (true) {
                    try {
                        //if (!((line = input.readLine()) != null)) break;
                        line = input.readLine();
                        if (line == null) {
                            break;
                        } else if (line.contains("--")) {
                            allNodeDependencies.add(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                ArrayList<String> nodeDependencies = new ArrayList<>();
                for (int i = 0; i < allNodeDependencies.size(); i++) {
                    nodeDependencies.add(getNameOfDependency(allNodeDependencies, i));
                }
                //the command for getting all unused dependencies
                pb.command("cmd", "/c", "depcheck");
                Process p2 = null;
                try {
                    p2 = pb.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(p2.getInputStream()));

                String row = null;
                ArrayList<String> depcheckDependencies = new ArrayList<>();
                while (true) {
                    try {
                        if (!((row = reader.readLine()) != null)) break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    depcheckDependencies.add(row);
                }
                ArrayList<String> unusedNodeDependencies = new ArrayList<>();
                String depcheckText = "Missing dependencies";
                if (depcheckDependencies.contains(depcheckText)) {
                    for (int i = 0; i < getUnusedDependencyIndex(depcheckDependencies, depcheckText); i++) {
                        unusedNodeDependencies.add(depcheckDependencies.get(i));
                    }
                } else {
                    for (int j = 0; j <= depcheckDependencies.size(); j++) {
                        unusedNodeDependencies.add(depcheckDependencies.get(j));
                    }
                }
                //if it doesnt contain the element it remains unchanged
                unusedNodeDependencies.remove("Unused dependencies");
                unusedNodeDependencies.remove("Unused devDependencies");

                return new NodeDependencyAnalysisResult(repositoryInformation, getUniqueName(), nodeDependencies, unusedNodeDependencies);
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
