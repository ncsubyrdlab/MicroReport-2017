package edu.ucsc.psyc_files.microreport;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes the process_reports.xml and translates into Report objects to be displayed on the map
 * in {@MainActivity}. Needs to be replaced.
 * XmlPullParser code based on: http://developer.android.com/training/basics/network-ops/xml.html
 * but note that this is so much more complicated than the code from Android Cookbook (see {@BulletinBoard.java}
 */
public class TransformReports {
    List<TransformReports.Report> reports;
    File reportsFile;
    private static final String ns = null;

    public TransformReports(File reportsFile) {
        //this.reports = reports;
        this.reportsFile = reportsFile;
    }

    public List<TransformReports.Report> getReports()  throws XmlPullParserException, IOException {
        reports = ReturnReportsList(reportsFile);
        return reports;
    }

    public void setReports(List<TransformReports.Report> reports) {
        this.reports = reports;
    }

    public File getReportsFile() {
        return reportsFile;
    }

    public void setReportsFile(File reportsFile) {
        this.reportsFile = reportsFile;
    }

     public List<TransformReports.Report> ReturnReportsList(File reportsFile)  throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        //http://developer.android.com/reference/java/io/FileInputStream.html
        InputStream in = new BufferedInputStream(new FileInputStream(reportsFile));
        parser.setInput(in, null);
        parser.nextTag();
        return readFeed(parser);
    }

    private List<TransformReports.Report> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<TransformReports.Report> reports = new ArrayList<TransformReports.Report>();

        parser.require(XmlPullParser.START_TAG, ns, "reports");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the report tag
            if (name.equals("report")) {
                reports.add(readReport(parser));
            } else {
                skip(parser);
            }
        }
        return reports;
    }

    public static class Report {
        public final String date;   //this is uDate not date as of 6/20/15
        public final String classOrEvent;
        public final String description;
        public final String reportID;
        public final String partID;
        public final String latitude;
        public final String longitude;

        private Report(String reportID, String partID, String date, String classOrEvent, String description, String latitude, String longitude) {
            this.reportID = reportID;
            this.partID = partID;
            this.date = date;
            this.classOrEvent = classOrEvent;
            this.description = description;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }


    // Parses the contents of an entry. If it encounters a tag, hands them off
// to the "read" methods for processing. Otherwise, skips the tag.
    private Report readReport(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "report");
        String reportID = null;
        String partID = null;
        String date = null;
        String classOrEvent = null;
        String description = null;
        String latitude = null;
        String longitude = null;

        //get attributes first
        reportID = parser.getAttributeValue(ns,"reportID");
        partID = parser.getAttributeValue(ns,"partID");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case ("uDate"):
                    date = readString(parser, name);
                    break;
                case ("classOrEvent"):
                    classOrEvent = readString(parser, name);
                    break;
                case ("description"):
                    description = readString(parser, name);
                    break;
                case ("latitude"):
                    latitude = readString(parser, name);
                    break;
                case ("longitude"):
                    longitude = readString(parser, name);
                    break;
                default:
                    skip(parser);
                    break;
            }

        }
        return new Report(reportID, partID, date, classOrEvent, description, latitude, longitude);
    }

    // Processes tags (all strings).
    private String readString(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, name);
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, name);
        return result;
    }

    //skip function
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}


