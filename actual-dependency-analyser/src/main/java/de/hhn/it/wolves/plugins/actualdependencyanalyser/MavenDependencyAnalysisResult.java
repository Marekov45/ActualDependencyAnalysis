package de.hhn.it.wolves.plugins.actualdependencyanalyser;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.RepositoryInformation;
import org.apache.maven.artifact.Artifact;

import java.util.List;

public class MavenDependencyAnalysisResult extends AnalysisResult {

    private final List<Artifact> MavenDependencies;


    public MavenDependencyAnalysisResult(RepositoryInformation repositoryInformation, String analysisPluginname, List<Artifact> MavenDependencies) {
        super(repositoryInformation, analysisPluginname);
        this.MavenDependencies = MavenDependencies;
    }

    public List<Artifact> getMavenDependencies() {
        return MavenDependencies;
    }
}
