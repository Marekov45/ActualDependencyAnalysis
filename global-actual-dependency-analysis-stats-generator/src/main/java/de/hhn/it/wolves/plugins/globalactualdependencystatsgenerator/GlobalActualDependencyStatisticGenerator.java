package de.hhn.it.wolves.plugins.globalactualdependencystatsgenerator;

import de.hhn.it.wolves.components.TwoPhasesStatisticGeneratorPlugin;
import de.hhn.it.wolves.domain.*;
import de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing.MavenDependencyStatisticInformation;
import de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing.NodeDependencyStatisticInformation;
import org.apache.maven.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalActualDependencyStatisticGenerator implements TwoPhasesStatisticGeneratorPlugin {

    private static final String PLUGIN_NAME = "Global actual dependency statistic generator";
    private static final Logger logger = LoggerFactory.getLogger(GlobalActualDependencyStatisticGenerator.class.getName());

    private final Set<Entry> dependencies = new HashSet<>();
    //  private final Set<NodeEntry> depcheckDependencies = new HashSet<>();
    private int unusedDeps;
    private int noUnusedDeps;

    //startet diese Phase erst wenn alle projekte abgearbeitet wurden?
    //evtl noch überprüfen ob überhaupt unused dependencies vorhanden sind damit die schleife nicht umsonst durchlaufen wird
    @Override
    public void startOperationInPhaseTwo(File file) {
        String separator = ";";
        logger.info("WE FOUND:\n{} projects with unused dependencies\n{} projects without unused dependencies", unusedDeps, noUnusedDeps);
        for (Entry entry : dependencies) {
            String unusedDependencies = "";
            if (entry.getRepositoryInformation().getProgrammingLanguage().equals(ProgrammingLanguage.JAVA)) {
                for (Artifact artifact : entry.getMavenDependencies()) {
                    for (Artifact artifact1 : entry.getUnusedMavenDependencies()) {
                        if (artifact1.equals(artifact)) {
                            unusedDependencies = "\n-" + artifact1.getArtifactId(); //evtl. noch groupId etc. hinzufügen
                        }
                    }
                }
            } else if (entry.getRepositoryInformation().getProgrammingLanguage().equals(ProgrammingLanguage.JAVA_SCRIPT)) {
                for (String str : entry.getDepcheckDependencies()) {
                    String allSplitValues[] = str.split("@");
                    String dependency = allSplitValues[0];
                    for (String str1 : entry.getUnusedDepcheckDependencies()) {
                        String unusedSplitValues[] = str1.split("\\s");
                        if (unusedSplitValues[1].equals(dependency)) {
                            unusedDependencies = "\n-" + unusedSplitValues[1];
                        }
                    }
                }
            }
            logger.info("For " + entry.getRepositoryInformation().getName() + " we found the following unused Dependencies:" + unusedDependencies);
        }
        List<String> csvLines = new ArrayList<>();
        csvLines.add("Name of Repository;Programming language;Dependency count;Unused");
        for (Entry e : dependencies) {
            StringBuilder sb = new StringBuilder(e.getRepositoryInformation().getName());
            sb.append(separator).append(e.getRepositoryInformation().getProgrammingLanguage());
            int unused = 0;
            if (e.getRepositoryInformation().getProgrammingLanguage().equals(ProgrammingLanguage.JAVA)) {
                sb.append(separator).append(e.getMavenDependencies().size());
                if (e.getUnusedMavenDependencies().size() != 0) {
                    unused = e.getUnusedMavenDependencies().size();
                }
            }
            if (e.getRepositoryInformation().getProgrammingLanguage().equals(ProgrammingLanguage.JAVA_SCRIPT)) {
                sb.append(separator).append(e.getDepcheckDependencies().size());
                if (e.getUnusedDepcheckDependencies().size() != 0) {
                    unused = e.getUnusedDepcheckDependencies().size();
                }
            }
            sb.append(separator).append(unused);
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
                    logger.error("We could not close the buffered writer. Maybe data will be lost or not generated!");
                }
        }
    }

    @Override
    public GeneratedStatisticInformation generateStatistic(File file, String s, StatisticInformation statisticInformation) {
        if ((statisticInformation instanceof MavenDependencyStatisticInformation)) {
            dependencies.add(new Entry(statisticInformation.getRepositoryInformation(), ((MavenDependencyStatisticInformation) statisticInformation).getAllForwardedMavenDependencies(), ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies(), null, null));
            for (Artifact artifact : ((MavenDependencyStatisticInformation) statisticInformation).getAllForwardedMavenDependencies()) {
                if (!((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().isEmpty()) {
                    for (Artifact artifact2 : ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies()) {
                        if (artifact2.equals(artifact)) {
                            unusedDeps++;
                            return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
                        }
                    }
                }
            }
            // noUnusedDeps++;
            // return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
        } else if ((statisticInformation instanceof NodeDependencyStatisticInformation)) {
            dependencies.add(new Entry(statisticInformation.getRepositoryInformation(), null, null, ((NodeDependencyStatisticInformation) statisticInformation).getAllForwardedNodeDependencies(), ((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies()));
            for (String str : ((NodeDependencyStatisticInformation) statisticInformation).getAllForwardedNodeDependencies()) {
                String allSplitValues[] = str.split("@");
                String dependency = allSplitValues[0];
                if (!((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies().isEmpty()) {
                    for (String str2 : ((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies()) {
                        String unusedSplitValues[] = str2.split("\\s");
                        if (unusedSplitValues[1].equals(dependency)) {
                            unusedDeps++;
                            return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
                        }
                    }
                }
            }
            //  noUnusedDeps++;
            //  return new GeneratedStatisticInformation(statisticInformation, null, getUniqueName());
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

    private class Entry {
        private final RepositoryInformation repositoryInformation;
        private final List<Artifact> mavenDependencies;
        private final List<Artifact> unusedMavenDependencies;
        private final List<String> depcheckDependencies;
        private final List<String> unusedDepcheckDependencies;

        public Entry(RepositoryInformation repositoryInformation, List<Artifact> mavenDependencies, List<Artifact> unusedMavenDependencies, List<String> depcheckDependencies, List<String> unusedDepcheckDependencies) {
            this.repositoryInformation = repositoryInformation;
            this.mavenDependencies = mavenDependencies;
            this.unusedMavenDependencies = unusedMavenDependencies;
            this.depcheckDependencies = depcheckDependencies;
            this.unusedDepcheckDependencies = unusedDepcheckDependencies;
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
    }

    /** private class NodeEntry {
     private final RepositoryInformation repositoryInformation;
     private final List<String> dependencies;

     public NodeEntry(RepositoryInformation repositoryInformation, List<String> dependencies) {
     this.repositoryInformation = repositoryInformation;
     this.dependencies = dependencies;
     }

     public RepositoryInformation getRepositoryInformation() {
     return repositoryInformation;
     }

     public List<String> getDependencies() {
     return dependencies;
     }
     } **/
}