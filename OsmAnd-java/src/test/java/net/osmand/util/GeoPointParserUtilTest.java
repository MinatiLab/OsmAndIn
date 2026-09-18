package net.osmand.util;


import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class GeoPointParserUtilTest {

	@Test
	public void testGeoPointUrlDecode() {
		// bug in get scheme getSchemeSpecificPart()
		// equal results for : URI.create("geo:0,0?q=86HJV99P+29") && URI.create("geo:0,0?q=86HJV99P%2B29");
		GeoParsedPoint test = GeoPointParserUtil.parse("geo:0,0?q=86HJV99P%2B29");
		Assert.assertEquals(test.getQuery(), "86HJV99P+29");
	}

	@Test
	public void testOsmIndiaIndicMapParser() {
		// https://www.openstreetmap.in/indic-map/#5.1/23.300/82.000
		GeoParsedPoint actual = GeoPointParserUtil.parse(
				"https://www.openstreetmap.in/indic-map/#5.1/23.300/82.000");
		assertGeoPoint(actual, new GeoParsedPoint(23.300, 82.000, 5));

		// the same link without the www. prefix
		actual = GeoPointParserUtil.parse("https://openstreetmap.in/indic-map/#11/28.6139/77.2090");
		assertGeoPoint(actual, new GeoParsedPoint(28.6139, 77.2090, 11));
	}

	@Test
	public void testGeoPoint() {
		final int ilat = 34, ilon = -106;
		final double dlat = 34.99393, dlon = -106.61568;
		final String name = "Treasure Island";
		int z = GeoParsedPoint.NO_ZOOM;
		String url;

		String noQueryParameters[] = {
				"geo:0,0",
				"geo:0,0?",
				"http://download.osmand.net/go",
				"http://download.osmand.net/go?",
		};
		for (String s : noQueryParameters) {
			URI uri = URI.create(s);
			Map<String, String> map = GeoPointParserUtil.getQueryParameters(uri);
			System.out.print(s + " map: " + map.size() + "...");
			if (map.size() != 0) {
				System.out.println("");
				throw new RuntimeException("Map should be 0 but is " + map.size());
			}
			System.out.println(" Passed!");
		}

		String oneQueryParameter[] = {
				"geo:0,0?m",
				"geo:0,0?m=",
				"geo:0,0?m=foo",
				"geo:0,0?q=%D0%9D%D0",
				"http://download.osmand.net/go?lat",
				"http://download.osmand.net/go?lat=",
				"http://download.osmand.net/go?lat=34.99393",
		};
		for (String s : oneQueryParameter) {
			URI uri = URI.create(s);
			Map<String, String> map = GeoPointParserUtil.getQueryParameters(uri);
			System.out.print(s + " map: " + map.size() + "...");
			if (map.size() != 1) {
				System.out.println("");
				throw new RuntimeException("Map should be 1 but is " + map.size());
			}
			System.out.println(" Passed!");
		}

		String twoQueryParameters[] = {
				"geo:0,0?z=11&q=Lots+Of+Stuff",
				"http://osmand.net/go?lat=34.99393&lon=-110.12345",
				"http://www.osmand.net/go.html?lat=34.99393&lon=-110.12345",
				"http://download.osmand.net/go?lat=34.99393&lon=-110.12345",
				"http://download.osmand.net/go?lat=34.99393&lon=-110.12345#this+should+be+ignored",
		};
		for (String s : twoQueryParameters) {
			URI uri = URI.create(s);
			Map<String, String> map = GeoPointParserUtil.getQueryParameters(uri);
			System.out.print(s + " map: " + map.size() + "...");
			if (map.size() != 2) {
				System.out.println("");
				throw new RuntimeException("Map should be 2 but is " + map.size());
			}
			System.out.println(" Passed!");
		}



		// geo:34,-106
		url = "geo:" + ilat + "," + ilon;
		System.out.println("url: " + url);
		GeoParsedPoint actual = GeoPointParserUtil.parse(url);
		assertUrlEquals(url, actual.getGeoUriString());
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon));

		// geo:34.99393,-106.61568
		url = "geo:" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertUrlEquals(url, actual.getGeoUriString());
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));

		// geo:34.99393,-106.61568?z=11
		z = 11;
		url = "geo:" + dlat + "," + dlon + "?z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertUrlEquals(url, actual.getGeoUriString());
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// geo:34.99393,-106.61568 (Treasure Island)
		url = "geo:" + dlat + "," + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, name));

		// geo:34.99393,-106.61568?z=11 (Treasure Island)
		z = 11;
		url = "geo:" + dlat + "," + dlon + "?z=" + z + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:34.99393,-106.61568?q=34.99393%2C-106.61568 (Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:" + dlat + "," + dlon + "?q=" + dlat + "%2C" + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:34.99393,-106.61568?q=34.99393,-106.61568(Treasure+Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:" + dlat + "," + dlon + "?q=" + dlat + "," + dlon + "(" + URLEncoder.encode(name) + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));
		assertUrlEquals(url, actual.getGeoUriString());

		// 0,0?q=34,-106(Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:0,0?q=" + ilat + "," + ilon + "(" + name + ")";
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z, name));

		// 0,0?q=34.99393,-106.61568(Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:0,0?q=" + dlat + "," + dlon + "(" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:0,0?z=11&q=34.99393,-106.61568(Treasure Island)
		z = 11;
		url = "geo:0,0?z=" + z + "&q=" + dlat + "," + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:0,0?z=11&q=34.99393,-106.61568
		z = 11;
		url = "geo:0,0?z=" + z + "&q=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// geo: link as emitted by calendar apps
		// geo:0,0?q=760 West Genesee Street Syracuse NY 13204
		String qstr = "760 West Genesee Street Syracuse NY 13204";
		url = "geo:0,0?q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));
		assertUrlEquals(url, actual.getGeoUriString());

		// geo:?q=Paris
		qstr = "Paris";
		url = "geo:?q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));
		assertUrlEquals("geo:0,0?q=" + URLEncoder.encode(qstr), actual.getGeoUriString());

		// geo:0,0?q=760 West Genesee Street Syracuse NY 13204
		qstr = "760 West Genesee Street Syracuse NY 13204";
		url = "geo:0,0?q=" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// geo:0,0?z=11&q=1600+Amphitheatre+Parkway,+CA
		qstr = "1600 Amphitheatre Parkway, CA";
		url = "geo:0,0?q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));
		assertUrlEquals(url, actual.getGeoUriString());

		// geo:0,0?z=11&q=1600+Amphitheatre+Parkway,+CA
		qstr = "1600 Amphitheatre Parkway, CA";
		url = "geo:0,0?z=11&q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// geo:50.451300,30.569900?z=15&q=50.451300,30.569900 (Kiev)
		z = 15;
		String qname = "Kiev";
		double qlat = 50.4513;
		double qlon = 30.5699;

		url = "geo:50.451300,30.569900?z=15&q=50.451300,30.569900 (Kiev)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qlat, qlon, z, qname));

		// geo:0,0?q=50.45%2C%2030.5233
		z = GeoParsedPoint.NO_ZOOM;
		qlat = 50.4513;
		qlon = 30.5699;

		url = "geo:0,0?q=" + qlat + "%2C%20" + qlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qlat, qlon, z, null));

		// http://download.osmand.net/go?lat=34&lon=-106&z=11
		url = "http://download.osmand.net/go?lat=" + ilat + "&lon=" + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://download.osmand.net/go?lat=34.99393&lon=-106.61568&z=11
		url = "http://download.osmand.net/go?lat=" + dlat + "&lon=" + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.osmand.net/go.html?lat=34&lon=-106&z=11
		url = "http://www.osmand.net/go.html?lat=" + ilat + "&lon=" + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://www.osmand.net/go.html?lat=34.99393&lon=-106.61568&z=11
		url = "http://www.osmand.net/go.html?lat=" + dlat + "&lon=" + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://osmand.net/go?lat=34&lon=-106&z=11
		url = "http://osmand.net/go?lat=" + ilat + "&lon=" + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://osmand.net/go?lat=34.99393&lon=-106.61568&z=11
		url = "http://osmand.net/go?lat=" + dlat + "&lon=" + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		/* URLs straight from various services, instead of generated here */

		String urls[] = {
				"https://www.openstreetmap.in/indic-map/#5.1/23.300/82.000",
				"https://openstreetmap.in/indic-map/#11/28.6139/77.2090",
				"https://www.openstreetmap.in/indic-map/#15/19.0760/72.8777",
				"http://download.osmand.net/go?lat=34.99393&lon=-106.61568&z=11",
				"http://osmand.net/go?lat=34.99393&lon=-106.61568&z=11",
		};

		for (String u : urls) {
			System.out.println("url: " + u);
			actual = GeoPointParserUtil.parse(u);
			if (actual == null)
				throw new RuntimeException(u + " not parsable!");
			System.out.println("Properly parsed as: " + actual.getGeoUriString());
		}

		// OsmAndIn ships for India only, so links of map services that do not cover it are
		// deliberately left unhandled - they must not crash or cause problems either
		String[] unparsableUrls = {
				"https://www.openstreetmap.org/#map=6/33.907/34.662",
				"https://osm.org/go/0LQ127-?m",
				"https://www.google.com/maps/search/food/@34.99393,-106.61568,14z",
				"http://maps.yandex.ru/?ll=34.99393,-106.61568&z=11",
				"http://map.baidu.com/?l=11&c=3499393,-10661568",
				"http://www.amap.com/?q=34.99393,-106.61568",
				"https://www.here.com/?map=48.23145,16.38454,15,normal",
				"http://map.qq.com/?l=261496722",
				"http://maps.apple.com/?ll=34.99393,-106.61568&z=11",
				"http://ge0.me/44TvlEGXf-/Kyiv",
				"https://openstreetmap.de/zoom=11&lat=34.99393&lon=-106.61568",
				"http://goo.gl/maps/Cji0V",
		};

		for (String u : unparsableUrls) {
			System.out.println("url: " + u);
			actual = GeoPointParserUtil.parse(u);
			if (actual != null)
				throw new RuntimeException(u + " should not be parsable, but parse returned " + actual);
			System.out.println("Handled URL");
		}
	}



	private static boolean areCloseEnough(double a, double b, long howClose) {
		long aRounded = (long) Math.round(a * Math.pow(10, howClose));
		long bRounded = (long) Math.round(b * Math.pow(10, howClose));
		return aRounded == bRounded;
	}

	private static void assertGeoPoint(GeoParsedPoint actual, GeoParsedPoint expected) {
		if (expected.getQuery() != null) {
			if (!expected.getQuery().equals(actual.getQuery()))
				throw new RuntimeException("Query param not equal:\n'" +
						actual.getQuery() + "' != '" + expected.getQuery());
		} else {
			double aLat = actual.getLatitude(), eLat = expected.getLatitude(), aLon = actual.getLongitude(), eLon = expected.getLongitude();
			int aZoom = actual.getZoom(), eZoom = expected.getZoom();
			String aLabel = actual.getLabel(), eLabel = expected.getLabel();
			if (eLabel != null) {
				if (!aLabel.equals(eLabel)) {
					throw new RuntimeException("Point label is not equal; actual="
							+ aLabel + ", expected=" + eLabel);
				}
			}
			if (!areCloseEnough(eLat, aLat, 5)) {
				throw new RuntimeException("Latitude is not equal; actual=" + aLat + ", expected=" + eLat);
			}
			if (!areCloseEnough(eLon, aLon, 5)) {
				throw new RuntimeException("Longitude is not equal; actual=" + aLon + ", expected=" + eLon);
			}
			if (eZoom != aZoom) {
				throw new RuntimeException("Zoom is not equal; actual=" + aZoom + ", expected=" + eZoom);
			}
		}
		System.out.println("Passed: " + actual);
	}

	private static void assertUrlEquals(String actual, String expected) {
		if (actual == null || !actual.equals(expected))
			throw new RuntimeException("URLs not equal; actual=" + actual + ", expected=" + expected);
	}

}
