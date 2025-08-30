package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class XmlVsJsonParsingTest {

    // ----- knobs -----
    static final int LOCATIONS = Integer.getInteger("locs", 200);
    static final int BUILDINGS_PER_LOC = Integer.getInteger("blds", 10);
    static final int COV_PER_BLDG = Integer.getInteger("covs", 20);
    static final int WARMUP = Integer.getInteger("warmup", 50);
    static final int RUNS = Integer.getInteger("runs", 100);

    @FunctionalInterface
    interface CheckedSupplier<T> { T get() throws Exception; }   // <-- lets lambdas throw

    static final class Bench {
        final String name;
        final CheckedSupplier<Object> runner;
        final int bytesPerRun;
        Bench(String name, CheckedSupplier<Object> runner, int bytesPerRun) {
            this.name = name; this.runner = runner; this.bytesPerRun = bytesPerRun;
        }
    }
    static final class Result {
        final String name; final long bytes; final long ns;
        Result(String name, long bytes, long ns) { this.name = name; this.bytes = bytes; this.ns = ns; }
        double seconds() { return ns / 1_000_000_000.0; }
        double mib()     { return bytes / 1_048_576.0; }
        double mibps()   { return mib() / seconds(); }
        double msPer()   { return (seconds() * 1000.0) / RUNS; }
    }

    // Minimal SAX handler to ensure parser does real work
    static final class CountingHandler extends DefaultHandler {
        long elementCount = 0, attrCount = 0, charCount = 0;
        @Override public void startElement(String uri, String local, String qName, Attributes atts) {
            elementCount++; if (atts != null) attrCount += atts.getLength();
        }
        @Override public void characters(char[] ch, int start, int length) { charCount += length; }
    }

    @Test
    void all_parsers_benchmark() throws Exception {
        long seed = 4242L;

        // Build once, serialize once
        AcordSamples.ACORD acord = AcordSamples.buildEnvelope(LOCATIONS, BUILDINGS_PER_LOC, COV_PER_BLDG, seed);
        String xml = AcordSamples.toXml(acord);
        String json = AcordSamples.toJson(acord);
        assertNotNull(xml); assertNotNull(json);

        byte[] xmlBytes  = xml.getBytes(StandardCharsets.UTF_8);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        System.out.println("Payload sizes:");
        System.out.println("  XML  = " + prettyBytes(xmlBytes.length));
        System.out.println("  JSON = " + prettyBytes(jsonBytes.length));
        System.out.println("Runs: " + RUNS + "  Warmup: " + WARMUP);
        System.out.println();

        // Factories
        SAXParserFactory saxFactory = secureSaxFactory();
        DocumentBuilderFactory domFactory = secureDomFactory();
        JsonFactory jf = new JsonFactory();

        // Define variants (each may throw -> CheckedSupplier)
        List<Bench> benches = CollectionUtilities.listOf(
                new Bench("XML (Jackson POJO)", () -> { AcordSamples.fromXml(xml); return 1; }, xmlBytes.length),
                new Bench("JSON (Jackson POJO)", () -> { AcordSamples.fromJson(json); return 1; }, jsonBytes.length),

                new Bench("XML (SAX walk)", () -> {
                    CountingHandler h = new CountingHandler();
                    try (ByteArrayInputStream is = new ByteArrayInputStream(xmlBytes)) {
                        saxFactory.newSAXParser().parse(is, h);
                    }
                    return h.elementCount;
                }, xmlBytes.length),

                new Bench("XML (StAX walk)", () -> {
                    try (ByteArrayInputStream is = new ByteArrayInputStream(xmlBytes)) {
                        javax.xml.stream.XMLInputFactory xif = javax.xml.stream.XMLInputFactory.newInstance();
                        xif.setProperty(javax.xml.stream.XMLInputFactory.IS_COALESCING, Boolean.TRUE);
                        javax.xml.stream.XMLStreamReader xr = xif.createXMLStreamReader(is, "UTF-8");
                        long tokens = 0;
                        while (xr.hasNext()) { xr.next(); tokens++; }
                        xr.close();
                        return tokens;
                    }
                }, xmlBytes.length),

                new Bench("XML (DOM build+walk)", () -> {
                    try (ByteArrayInputStream is = new ByteArrayInputStream(xmlBytes)) {
                        Document doc = domFactory.newDocumentBuilder().parse(is);
                        return countNodes(doc);
                    }
                }, xmlBytes.length),

                new Bench("JSON (streaming walk)", () -> {
                    try (ByteArrayInputStream is = new ByteArrayInputStream(jsonBytes);
                         JsonParser p = jf.createParser(is)) {
                        long tokens = 0;
                        while (p.nextToken() != null) tokens++;
                        return tokens;
                    }
                }, jsonBytes.length)
        );

        // Warmup
        for (Bench b : benches) for (int i = 0; i < WARMUP; i++) b.runner.get();

        // Timed runs
        List<Result> results = new ArrayList<>();
        for (Bench b : benches) {
            long t0 = System.nanoTime();
            long totalBytes = 0L;
            Object sink = null;
            for (int i = 0; i < RUNS; i++) {
                sink = b.runner.get();                     // allowed to throw
                totalBytes += (long) b.bytesPerRun;
            }
            long ns = System.nanoTime() - t0;
            if (sink == null) throw new AssertionError("runner returned null: " + b.name);
            results.add(new Result(b.name, totalBytes, ns));
        }

        printTable(results);

        // Structure sanity (same Location count)
        assertEquals(
                AcordSamples.fromJson(json).InsuranceSvcRq.SpecialtySubmissionRq.Submission.Locations.size(),
                AcordSamples.fromXml(xml).InsuranceSvcRq.SpecialtySubmissionRq.Submission.Locations.size()
        );
    }

    private static void printTable(List<Result> results) {
        results.sort(Comparator.comparingDouble(Result::mibps).reversed());
        int w = results.stream().map(r -> r.name.length()).max(Integer::compare).orElse(10);
        String fmtHeader = "%-" + w + "s  %12s  %8s  %10s  %12s%n";
        String fmtRow    = "%-" + w + "s  %12.2f  %8.3f  %10.1f  %12.2f%n";
        System.out.printf(fmtHeader, "Variant", "MiB parsed", "sec", "MiB/s", "ms/parse");
        for (Result r : results) {
            System.out.printf(fmtRow, r.name, r.mib(), r.seconds(), r.mibps(), r.msPer());
        }
    }

    private static SAXParserFactory secureSaxFactory() throws Exception {
        SAXParserFactory f = SAXParserFactory.newInstance();
        f.setNamespaceAware(true);
        try {
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Throwable ignore) { }
        return f;
    }

    private static DocumentBuilderFactory secureDomFactory() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        try {
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Throwable ignore) { }
        return f;
    }

    private static int countNodes(Node n) {
        int c = 1;
        for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
            c += countNodes(child);
        }
        return c;
    }

    // Keep your existing prettyBytes if you like
    private static String prettyBytes(long bytes) {
        NumberFormat nf = NumberFormat.getInstance(java.util.Locale.US);
        return nf.format(bytes) + " bytes (" + String.format(java.util.Locale.US, "%.2f", bytes / 1_048_576.0) + " MiB)";
    }
}
