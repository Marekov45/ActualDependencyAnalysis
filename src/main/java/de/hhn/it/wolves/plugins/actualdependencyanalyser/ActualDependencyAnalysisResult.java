package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.RepositoryInformation;
import org.apache.maven.artifact.Artifact;

import java.util.List;

public class ActualDependencyAnalysisResult extends AnalysisResult {

    private final List<Artifact> unusedDependencies;

    public ActualDependencyAnalysisResult(RepositoryInformation repositoryInformation, String analysisPluginname, List<Artifact> unusedDependencies) {
        super(repositoryInformation, analysisPluginname);
        this.unusedDependencies = unusedDependencies;
    }
    public List<Artifact> getUnusedDependencies() {
        return unusedDependencies;
    }
}
