import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Main {
    static double numeric(String value) {
        double v = Double.parseDouble(value);
        if (!Double.isFinite(v)) throw new IllegalArgumentException("finite number required");
        return v;
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    static List<String[]> input(int columns) throws IOException {
        var rows = new ArrayList<String[]>();
        var reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            require(rows.size() < 100000, "input limit exceeded");
            var row = line.split(",", -1);
            require(row.length == columns && Arrays.stream(row).noneMatch(String::isBlank), "invalid CSV row");
            rows.add(row);
        }
        require(!rows.isEmpty(), "missing input");
        return rows;
    }
    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--self-test")) { test(); System.out.println("tests passed"); }
            else run();
        } catch (Exception e) { System.err.println(e.getMessage()); System.exit(2); }
    }

    record Link(String from, String to, double latency, boolean healthy) {}
    record Visit(String node, double distance) {}
    record Route(List<String> path, double latency) {}
    static Route route(List<Link> links, String start, String target) {
        var graph = new HashMap<String, List<Link>>();
        for (var l : links) { require(l.latency >= 0, "negative latency"); graph.computeIfAbsent(l.from, k -> new ArrayList<>()).add(l); graph.computeIfAbsent(l.to, k -> new ArrayList<>()); }
        require(graph.containsKey(start) && graph.containsKey(target), "unknown endpoint");
        var distance = new HashMap<String, Double>(); var parent = new HashMap<String, String>();
        var queue = new PriorityQueue<Visit>(Comparator.comparingDouble(Visit::distance).thenComparing(Visit::node));
        distance.put(start, 0.0); queue.add(new Visit(start, 0));
        while (!queue.isEmpty()) {
            var v = queue.remove(); if (v.distance != distance.getOrDefault(v.node, Double.POSITIVE_INFINITY)) continue;
            if (v.node.equals(target)) break;
            for (var l : graph.get(v.node)) {
                if (!l.healthy) continue; double candidate = v.distance + l.latency;
                if (candidate < distance.getOrDefault(l.to, Double.POSITIVE_INFINITY)) { distance.put(l.to, candidate); parent.put(l.to, v.node); queue.add(new Visit(l.to, candidate)); }
            }
        }
        if (!distance.containsKey(target)) return new Route(List.of(), Double.POSITIVE_INFINITY);
        var path = new ArrayList<String>(); for (String node = target; node != null; node = parent.get(node)) path.add(node); Collections.reverse(path);
        return new Route(path, distance.get(target));
    }
    static void test() {
        var links = List.of(new Link("A", "B", 1, false), new Link("A", "C", 2, true), new Link("C", "B", 3, true));
        var r = route(links, "A", "B"); require(r.path.equals(List.of("A", "C", "B")) && r.latency == 5, "alternate route mismatch");
        require(route(links, "B", "A").path.isEmpty(), "unreachable route accepted");
    }
    static void run() throws IOException {
        var links = new ArrayList<Link>();
        for (var r : input(4)) { require(r[3].equals("UP") || r[3].equals("DOWN"), "invalid link state"); links.add(new Link(r[0], r[1], numeric(r[2]), r[3].equals("UP"))); }
        var result = route(links, "field", "control");
        System.out.printf(Locale.ROOT, "reachable=%s,latency_ms=%.3f,path=%s%n", !result.path.isEmpty(), result.latency, String.join("|", result.path));
    }

}
