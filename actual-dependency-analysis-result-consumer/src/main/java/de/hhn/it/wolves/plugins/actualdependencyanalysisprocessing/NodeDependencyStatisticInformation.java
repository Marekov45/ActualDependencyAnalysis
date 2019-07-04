package de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.StatisticInformation;

import java.util.List;

public class NodeDependencyStatisticInformation extends StatisticInformation {

    private final List<String> forwardedNodeDependencies;

    public NodeDependencyStatisticInformation(AnalysisResult analysisResult, String processedPluginName, List<String> forwardedNodeDependencies) {
        super(analysisResult, processedPluginName);
        this.forwardedNodeDependencies = forwardedNodeDependencies;
    }

    public List<String> getForwardedNodeDependencies() {
        return forwardedNodeDependencies;
    }
}
