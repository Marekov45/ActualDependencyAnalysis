package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.RepositoryInformation;
import org.apache.maven.artifact.Artifact;

import java.util.List;

public class NodeDependencyAnalysisResult extends AnalysisResult {

    private final List<String> allNodeDependencies;
    private final List<String> unusedNodeDependencies;
    private boolean isMultiModule;


    public NodeDependencyAnalysisResult(RepositoryInformation repositoryInformation, String analysisPluginname, List<String> allNodeDependencies, List<String> unusedNodeDependencies,boolean isMultiModule) {
        super(repositoryInformation, analysisPluginname);
        this.allNodeDependencies = allNodeDependencies;
        this.unusedNodeDependencies = unusedNodeDependencies;
        this.isMultiModule = isMultiModule;
    }

    public List<String> getAllNodeDependencies() {
        return allNodeDependencies;
    }

    public List<String> getUnusedNodeDependencies() {
        return unusedNodeDependencies;
    }

    public boolean isMultiModule() {
        return isMultiModule;
    }
}
