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
import java.util.*;

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
                request.setBatchMode(true);

                File pomFile = new File(repositoryInformation.getLocalDownloadPath() + POM_FILE);
                if (!pomFile.exists()) {
                    logger.info("We could not a find a pom file for" + repositoryInformation.getName() + ", thus it will be excluded from the analysis.");
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                logger.info("Starting Maven process for " + pomFile.getAbsolutePath());
                request.setPomFile(pomFile);
                //  request.setGoals(Collections.singletonList("dependency:analyze"));
                request.setGoals(Collections.singletonList("dependency:list"));

                Invoker invoker = new DefaultInvoker();
                invoker.setMavenHome(new File("/usr/share/maven"));
                ArrayList<String> allMavenDependencies = new ArrayList<>();
                List<Artifact> allArtifacts = new ArrayList<>();
                try {

                    invoker.setOutputHandler(new InvocationOutputHandler() {
                        @Override
                        public void consumeLine(String line) throws IOException {
                            //       if (line.startsWith("[WARNING] Unused declared") || line.startsWith("[WARNING]    ")) {
                            logger.info(line);
                            if (line.startsWith("[INFO]    ") && !line.equals("[INFO]    none")) {

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

                request.setGoals(Collections.singletonList("dependency:analyze"));
                ArrayList<String> unusedMavenDependencies = new ArrayList<>();

                List<Artifact> unusedArtifacts = new ArrayList<>();
                try {

                    invoker.setOutputHandler(new InvocationOutputHandler() {
                        @Override
                        public void consumeLine(String line) throws IOException {
                            if (line.startsWith("[WARNING] Unused declared") || line.startsWith("[WARNING]    ")) {
                                logger.info(line);
                                unusedMavenDependencies.add(line);
                            }
                        }
                    });
                    invoker.execute(request);
                } catch (MavenInvocationException e) {
                    e.printStackTrace();
                }
                // ArrayList<String> unusedMavenList = new ArrayList<>();
                // unusedMavenList.addAll(unusedMavenDependencies);
                String mavenText = "[WARNING] Unused declared dependencies found:";

                // check if repo has unused dependencies
                if (unusedMavenDependencies.contains(mavenText)) {
                    for (int i = getUnusedDependencyIndex(unusedMavenDependencies, mavenText) + 1; i < unusedMavenDependencies.size(); i++) {
                        //   if(!unusedMavenDependencies.get(i).contains("Unused declared")) {
                        unusedArtifacts.add(buildArtifactFromString(unusedMavenDependencies, i));
                        //     }
                    }
                }
                return new MavenDependencyAnalysisResult(repositoryInformation, getUniqueName(), allArtifacts, unusedArtifacts);

            case JAVA_SCRIPT:
                File packageJSONFile = new File(repositoryInformation.getLocalDownloadPath() + JSON_FILE);
                if (!packageJSONFile.exists()) {
                    logger.info("We could not a find a package.json file for " + repositoryInformation.getName() + ", thus it will be excluded from the analysis.");
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }

                // ProcessBuilder pb = new ProcessBuilder("npm","list","-depth 0");

                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "npm list -dev -prod -depth 0");
                pb.directory(repositoryInformation.getLocalDownloadPath());
                pb.redirectErrorStream(true);
                Process p3 = null;
                try {
                    p3 = pb.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedReader input = new BufferedReader((new InputStreamReader(p3.getInputStream())));
                ArrayList<String> allNodeDependencies = new ArrayList<>();
                String line = null;
                try {
                    input.readLine();  // read first line so analysis starts at line 2
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (true) {
                    try {

                        //if (!((line = input.readLine()) != null)) break;
                        line = input.readLine();
                        logger.info("npm run information:" + line);

                        if (line == null) {
                            break;
// Programm erreicht nie die else if Bedingung??? Darstellung dependency structure windows: +--
                        } else if (line.contains("@") && line.charAt(1) == '─') {
                            allNodeDependencies.add(line);
                            // logger.info("Inhalt Liste aller Dependencies: " + allNodeDependencies.toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                ArrayList<String> nodeDependencies = new ArrayList<>();
                for (int i = 0; i < allNodeDependencies.size(); i++) {
                    nodeDependencies.add(getNameOfDependency(allNodeDependencies, i));
                    if (nodeDependencies.get(i).startsWith("@")) {
                        String s = nodeDependencies.get(i).substring(1);
                        nodeDependencies.remove(i);
                        nodeDependencies.add(s);
                    }
                }
                //the command for getting all unused dependencies
                pb.command("depcheck");
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
                        logger.info(row);
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
                        if (unusedNodeDependencies.get(i).startsWith("* @")) {
                            String s = unusedNodeDependencies.get(i).substring(3);
                            unusedNodeDependencies.remove(i);
                            unusedNodeDependencies.add(s);
                        }
                    }
                } else {
                    for (int j = 0; j < depcheckDependencies.size(); j++) {
                        unusedNodeDependencies.add(depcheckDependencies.get(j));
                        if (unusedNodeDependencies.get(j).startsWith("* @")) {
                            String s = unusedNodeDependencies.get(j).substring(3);
                            unusedNodeDependencies.remove(j);
                            unusedNodeDependencies.add(s);
                        }
                    }
                }
                //remove Error messages in List
                for (int i = nodeDependencies.size() - 1; i >= 0; i--) {
                    String errorMessage = nodeDependencies.get(i);
                    if (errorMessage.contains("ERR")) {
                        nodeDependencies.remove(i);
                    }
                }
                //if it doesnt contain the element it remains unchanged
                unusedNodeDependencies.remove("Unused dependencies");
                unusedNodeDependencies.remove("Unused devDependencies");

                logger.info("Alle Dependencies:" + nodeDependencies);
                logger.info("Unused Dependencies:" + unusedNodeDependencies);
                return new NodeDependencyAnalysisResult(repositoryInformation, getUniqueName(), nodeDependencies, unusedNodeDependencies);
        }

        logger.error("This part should never been reached");
        return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
    }

    private String getNameOfDependency(ArrayList<String> allDependencies, int i) {
        String dependency;
        String line = allDependencies.get(i);
        String splitValues[] = line.split("\\s");
        if (line.contains("OPTIONAL") || line.contains("PEER")) {
            return dependency = splitValues[4];
        }
        if (!splitValues[1].equals("UNMET")) {
            dependency = splitValues[1];
            return dependency;
        }
        if (splitValues[3].equals("├──") || splitValues[3].equals("└──")) {
            dependency = splitValues[6];
        } else {
            // if (line.contains("OPTIONAL") || line.contains("PEER")) {
            //     dependency = splitValues[4];
            // } else {
            //     dependency = splitValues[3];
            // }
            dependency = splitValues[3];
        }
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
        String artifactId = splitValues[2];// ganz selten out of bounds?
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
