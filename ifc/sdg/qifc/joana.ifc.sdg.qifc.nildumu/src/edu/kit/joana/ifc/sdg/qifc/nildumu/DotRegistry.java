/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.Context.INFTY;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.bl;
import static guru.nidi.graphviz.attribute.Attributes.attr;
import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.mutNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.html.HtmlEscapers;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Bit;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;
import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.RankDir;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.MutableNode;

/**
 * Registry for generated graphviz files.
 * Yes it's global state, but it simplifies the UI and the registry can be easily cleaned.
 */
public class DotRegistry {

    /**
     * A dot file with a name and a topic
     */
    private static class DotFile implements Comparable<DotFile> {

        public final String topic;
        public final String name;
        public final Supplier<Graph> graphCreator;
        private final Path svgPath;
        private final boolean createdSVG = false;

        private DotFile(String topic, String name, Supplier<Graph> graphCreator, Path svgPath) {
            this.topic = topic;
            this.name = name;
            this.graphCreator = graphCreator;
            this.svgPath = svgPath;
        }

        public void delete(){
            try {
                Files.deleteIfExists(svgPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int compareTo(DotFile o) {
            if (topic.equals(o.topic)) {
                return name.compareTo(o.name);
            }
            return topic.compareTo(o.topic);
        }

        public Path getSvgPath() {
            if (!createdSVG){
                try {
                    Graphviz.fromGraph(graphCreator.get().named(name)).engine(Engine.DOT)
                            .render(Format.SVG_STANDALONE).toFile(svgPath.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return svgPath;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final static String TMP_DIR = "tmp";

    private boolean enabled = System.getenv("NILDUMU_GRAPGHS") != null && System.getenv("NILDUMU_GRAPGHS").equals("1");

    private Map<String, LinkedHashMap<String, DotFile>> filesPerTopic = new LinkedHashMap<>();

    private Path tmpDir;

    private static DotRegistry instance = new DotRegistry();

    private DotRegistry(){
        tmpDir = Paths.get(TMP_DIR);
        try {
            Files.createDirectories(tmpDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean enabled(){
        return enabled;
    }

    public void enable(){
        enabled = true;
    }

    public void disable(){
        enabled = false;
    }

    public Map<String, LinkedHashMap<String, DotFile>> getFilesPerTopic() {
        return filesPerTopic;
    }

    public void reset(){
        filesPerTopic.values().stream().flatMap(l -> l.values().stream()).forEach(DotFile::delete);
        filesPerTopic.clear();
        try {
            for (Path path : Files.list(tmpDir).collect(Collectors.toList())){
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DotRegistry get() {
        return instance;
    }

    /**
     * Creates a dot graph using the passed graphCreator if the registry is enabled.
     * Stores it under the topic.
     *
     * <b>Does not create any files, the svg file is created lazily</b>
     */
    public void store(String topic, String name, Supplier<Supplier<Graph>> graphCreator){
        if (enabled){
            Path topicPath = tmpDir.resolve(topic);
            if (!filesPerTopic.containsKey(topic)){
                filesPerTopic.put(topic, new LinkedHashMap<>());
                try {
                    Files.createDirectories(topicPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            filesPerTopic.get(topic).put(name, new DotFile(topic, name, graphCreator.get(), topicPath.resolve(name + ".svg")));
        }
    }

    public boolean hasTopic(String topic){
        return filesPerTopic.containsKey(topic);
    }

    public LinkedHashMap<String, DotFile> getFilesPerTopic(String topic){
        return filesPerTopic.getOrDefault(topic == null ? "" : topic, new LinkedHashMap<>());
    }

    public boolean has(String topic, String name){
        return getFilesPerTopic(topic).containsKey(name);
    }

    public Optional<DotFile> get(String topic, String name){
        return Optional.ofNullable(getFilesPerTopic(topic).getOrDefault(name, null));
    }

    public static class Anchor {

        private final String name;

        private final Value value;

        public Anchor(String name, Value value) {
            this.name = name;
            this.value = value;
        }
    }

    public static Graph createDotGraph(Context context, String name, List<Anchor> topAnchors, Anchor botAnchor,
                                       Set<Bit> selectedBits){
        return createDotGraph(context, name, topAnchors, botAnchor, b -> {
            if (selectedBits.contains(b)){
                return Arrays.asList(Color.RED, Color.RED.font());
            }
            return Collections.emptyList();
        });
    }

    public static Graph createDotGraph(Context context, String name, List<Anchor> topAnchors, Anchor botAnchor,
                                       Function<Bit, List<Attributes>> nodeAttributes){
        List<MutableNode> nodeList = new ArrayList<>();
        Set<Bit> alreadyVisited = new HashSet<>();
        Function<Bit, Attributes> dotLabel = b -> {
            List<String> parts = new ArrayList<>();
            if (context.weight(b) == INFTY){
                parts.add("inf");
            }
            parts.add(b.uniqueId());
            parts.add(HtmlEscapers.htmlEscaper().escape(b.toString()));
            return Records.of((String[])parts.toArray(new String[0]));
        };
        Map<Bit, MutableNode> nodes = new DefaultMap<>((map, b) -> {
            MutableNode node = mutNode(b.bitNo + "");
            node.add(dotLabel.apply(b));
            node.add(nodeAttributes.apply(b).toArray(new Attributes[0]));
            node.add(attr("font-family", "Helvetica"));
            node.add(attr("fontname", "Helvetica"));
            nodeList.add(node);
            return node;
        });
        for (Bit bit : botAnchor.value) {
            bl.walkBits(bit, b -> {
                nodes.get(b).addLink((String[])b.deps().stream().sorted(Comparator.comparingLong(d -> d.bitNo)).map(d -> d.bitNo + "").toArray(String[]::new));
            }, b -> false, alreadyVisited, b -> b.deps().stream().sorted(Comparator.comparingLong(d -> d.bitNo)).collect(Collectors.toList()));
        }
        topAnchors.stream().sorted(Comparator.comparing(s -> s.name)).forEach(anchor -> {
            Lattices.Value val = anchor.value;
            String nodeId = anchor.name;
            MutableNode paramNode = mutNode(nodeId);
            paramNode.add(Color.GREEN, Color.GREEN.font());
            nodeList.add(paramNode);
            val.stream().map(nodes::get).forEach(n -> n.addLink(paramNode));
        });
        MutableNode ret = mutNode(botAnchor.name);
        ret.add(Color.BLUE, Color.BLUE.font());
        nodeList.add(ret);
        ret.addLink((MutableNode[])botAnchor.value.stream().map(nodes::get).toArray(MutableNode[]::new));
        return graph(name).directed().nodeAttr().with(Font.name("Helvetica")).graphAttr().with(RankDir.BOTTOM_TO_TOP).graphAttr().with(Font.name("Helvetica")).with((MutableNode[])nodeList.toArray(new MutableNode[0]));
    }
    
    public void storeFiles() {
    	filesPerTopic.values().stream().flatMap(v -> v.values().stream()).forEach(DotFile::getSvgPath);
    }

    public void setTmpDir(String tmpDirectory) {
    	tmpDir = Paths.get(tmpDirectory);
        try {
            Files.createDirectories(tmpDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
