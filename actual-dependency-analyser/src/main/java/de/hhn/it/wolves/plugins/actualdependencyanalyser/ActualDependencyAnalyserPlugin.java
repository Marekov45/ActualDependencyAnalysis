package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.components.AnalysisPlugin;
import de.hhn.it.wolves.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by Marvin Rekovsky on 11.06.19.
 * <p>
 * This plugin analyses the use of declared dependencies that are unused in a project.
 */
public class ActualDependencyAnalyserPlugin implements AnalysisPlugin {

    private static final String PLUGIN_NAME = "Actual dependency analyser";
    private static final Logger logger = LoggerFactory.getLogger(ActualDependencyAnalyserPlugin.class.getName());
    private static final String POM_FILE = "/pom.xml";
    private static final String JSON_FILE = "/package.json";
    private static int buildFailures = 0; //counts build failures for global stats
    private static int multiModule = 0; //counts multi module projects for global stats
    private static List<String> multiModuleProjects = new ArrayList<>(); //list for all multi module projects to be displayed in global stats
    private static List<Artifact> transformModuletoArtifact = new ArrayList<>();
    private static ArrayList<String> listOfAllModules = new ArrayList<>(); //this list is needed for extracting modules that are used as dependencies and should not count towards stats


    @Override
    public AnalysisResult analyseRepository(RepositoryInformation repositoryInformation) {
        switch (repositoryInformation.getProgrammingLanguage()) {
            case JAVA:


                InvocationRequest request = new DefaultInvocationRequest();
                request.setBatchMode(true);

                File pomFile = new File(repositoryInformation.getLocalDownloadPath() + POM_FILE);

                if (!pomFile.exists()) {
                    logger.error("We could not a find a pom file for" + repositoryInformation.getName() + ", thus it will be excluded from the analysis.");
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                logger.info("Starting Maven process for " + pomFile.getAbsolutePath());
                request.setPomFile(pomFile);

                Model model = null;
                FileReader r = null;
                MavenXpp3Reader mavenreader = new MavenXpp3Reader();
                try {

                    r = new FileReader(pomFile);
                    model = mavenreader.read(r);
                    model.setPomFile(pomFile);

                } catch (XmlPullParserException | IOException e) {
                    logger.error("We could not read the pom.xml for the current project! It will be excluded from the analysis.", e);
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }

                boolean isMultiModule = false;

                MavenProject project = new MavenProject(model);
                Invoker invoker = new DefaultInvoker();
                invoker.setMavenHome(new File("/usr/share/maven"));
                //only list non transitive dependencies
                request.setGoals(Collections.singletonList("-DexcludeTransitive=true dependency:list"));


                ArrayList<String> allMavenDependencies = new ArrayList<>();
                List<Artifact> allArtifacts = new ArrayList<>();
                try {

                    invoker.setOutputHandler(new InvocationOutputHandler() {
                        @Override
                        public void consumeLine(String line) throws IOException {
                            //all maven dependencies have this substring at the start
                            if (line.startsWith("[INFO]    ") && !line.equals("[INFO]    none")) {

                                allMavenDependencies.add(line);
                            }
                            //for multi-module projects, substrings have to be included that identify the module
                            else if (line.contains("[ pom ]") || line.contains("[ jar ]")) {
                                if (!project.getModel().getModules().isEmpty()) {
                                    line = StringUtils.remove(line, '-');
                                    allMavenDependencies.add(line);
                                }
                            } else if (line.startsWith("[ERROR]")) {
                                logger.info("The build failed for the project" + repositoryInformation.getName() + ". It will be excluded from the analysis");
                                allMavenDependencies.add(line);
                            }
                        }
                    });
                    //invoke maven goal from command line
                    invoker.execute(request);
                } catch (MavenInvocationException e) {
                    logger.error("We got an error during the construction of the command line used to invoke Maven! The information" +
                            "for this repository will not be processed any further!", e);
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                //check if list contains an error, if thats the case, remove project from analysis
                for (String element : allMavenDependencies) {
                    if (element.startsWith("[ERROR")) {
                        buildFailures++;
                        return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                    }
                }

                //parent poms will be skipped for multi-module projects
                if (!project.getModel().getModules().isEmpty()) {
                    for (int i = 0; i < allMavenDependencies.size(); i++) {
                        if (allMavenDependencies.get(i).contains("[ jar ]")) {
                            allMavenDependencies.subList(0, i).clear();
                            break; // after the initial parent pom is removed
                        }
                    }
                    for (int j = allMavenDependencies.size() - 1; j >= 0; j--) {
                        //remove every string left that is not a dependency
                        if (allMavenDependencies.get(j).equals("[INFO] [ jar ]") || allMavenDependencies.get(j).equals("[INFO] [ pom ]")) {
                            allMavenDependencies.remove(j);
                        }
                    }
                }

                //change dependency type from string to artifact
                for (int i = 0; i < allMavenDependencies.size(); i++) {
                    allArtifacts.add(buildArtifactFromString(allMavenDependencies, i));
                }


                //only bytecode analysis, cant detect runtime,test,provided dependencies etc. ignore everything non compile
                request.setGoals(Collections.singletonList(" -DignoreNonCompile=true dependency:analyze"));

                ArrayList<String> unusedMavenDependencies = new ArrayList<>();

                List<Artifact> unusedArtifacts = new ArrayList<>();

                List<String> commandLineInfos = new ArrayList<>();

                try {

                    invoker.setOutputHandler(new InvocationOutputHandler() {
                        @Override
                        public void consumeLine(String line) throws IOException {
                            if (line.startsWith("[WARNING] Used undeclared") || line.startsWith("[WARNING] Unused declared")) {
                                logger.info(line);
                                unusedMavenDependencies.add(line);
                                commandLineInfos.add(line);
                                // only bytcode-analysis, classes that are loaded at runtime wont be recognized (jdbc driver etc.)
                                //"spring boot only autoconfigures different things  by adding the corresponding dependency
                                // and this configuration is happening outside of the application code, thus everything related
                                //to springframework will be ignored"
                                //for more see https://stackoverflow.com/questions/37528928/spring-boot-core-dependencies-seen-as-unused-by-maven-dependency-plugin
                            } else if (line.startsWith("[WARNING]    ") && !line.contains("org.springframework") && !line.contains("spring-boot")
                                    && !line.contains("mysql-connector-java") && !line.contains("sqlite-jdbc") && !line.contains("lombok") && !line.contains("guava") && !line.contains("curator")
                                    && !line.contains("springfox") && !line.contains("postgresql") && !line.contains("com.h2database")) {
                                unusedMavenDependencies.add(line);
                            } else if (line.startsWith("[ERROR]")) {
                                logger.info("The build failed for the project" + repositoryInformation.getName() + ". It will be excluded from the analysis");
                                unusedMavenDependencies.add(line);
                                return;
                            }
                            //for multi-module projects
                            else if (!project.getModel().getModules().isEmpty()) {
                                if (line.contains("[INFO] No dependency problems found") || line.startsWith("[INFO] --- maven-dependency-plugin:") || line.contains("[INFO] Skipping pom project")) {
                                    logger.info(line);
                                    commandLineInfos.add(line);
                                }

                            }
                        }
                    });
                    invoker.execute(request);
                } catch (MavenInvocationException e) {
                    logger.error("We got an error during the construction of the command line used to invoke Maven! The information" +
                            "for this repository will not be processed any further!", e);
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }

                for (String element : unusedMavenDependencies) {
                    if (element.startsWith("[ERROR")) {
                        buildFailures++;
                        return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                    }
                }


                //check if project is multi module
                if (!project.getModel().getModules().isEmpty()) {
                    multiModule++;
                    multiModuleProjects.add(repositoryInformation.getName());
                    isMultiModule = true;
                    List<String> completeCMDLogList = new ArrayList<>();
                    for (String commandLineInfo : commandLineInfos) {
                        if (commandLineInfo.contains("@")) {
                            //this string contains the module
                            String module = StringUtils.substringBetween(commandLineInfo, "@", "---").trim();
                            completeCMDLogList.add(module);
                            listOfAllModules.add(module);
                        } else if (commandLineInfo.contains("No dependency problems")) {
                            String noDepProblem = StringUtils.substringBetween(commandLineInfo, "[INFO]", "found");
                            completeCMDLogList.add(noDepProblem);
                        } else if (commandLineInfo.contains("Skipping pom project")) {
                            String skipPom = StringUtils.substringBetween(commandLineInfo, "[INFO]", "project");
                            completeCMDLogList.add(skipPom);
                        } else if (commandLineInfo.contains("[WARNING]")) {
                            String unusedOrUndeclaredDep = StringUtils.substringBetween(commandLineInfo, "[WARNING]", "dependencies");
                            completeCMDLogList.add(unusedOrUndeclaredDep);
                        }
                    }
                    // removes every module from the current multi-module project that has no dependency problems, is packaged as a pom or
                    // only has used undeclared dependencies
                    for (int i = completeCMDLogList.size() - 1; i >= 0; ) {
                        if (completeCMDLogList.get(i).contains("No dependency problems") || completeCMDLogList.get(i).contains("Skipping pom")) {
                            completeCMDLogList.remove(i);
                            completeCMDLogList.remove(i - 1);
                            i -= 2;
                        } else if (completeCMDLogList.get(i).contains("Unused declared")) {
                            completeCMDLogList.remove(i);
                            if (completeCMDLogList.get(i - 1).contains("Used undeclared")) {
                                completeCMDLogList.remove(i - 1);
                                i -= 2;
                            } else {
                                i -= 1;
                            }
                        } else if (completeCMDLogList.get(i).contains("Used undeclared")) {
                            completeCMDLogList.remove(i);
                            completeCMDLogList.remove(i - 1);
                            i -= 2;
                        } else {
                            i -= 1;
                        }
                    }

                    // transforms every module from current multi-module project into a dummy Artifact for easier processing
                    for (int i = 0; i < completeCMDLogList.size(); i++) {
                        transformModuletoArtifact.add(i, new DefaultArtifact("org.dummy", completeCMDLogList.get(i), "No version", null, "jar", null, new DefaultArtifactHandler()));
                    }


                }

                String mavenText = "[WARNING] Unused declared dependencies found:";
                boolean unusedPattern = false;
                int moduleCounter = 0;
                // check if repo has unused dependencies
                if (unusedMavenDependencies.contains(mavenText)) {
                    for (int i = getUnusedDependencyIndex(unusedMavenDependencies, mavenText); i < unusedMavenDependencies.size(); i++) {
                        if (unusedMavenDependencies.get(i).contains("[WARNING] Unused declared dependencies found:")) {
                            unusedPattern = true;
                            //match modules with unused dependencies for multi-module projects
                            if (!project.getModel().getModules().isEmpty()) {
                                unusedArtifacts.add(transformModuletoArtifact.get(moduleCounter));
                                moduleCounter++;
                            }
                        }
                        if (unusedMavenDependencies.get(i).contains("[WARNING] Used undeclared dependencies found:")) {
                            unusedPattern = false;
                        }
                        if (unusedPattern && !unusedMavenDependencies.get(i).contains("[WARNING] Unused declared dependencies found:")) {

                            unusedArtifacts.add(buildArtifactFromString(unusedMavenDependencies, i));

                        }
                    }
                }
                return new MavenDependencyAnalysisResult(repositoryInformation, getUniqueName(), allArtifacts, unusedArtifacts, isMultiModule);

            case JAVA_SCRIPT:
                File packageJSONFile = new File(repositoryInformation.getLocalDownloadPath() + JSON_FILE);
                if (!packageJSONFile.exists()) {
                    logger.error("We could not a find a package.json file for " + repositoryInformation.getName() + ", thus it will be excluded from the analysis.");
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }


                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "npm install");
                pb.directory(repositoryInformation.getLocalDownloadPath());
                pb.redirectErrorStream(true);
                //  Process installProcess = null;
                Process process = null;
                try {
                    process = pb.start();

                } catch (IOException e) {

                    e.printStackTrace();
                }
                BufferedReader installReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String installOutput;
                while (true) {
                    try {
                        if (!((installOutput = installReader.readLine()) != null)) break;
                        logger.info("npm install output :" + installOutput);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //list all non transitive dependencies
                pb.command("/bin/bash", "-c", "npm list -dev -prod -depth 0");

                // Process process = null;
                try {
                    process = pb.start();
                } catch (IOException e) {
                    logger.error("We could not start the current process! Either the command is not a valid system operating command or" +
                            "the working directory does not exist.", e);
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                BufferedReader input = new BufferedReader((new InputStreamReader(process.getInputStream())));
                ArrayList<String> allNodeDependencies = new ArrayList<>();
                String line = null;
                try {
                    input.readLine();  // read first line so analysis starts at line 2
                } catch (IOException e) {
                    logger.error("We could not read the input from the current process! The project will not be analysed.", e);
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                while (true) {
                    try {

                        line = input.readLine();
                        logger.info("npm run information:" + line);

                        if (line == null) {
                            break;
                            //all dependencies have these chars
                        } else if (line.contains("@") && line.charAt(1) == '─') {
                            allNodeDependencies.add(line);
                        }
                    } catch (IOException e) {
                        logger.error("We could not read the input from the current process! The project will not be analysed.", e);
                        return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                    }

                }
                ArrayList<String> nodeDependencies = new ArrayList<>();
                int index = 0;
                for (int i = allNodeDependencies.size() - 1; i >= 0; i--) {
                    nodeDependencies.add(getNameOfDependency(allNodeDependencies, i));
                    //if dependencies have an '@' at the start of their name, remove it
                    if (nodeDependencies.get(index).startsWith("@")) {
                        String s = nodeDependencies.get(index).substring(1);
                        nodeDependencies.remove(index);
                        nodeDependencies.add(s);
                    }
                    index++;
                }
                //the command for getting all unused dependencies
                pb.command("depcheck");
                Process depcheckProcess = null;
                try {
                    depcheckProcess = pb.start();
                } catch (IOException e) {
                    logger.error("We could not start the current process! The command is most likely not a valid system operating command.", e);
                    return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(depcheckProcess.getInputStream()));

                String row = null;
                ArrayList<String> depcheckDependencies = new ArrayList<>();
                while (true) {
                    try {

                        if (!((row = reader.readLine()) != null)) break;
                        logger.info(row);
                    } catch (IOException e) {
                        logger.error("We could not read the input from the current process! The project will not be analysed.", e);
                        return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
                    }
                    depcheckDependencies.add(row);
                }
                ArrayList<String> unusedNodeDependencies = new ArrayList<>();
                String depcheckText = "Missing dependencies";
                if (depcheckDependencies.contains(depcheckText)) {
                    int unusedindex = 0;
                    for (int i = getUnusedDependencyIndex(depcheckDependencies, depcheckText) - 1; i >= 0; i--) {
                        unusedNodeDependencies.add(depcheckDependencies.get(i));
                        if (unusedNodeDependencies.get(unusedindex).startsWith("* @")) {
                            String s = unusedNodeDependencies.get(unusedindex).substring(3);
                            unusedNodeDependencies.remove(unusedindex);
                            unusedNodeDependencies.add(s);
                        }
                        unusedindex++;
                    }
                } else {
                    int unusedIndexNoMissingDeps = 0;
                    for (int j = depcheckDependencies.size() - 1; j >= 0; j--) {
                        unusedNodeDependencies.add(depcheckDependencies.get(j));
                        if (unusedNodeDependencies.get(unusedIndexNoMissingDeps).startsWith("* @")) {
                            String s = unusedNodeDependencies.get(unusedIndexNoMissingDeps).substring(3);
                            unusedNodeDependencies.remove(unusedIndexNoMissingDeps);
                            unusedNodeDependencies.add(s);
                        }
                        unusedIndexNoMissingDeps++;
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
                return new NodeDependencyAnalysisResult(repositoryInformation, getUniqueName(), nodeDependencies, unusedNodeDependencies, false);
        }

        logger.error("This part should never been reached");
        return new AnalysisResultWithoutProcessing(repositoryInformation, getUniqueName());
    }

    /**
     * Returns the dependency substring from the command line string.
     *
     * @param allDependencies the list that contains all the command lines including the dependency.
     *                        <b>Must not be <code>null</code></b>.
     * @param i               the index of the element in the list.
     * @return the dependency of the project for the current line. <b>Must not be <code>null</code></b>.
     */
    private String getNameOfDependency(ArrayList<String> allDependencies, int i) {
        String dependency;
        String line = allDependencies.get(i);
        //split the command line string at every whitespace and find the dependency string
        String[] splitValues = line.split("\\s");
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
            dependency = splitValues[3];
        }
        return dependency;
    }

    /**
     * Returns the index of the starting point for extracting unused dependencies.
     *
     * @param output        the list that contains the unused dependencies along other possible stuff like missing dependencies.
     *                      <b>Must not be <code>null</code></b>.
     * @param startingPoint the string to be searched for in the list.<b>Must not be <code>null</code> or empty</b>.
     * @return the index of the starting point for extracting unused dependencies.
     */
    private int getUnusedDependencyIndex(ArrayList<String> output, String startingPoint) {
        int index = 0;
        int counter = 0;
        for (String start : output) {
            if (start.equals(startingPoint)) {
                index = counter;
                break;
            }
            counter++;
        }
        return index;
    }

    /**
     * Transforms the dependency of the type String into an Artifact and returns it.
     *
     * @param pluginOutput          the list that contains all dependencies.
     * @param unusedDependencyIndex the current index of the dependency to be transformed.
     * @return the dependency that now has the reference type Artifact.
     */
    private Artifact buildArtifactFromString(ArrayList<String> pluginOutput, int unusedDependencyIndex) {
        String line = pluginOutput.get(unusedDependencyIndex);
        String[] splitValues = line.split(":|\\s+");
        String groupId = splitValues[1];
        String artifactId = splitValues[2];
        String type = splitValues[3];
        String version = splitValues[4];
        String scope = splitValues[5];
        return new DefaultArtifact(groupId, artifactId, version, scope, type, null, new DefaultArtifactHandler());

    }

    public static int getBuildFailures() {
        return buildFailures;
    }

    public static int getMultiModule() {
        return multiModule;
    }

    public static List<String> getMultiModuleProjects() {
        return multiModuleProjects;
    }

    public static List<Artifact> getTransformedModules() {
        return transformModuletoArtifact;
    }

    public static List<String> getListOfAllModules() {
        return listOfAllModules;
    }

    @Override
    public ProgrammingLanguage[] getLanguageThisPluginIsDesignedFor() {
        return new ProgrammingLanguage[]{ProgrammingLanguage.JAVA, ProgrammingLanguage.JAVA_SCRIPT};
    }

    @Override
    public void init(FrameworkManager frameworkManager) {

    }

    @Override
    public String getUniqueName() {
        return PLUGIN_NAME;
    }

    @Override
    public void cleanUp() {

    }


}
