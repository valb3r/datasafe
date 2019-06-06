package de.adorsys.datasafe.graph;

import com.google.auto.service.AutoService;
import dagger.model.BindingGraph;
import dagger.spi.BindingGraphPlugin;
import dagger.spi.DiagnosticReporter;

import javax.lang.model.element.TypeElement;
import java.util.stream.Collectors;

@AutoService(BindingGraphPlugin.class)
public class GraphPlug implements BindingGraphPlugin {

    @Override
    public void visitGraph(BindingGraph bindingGraph, DiagnosticReporter diagnosticReporter) {
        TypeElement componentElement =
                bindingGraph.rootComponentNode().componentPath().currentComponent();
        System.out.println(componentElement.getSimpleName());
        for (BindingGraph.Node node : bindingGraph.nodes().stream().distinct().collect(Collectors.toList())) {
            if (!(node instanceof BindingGraph.BindingNode)) {
                continue;
            }

            BindingGraph.BindingNode asBind = (BindingGraph.BindingNode) node;
            System.out.println("--" + asBind.binding().key().type().toString() + "/" + asBind.binding().bindingElement().get().getSimpleName());
        }
    }
}


