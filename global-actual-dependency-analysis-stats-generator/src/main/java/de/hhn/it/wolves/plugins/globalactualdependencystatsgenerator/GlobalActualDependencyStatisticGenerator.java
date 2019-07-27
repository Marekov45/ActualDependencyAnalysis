package de.hhn.it.wolves.plugins.globalactualdependencystatsgenerator;

import de.hhn.it.wolves.components.TwoPhasesStatisticGeneratorPlugin;
import de.hhn.it.wolves.domain.*;
import de.hhn.it.wolves.plugins.actualdependencyanalyser.ActualDependencyAnalyserPlugin;
import de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing.MavenDependencyStatisticInformation;
import de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing.NodeDependencyStatisticInformation;
import org.apache.maven.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by Marvin Rekovsky on 07.07.19.
 * <p>
 * This plugin gathers data about all repositories regarding the use of unused declared dependencies. At the end it generates
 * a global statistic over all the analysed repositories.
 */
public class GlobalActualDependencyStatisticGenerator implements TwoPhasesStatisticGeneratorPlugin {

    private static final String PLUGIN_NAME = "Global actual dependency statistic generator";
    private static final Logger logger = LoggerFactory.getLogger(GlobalActualDependencyStatisticGenerator.class.getName());
    private final Set<Repo> repositories = new HashSet<>();
    private int unusedDeps;
    private int noUnusedDeps;


