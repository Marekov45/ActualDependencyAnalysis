package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.RepositoryInformation;
import org.apache.maven.artifact.Artifact;

import java.util.List;

public class NodeDependencyAnalysisResult extends AnalysisResult {

    private final List<String> NodeDependencies;


    public NodeDependencyAnalysisResult(RepositoryInformation repositoryInformation, String analysisPluginname, List<String> NodeDependencies) {
        super(repositoryInformation, analysisPluginname);
        this.NodeDependencies = NodeDependencies;
    }

    public List<String> getNodeDependencies() {
        return NodeDependencies;
    }
}
