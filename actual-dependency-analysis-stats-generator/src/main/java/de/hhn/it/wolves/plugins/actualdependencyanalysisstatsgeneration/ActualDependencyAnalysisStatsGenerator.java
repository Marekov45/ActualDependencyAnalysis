package de.hhn.it.wolves.plugins.actualdependencyanalysisstatsgeneration;

import de.hhn.it.wolves.components.StatisticGeneratorPlugin;
import de.hhn.it.wolves.domain.FrameworkManager;
import de.hhn.it.wolves.domain.GeneratedStatisticInformation;
import de.hhn.it.wolves.domain.StatisticInformation;
import de.hhn.it.wolves.plugins.actualdependencyanalyser.ActualDependencyAnalyserPlugin;
import de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing.MavenDependencyStatisticInformation;
import de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing.NodeDependencyStatisticInformation;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by Marvin Rekovsky on 04.07.19.
 * <p>
 * This plugin generates the statistics for each single repository regarding the use of unused declared dependencies.
 */
public class ActualDependencyAnalysisStatsGenerator implements StatisticGeneratorPlugin {

    private static final String PLUGIN_NAME = "Actual dependency statistic generator";
    private static final Logger logger = LoggerFactory.getLogger(ActualDependencyAnalysisStatsGenerator.class.getName());


    @Override
    public GeneratedStatisticInformation generateStatistic(File file, String s, StatisticInformation statisticInformation) {
        String separator = ";";
        List<String> lines = new ArrayList<>();
        lines.add("Name der Bibliothek;Version;Module;Unbenutzt");
        boolean foundUnused = false;
        if ((statisticInformation instanceof MavenDependencyStatisticInformation)) {
            if (!((MavenDependencyStatisticInformation) statisticInformation).isMultiModule()) {
                for (Artifact artifact : ((MavenDependencyStatisticInformation) statisticInformation).getAllForwardedMavenDependencies()) {
                    StringBuilder sb = new StringBuilder(artifact.getArtifactId());
                    sb.append(separator).append(artifact.getVersion());
                    sb.append(separator).append(" ");
                    // check if the repo has any unused dependencies
                    if (!((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().isEmpty()) {
                        for (Artifact artifact2 : ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies()) {
                            if (artifact2.equals(artifact)) {
                                sb.append(separator).append("X");
                            }
                        }
                        foundUnused = true;
                    }
                    lines.add(sb.toString());
                }
            } else if (((MavenDependencyStatisticInformation) statisticInformation).isMultiModule()) {
                String moduleName = " ";
                //create copied list of unused dependencies so elements can be safely removed without changing the original list for global stats later
                List<Artifact> unusedDepsCopy = new ArrayList<>();
                for (int i = 0; i < ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().size(); i++) {
                    unusedDepsCopy.add(((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().get(i));
                }
                for (Artifact artifact : ((MavenDependencyStatisticInformation) statisticInformation).getAllForwardedMavenDependencies()) {
                    StringBuilder builder = new StringBuilder(artifact.getArtifactId());
                    builder.append(separator).append(artifact.getVersion());
                    if (((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().isEmpty()) {
                        builder.append(separator).append(" ");
                    } else {
                        //this part checks in which module the unused declared dependency is used in a multi-module project
                        for (Iterator<Artifact> iterator = unusedDepsCopy.iterator(); iterator.hasNext(); ) {
                            Artifact module = iterator.next();
                            if (module.getVersion().equals("No version") && !moduleName.equals(module.getArtifactId())) {
                                moduleName = module.getArtifactId();
                            } else {
                                if (module.equals(artifact)) {
                                    builder.append(separator).append(moduleName);
                                    builder.append(separator).append("X");
                                    iterator.remove();
                                    break;
                                }
                            }
                        }
                        foundUnused = true;
                    }
                    lines.add(builder.toString());
                }
                //get rid of modules in unused dependencies list to have correct global stats
                for (int i = 0; i < ActualDependencyAnalyserPlugin.getTransformedModules().size(); i++) {
                    for (int j = ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().size() - 1; j >= 0; j--) {
                        if (((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().get(j).equals(ActualDependencyAnalyserPlugin.getTransformedModules().get(i))) {
                            ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().remove(j);
                        }
                    }
                }
                //if a module of a multi module project is a dependency for a different module, the plugin might tag it as unused declared
                // dependency even though it could be used. Additionally the module will never be found in the CVE database for further analysis
                // since it is only a module of the current project. It will be excluded in the single repository stats.
                for (int i = lines.size() - 1; i > 0; i--) {
                    String[] dependencyName = lines.get(i).split(";");
                    logger.info("Artifact ID of the module:" + dependencyName[0]);
                    logger.info("What modules are used?: " + ActualDependencyAnalyserPlugin.getTransformedModules());
                    for (String module : ActualDependencyAnalyserPlugin.getListOfAllModules()) {
                        if (dependencyName[0].equals(module) && Arrays.asList(dependencyName).contains("X")) {
                            logger.info("Line that will be removed: " + lines.get(i));
                            for (int j = ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().size() - 1; j >= 0; j--) {
                                if (((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().get(j).getArtifactId().contains(dependencyName[0])) {
                                    ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies().remove(j);
                                }
                            }

                            lines.remove(i);
                        }
                    }

                }
                logger.info("List of all unused Dependencies: " + ((MavenDependencyStatisticInformation) statisticInformation).getUnusedForwardedMavenDependencies());

            }
        } else if ((statisticInformation instanceof NodeDependencyStatisticInformation)) {
            for (String str : ((NodeDependencyStatisticInformation) statisticInformation).getAllForwardedNodeDependencies()) {
                logger.info("Following line incoming " + str);
                int count = StringUtils.countMatches(str, "@");
                String[] allSplitValues = str.split("@");
                String dependency;
                String version;
                if (count == 2) {
                    dependency = "@" + allSplitValues[1];
                    version = allSplitValues[2];
                } else {
                    dependency = allSplitValues[0];
                    version = allSplitValues[1];
                }
                //  String dependency = allSplitValues[0];
                //  String version = allSplitValues[1];
                StringBuilder sb = new StringBuilder();
                sb.append(dependency);
                sb.append(separator).append(version);
                sb.append(separator).append(" "); //JavaScript has no Multi-Module mechanism
                if (!((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies().isEmpty() && !((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies().get(0).equals("No depcheck issue")) {
                    for (String unusedDependency : ((NodeDependencyStatisticInformation) statisticInformation).getUnusedForwardedNodeDependencies()) {
                        logger.info("Apparently unused : " + unusedDependency);
                        String[] unusedSplitValues = unusedDependency.split("\\s");
                        if (unusedSplitValues[1].equals(dependency)) {
                            logger.info("Dependencies match! " + unusedSplitValues[1] + " is unused");
                            sb.append(separator).append("X");
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
            if (foundUnused) {
                writer = new BufferedWriter(new FileWriter(file.getAbsolutePath() + "/[UNUSED] " + s + ".csv"));
            } else {
                writer = new BufferedWriter(new FileWriter(file.getAbsolutePath() + "/" + s + ".csv"));
            }


            //sort alphabetically for better overview
            Collections.sort(lines.subList(1, lines.size()));
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

    }

    @Override
    public String getUniqueName() {
        return PLUGIN_NAME;
    }

    @Override
    public void cleanUp() {

    }


}