    @Override
    public void startOperationInPhaseTwo(File file) {
        String separator = ";";
        String allMultiModuleProjects = "";
        logger.info("WE FOUND:\n{} projects with unused dependencies\n{} projects without unused dependencies", unusedDeps, noUnusedDeps);
        logger.info(ActualDependencyAnalyserPlugin.getBuildFailures() + " projects had a build failure and were excluded from the analysis.");
        for (int i = 0; i < ActualDependencyAnalyserPlugin.getMultiModuleProjects().size(); i++) {
            allMultiModuleProjects += "\n-" + ActualDependencyAnalyserPlugin.getMultiModuleProjects().get(i);
        }
        logger.info("We found " + ActualDependencyAnalyserPlugin.getMultiModule() + " of the following projects that have multiple modules: " + allMultiModuleProjects);

        for (Repo repository : repositories) {
            String unusedDependencies = "";
            if (repository.getRepositoryInformation().getProgrammingLanguage().equals(ProgrammingLanguage.JAVA)) {
                //get rid of duplicated Dependencies only for global stats
                Set<Artifact> noDuplicateAllArtifacts = new LinkedHashSet<>(repository.getMavenDependencies());
                // get rid of duplicated unused Dependencies only for global stats
                Set<Artifact> noDuplicateUnusedArtifacts = new LinkedHashSet<>(repository.getUnusedMavenDependencies());
                for (Artifact artifact : noDuplicateAllArtifacts) {
                    for (Artifact artifact1 : noDuplicateUnusedArtifacts) {
                        if (artifact1.equals(artifact)) {
                            unusedDependencies += "\n-" + artifact1.getArtifactId();
                        }
                    }
                }
            } else if (repository.getRepositoryInformation().getProgrammingLanguage().equals(ProgrammingLanguage.JAVA_SCRIPT)) {
                for (String str : repository.getDepcheckDependencies()) {
                    String[] allSplitValues = str.split("@");
                    String dependency = allSplitValues[0];
                    for (String str1 : repository.getUnusedDepcheckDependencies()) {
                        if (!str1.contains("*")) {
                            if (str1.equals(dependency)) {
                                unusedDependencies += "\n-" + str1;
                            }
                        } else {
                            String[] unusedSplitValues = str1.split("\\s");
                            if (unusedSplitValues[1].equals(dependency)) {
                                unusedDependencies += "\n-" + unusedSplitValues[1];
                            }
                        }
                    }

                }
            }
            logger.info("For " + repository.getRepositoryInformation().getName() + " we found the following unused Dependencies:" + unusedDependencies);
        }
        List<String> csvLines = new ArrayList<>();
        csvLines.add("Name of Repository;Programming language;Dependencies count;Unused;Multi-Module");
        for (Repo repository : repositories) {
            StringBuilder sb = new StringBuilder(repository.getRepositoryInformation().getName());
            sb.append(separator).append(repository.getRepositoryInformation().getProgrammingLanguage());
            int unused = 0;
            if (repository.getRepositoryInformation().getProgrammingLanguage().equals(ProgrammingLanguage.JAVA)) {
                //get rid of duplicated Dependencies only for global stats
                Set<Artifact> noDuplicateAllArtifacts = new LinkedHashSet<>(repository.getMavenDependencies());

                // get rid of duplicated unused Dependencies only for global stats
                Set<Artifact> noDuplicateUnusedArtifacts = new LinkedHashSet<>(repository.getUnusedMavenDependencies());
                sb.append(separator).append(noDuplicateAllArtifacts.size());
                if (repository.getUnusedMavenDependencies().size() != 0) {
                    unused = noDuplicateUnusedArtifacts.size();
                }
            }
            if (repository.getRepositoryInformation().getProgrammingLanguage().equals(ProgrammingLanguage.JAVA_SCRIPT)) {
                sb.append(separator).append(repository.getDepcheckDependencies().size());
                if (repository.getUnusedDepcheckDependencies().size() != 0 && !repository.getUnusedDepcheckDependencies().get(0).equals("No depcheck issue")) {
                    unused = repository.getUnusedDepcheckDependencies().size();
                }
            }
            sb.append(separator).append(unused);
            if (repository.isMultiModule()) {
                sb.append(separator).append("X");
            }
            csvLines.add(sb.toString());
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file.getAbsolutePath() + "/globalreport.csv"));
            for (String s : csvLines) {
                writer.write(s);
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("We could not write the global report!", e);
        } finally {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("We could not close the buffered writer. Maybe data will be lost or not generated!",e);
                }
        }
    }

    @Override
    public GeneratedStatisticInformation generateStatistic(File file, String s, StatisticInformation statisticInformation) {
        if ((statisticInformation instanceof MavenDependencyStatisticInformation)) {
            repositories.add(new Repo(statisticInformation.getRepositoryInformation(), ((MavenDependencyStatisticInformation) statisticInformation).getAllForwardedMavenDependencies(), ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies(), null, null, ((MavenDependencyStatisticInformation) statisticInformation).isMultiModule()));
            for (Artifact dependency : ((MavenDependencyStatisticInformation) statisticInformation).getAllForwardedMavenDependencies()) {
                if (!((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().isEmpty()) {
                    for (Artifact unusedDependency : ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies()) {
                        //count projects with unused declared dependencies and excludes dependencies that are a module of a multi-module project
                        if (unusedDependency.equals(dependency)) {
                            unusedDeps++;
                            return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
                        }
                    }
                }
            }
        } else if ((statisticInformation instanceof NodeDependencyStatisticInformation)) {
            repositories.add(new Repo(statisticInformation.getRepositoryInformation(), null, null, ((NodeDependencyStatisticInformation) statisticInformation).getAllForwardedNodeDependencies(), ((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies(), false));
            for (String str : ((NodeDependencyStatisticInformation) statisticInformation).getAllForwardedNodeDependencies()) {
                String[] allSplitValues = str.split("@");
                String dependency = allSplitValues[0];
                if (!((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies().isEmpty() && !((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies().get(0).equals("No depcheck issue")) {
                    for (String unusedNodeDependency : ((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies()) {
                        if (!unusedNodeDependency.contains("*")) {
                            if (unusedNodeDependency.equals(dependency)) {
                                unusedDeps++;
                                return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
                            }
                        } else {
                            String[] unusedSplitValues = unusedNodeDependency.split("\\s");
                            if (unusedSplitValues[1].equals(dependency)) {
                                unusedDeps++;
                                return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
                            }
                        }
                    }
                }
            }
        } else {
            logger.error("I got an statistic information that I am not designed for");
            return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
        }
        noUnusedDeps++;
        return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
    }

    @Override
    public Class[] getStatisticInformationsThisPluginIsDesignedFor() {
        return new Class[]{MavenDependencyStatisticInformation.class, NodeDependencyStatisticInformation.class};
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

    /**
     * This nested class represents a repository with all of its information including used and unused declared dependencies.
     */
    private class Repo {
        private final RepositoryInformation repositoryInformation;
        private final List<Artifact> mavenDependencies;
        private final List<Artifact> unusedMavenDependencies;
        private final List<String> depcheckDependencies;
        private final List<String> unusedDepcheckDependencies;
        private final boolean isMultiModule;

        public Repo(RepositoryInformation repositoryInformation, List<Artifact> mavenDependencies, List<Artifact> unusedMavenDependencies, List<String> depcheckDependencies, List<String> unusedDepcheckDependencies, boolean isMultiModule) {
            this.repositoryInformation = repositoryInformation;
            this.mavenDependencies = mavenDependencies;
            this.unusedMavenDependencies = unusedMavenDependencies;
            this.depcheckDependencies = depcheckDependencies;
            this.unusedDepcheckDependencies = unusedDepcheckDependencies;
            this.isMultiModule = isMultiModule;
        }

        public RepositoryInformation getRepositoryInformation() {
            return repositoryInformation;
        }

        public List<Artifact> getMavenDependencies() {
            return mavenDependencies;
        }

        public List<String> getDepcheckDependencies() {
            return depcheckDependencies;
        }

        public List<Artifact> getUnusedMavenDependencies() {
            return unusedMavenDependencies;
        }

        public List<String> getUnusedDepcheckDependencies() {
            return unusedDepcheckDependencies;
        }

        public boolean isMultiModule() {
            return isMultiModule;
        }
    }

}
