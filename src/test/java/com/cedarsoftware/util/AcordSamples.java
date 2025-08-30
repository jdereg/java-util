package com.cedarsoftware.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AcordSamples {

    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final XmlMapper XML = (XmlMapper) new XmlMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** Build a big, deterministic ACORD-ish envelope with attached schedule. */
    public static ACORD buildEnvelope(int locations, int buildingsPerLoc, int coveragesPerBldg, long seed) {
        Random rnd = new Random(seed);

        Submission sub = new Submission();
        sub.SubmissionId = "SUB-" + Math.abs(seed % 1_000_000);

        sub.Insured = new Insured();
        sub.Insured.Name = "Acme Widgets LLC";
        sub.Insured.FEIN = String.format("%02d-%07d", rnd.nextInt(90) + 10, rnd.nextInt(10_000_000));
        sub.Insured.Address = "100 Main St, Cincinnati, OH 45202";

        sub.Policy = new Policy();
        sub.Policy.LineOfBusiness = "Property";
        sub.Policy.EffectiveDate = "2025-10-01";
        sub.Policy.ExpirationDate = "2026-10-01";
        sub.Policy.Carrier = "GAIG";
        sub.Policy.Program = "SpecProp";

        sub.Locations = new ArrayList<>(locations);
        sub.AttachedSchedule = new AttachedSchedule();
        sub.AttachedSchedule.Type = "BuildingCoverage";
        sub.AttachedSchedule.Items = new ArrayList<>(locations * buildingsPerLoc * coveragesPerBldg);

        for (int li = 1; li <= locations; li++) {
            Location loc = new Location();
            loc.Id = "LOC-" + li;
            loc.Address = (100 + li) + " Market St, Cincinnati, OH 45202";
            loc.Buildings = new ArrayList<>(buildingsPerLoc);

            for (int bi = 1; bi <= buildingsPerLoc; bi++) {
                Building b = new Building();
                b.Id = "BLD-" + li + "-" + bi;
                b.TSI = 500_000 + (rnd.nextInt(9500) * 100.0);
                b.Coverages = new ArrayList<>(coveragesPerBldg);

                for (int ci = 1; ci <= coveragesPerBldg; ci++) {
                    Coverage cov = new Coverage();
                    switch (ci % 5) {
                        case 1:
                            cov.Code = "BPP";
                            break;
                        case 2:
                            cov.Code = "BI";
                            break;
                        case 3:
                            cov.Code = "EQ";
                            break;
                        case 4:
                            cov.Code = "FLOOD";
                            break;
                        default:
                            cov.Code = "PROPCAT";
                            break;
                    }

                    cov.Limit = 250_000 + (rnd.nextInt(4000) * 1000.0);
                    cov.Deductible = (ci % 2 == 0) ? 0.0 : 10_000.0;
                    b.Coverages.add(cov);

                    ScheduleItem it = new ScheduleItem();
                    it.LocationId = loc.Id;
                    it.BuildingId = b.Id;
                    it.CoverageCode = cov.Code;
                    it.Limit = cov.Limit;
                    it.Rate = 0.005 + (rnd.nextInt(150) / 10_000.0);
                    sub.AttachedSchedule.Items.add(it);
                }
                loc.Buildings.add(b);
            }
            sub.Locations.add(loc);
        }

        sub.Quote = new Quote();
        sub.Quote.QuoteId = "Q-" + Math.abs(seed % 1_000_000);
        sub.Quote.Status = "Proposed";
        sub.Quote.Premium = 100_000 + rnd.nextInt(300_000);
        sub.Quote.Fees = 2_500;

        SpecialtySubmissionRq ssrq = new SpecialtySubmissionRq();
        ssrq.Submission = sub;

        InsuranceSvcRq isr = new InsuranceSvcRq();
        isr.SpecialtySubmissionRq = ssrq;

        ACORD acord = new ACORD();
        acord.Version = "2.32";
        acord.InsuranceSvcRq = isr;
        return acord;
    }

    public static String toJson(ACORD acord) throws JsonProcessingException {
        // wrap under {"ACORD": ...} to mirror sample
        return JSON.writeValueAsString(new AcordRoot(acord));
    }

    public static ACORD fromJson(String json) throws JsonProcessingException {
        return JSON.readValue(json, AcordRoot.class).ACORD;
    }

    public static String toXml(ACORD acord) throws JsonProcessingException {
        return XML.writeValueAsString(acord);
    }

    public static ACORD fromXml(String xml) throws JsonProcessingException {
        return XML.readValue(xml, ACORD.class);
    }

    /** helper wrapper to get {"ACORD": {...}} in JSON */
    public static final class AcordRoot {
        public ACORD ACORD;
        public AcordRoot() {}
        public AcordRoot(ACORD a) { this.ACORD = a; }
    }

    // ====== Model (Jackson XML annotations to keep names predictable) ======

    @JsonPropertyOrder({"Version", "InsuranceSvcRq"})
    public static class ACORD {
        @JacksonXmlProperty(isAttribute = true, localName = "Version")
        public String Version;

        @JacksonXmlProperty(localName = "InsuranceSvcRq")
        public InsuranceSvcRq InsuranceSvcRq;

        public ACORD() {}
    }

    public static class InsuranceSvcRq {
        @JacksonXmlProperty(localName = "SpecialtySubmissionRq")
        public SpecialtySubmissionRq SpecialtySubmissionRq;
        public InsuranceSvcRq() {}
    }

    public static class SpecialtySubmissionRq {
        @JacksonXmlProperty(localName = "Submission")
        public Submission Submission;
        public SpecialtySubmissionRq() {}
    }

    @JsonPropertyOrder({"SubmissionId","Insured","Policy","Locations","Quote","AttachedSchedule"})
    public static class Submission {
        @JacksonXmlProperty(isAttribute = true, localName = "SubmissionId")
        public String SubmissionId;

        public Insured Insured;
        public Policy Policy;

        @JacksonXmlElementWrapper(localName = "Locations")
        @JacksonXmlProperty(localName = "Location")
        public List<Location> Locations;

        public Quote Quote;
        public AttachedSchedule AttachedSchedule;

        public Submission() {}
    }

    public static class Insured {
        public String Name;
        public String FEIN;
        public String Address;
        public Insured() {}
    }

    public static class Policy {
        public String LineOfBusiness;
        public String EffectiveDate;
        public String ExpirationDate;
        public String Carrier;
        public String Program;
        public Policy() {}
    }

    public static class Location {
        @JacksonXmlProperty(isAttribute = true, localName = "Id")
        public String Id;
        public String Address;

        @JacksonXmlElementWrapper(localName = "Buildings")
        @JacksonXmlProperty(localName = "Building")
        public List<Building> Buildings;
        public Location() {}
    }

    public static class Building {
        @JacksonXmlProperty(isAttribute = true, localName = "Id")
        public String Id;
        public double TSI;

        @JacksonXmlElementWrapper(localName = "Coverages")
        @JacksonXmlProperty(localName = "Coverage")
        public List<Coverage> Coverages;
        public Building() {}
    }

    public static class Coverage {
        @JacksonXmlProperty(isAttribute = true, localName = "Code")
        public String Code;
        public double Limit;
        public double Deductible;
        public Coverage() {}
    }

    public static class Quote {
        @JacksonXmlProperty(isAttribute = true, localName = "QuoteId")
        public String QuoteId;
        @JacksonXmlProperty(isAttribute = true, localName = "Status")
        public String Status;

        public double Premium;
        public double Fees;
        public Quote() {}
    }

    public static class AttachedSchedule {
        @JacksonXmlProperty(isAttribute = true, localName = "Type")
        public String Type;

        @JacksonXmlElementWrapper(localName = "Items")
        @JacksonXmlProperty(localName = "Item")
        public List<ScheduleItem> Items;
        public AttachedSchedule() {}
    }

    public static class ScheduleItem {
        @JacksonXmlProperty(isAttribute = true, localName = "LocationId")
        public String LocationId;
        @JacksonXmlProperty(isAttribute = true, localName = "BuildingId")
        public String BuildingId;
        @JacksonXmlProperty(isAttribute = true, localName = "CoverageCode")
        public String CoverageCode;

        public double Limit;
        public double Rate;
        public ScheduleItem() {}
    }
}
