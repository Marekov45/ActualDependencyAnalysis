package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.RepositoryInformation;
import org.apache.maven.artifact.Artifact;

import java.util.List;

/**
 * Created by Marvin Rekovsky on 21.06.19.
 *
 * This class represents the implementation of an {@link AnalysisResult} for the framework. It holds the
 * information of all maven dependencies and unused declared maven dependencies in a java project
 * analysed by the {@link ActualDependencyAnalyserPlugin}.
 */
public class MavenDependencyAnalysisResult extends AnalysisResult {

    private final List<Artifact> allMavenDependencies;
    private final List<Artifact> unusedMavenDependencies;
    private final boolean isMultiModule;


    public MavenDependencyAnalysisResult(RepositoryInformation repositoryInformation, String analysisPluginname, List<Artifact> allMavenDependencies, List<Artifact> unusedMavenDependencies, boolean isMultiModule) {
        super(repositoryInformation, analysisPluginname);
        this.allMavenDependencies = allMavenDependencies;
        this.unusedMavenDependencies = unusedMavenDependencies;
        this.isMultiModule = isMultiModule;
    }

    public List<Artifact> getAllMavenDependencies() {
        return allMavenDependencies;
    }


    public List<Artifact> getUnusedMavenDependencies() {
        return unusedMavenDependencies;
    }

    public boolean isMultiModule() {
        return isMultiModule;
    }
}
