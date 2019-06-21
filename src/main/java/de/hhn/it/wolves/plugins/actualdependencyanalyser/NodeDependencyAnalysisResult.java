package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.RepositoryInformation;
import org.apache.maven.artifact.Artifact;

import java.util.List;

public class NodeDependencyAnalysisResult extends AnalysisResult {

    private final List<String> unusedNodeDependencies;


    public NodeDependencyAnalysisResult(RepositoryInformation repositoryInformation, String analysisPluginname, List<String> unusedNodeDependencies) {
        super(repositoryInformation, analysisPluginname);
        this.unusedNodeDependencies = unusedNodeDependencies;
    }

    public List<String> getUnusedNodeDependencies() {
        return unusedNodeDependencies;
    }
}
