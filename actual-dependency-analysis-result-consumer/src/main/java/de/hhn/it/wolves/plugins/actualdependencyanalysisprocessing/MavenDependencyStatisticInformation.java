package de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.StatisticInformation;

import org.apache.maven.artifact.Artifact;

import java.util.List;

/**
 * Created by Marvin Rekovsky on 04.07.19.
 *
 * This class represents the implementation of an {@link StatisticInformation} for the framework. It holds the
 * forwarded information of all maven dependencies and unused declared maven dependencies in a java project.
 */
public class MavenDependencyStatisticInformation extends StatisticInformation {


    private final List<Artifact> allForwardedMavenDependencies ;
    private final List<Artifact> unusedForwardedMavenDependencies ;
    private final boolean isMultiModule;

    public MavenDependencyStatisticInformation(AnalysisResult analysisResult, String processedPluginName, List<Artifact> allForwardedMavenDependencies,List<Artifact> unusedForwardedMavenDependencies, boolean isMultiModule) {
        super(analysisResult, processedPluginName);
        this.allForwardedMavenDependencies = allForwardedMavenDependencies;
        this.unusedForwardedMavenDependencies = unusedForwardedMavenDependencies;
        this.isMultiModule = isMultiModule;
    }
    public List<Artifact> getAllForwardedMavenDependencies() {
        return allForwardedMavenDependencies;
    }

    public List<Artifact> getUnusedForwardedMavenDependencies() {
        return unusedForwardedMavenDependencies;
    }

    public boolean isMultiModule() {
        return isMultiModule;
    }
}
