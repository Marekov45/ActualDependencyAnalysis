package de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.StatisticInformation;
import java.util.List;

/**
 * Created by Marvin Rekovsky on 04.07.19.
 *
 * This class represents the implementation of an {@link StatisticInformation} for the framework. It holds the
 * forwarded information of all JavaScript dependencies and unused declared dependencies in a JavaScript project.
 */
public class NodeDependencyStatisticInformation extends StatisticInformation {

    private final List<String> allForwardedNodeDependencies;
    private final List<String> unusedForwardedNodeDependencies;
    private boolean isMultiModule;

    public NodeDependencyStatisticInformation(AnalysisResult analysisResult, String processedPluginName, List<String> allForwardedNodeDependencies, List<String> unusedForwardedNodeDependencies,boolean isMultiModule) {
        super(analysisResult, processedPluginName);
        this.allForwardedNodeDependencies = allForwardedNodeDependencies;
        this.unusedForwardedNodeDependencies = unusedForwardedNodeDependencies;
        this.isMultiModule = isMultiModule;
    }

    public List<String> getAllForwardedNodeDependencies() {
        return allForwardedNodeDependencies;
    }

    public List<String> getUnusedForwardedNodeDependencies() {
        return unusedForwardedNodeDependencies;
    }

    public boolean isMultiModule() {
        return isMultiModule;
    }
}
