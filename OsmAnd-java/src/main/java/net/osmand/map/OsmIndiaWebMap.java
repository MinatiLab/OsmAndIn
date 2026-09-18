package net.osmand.map;

/**
 * Links to the web map of OpenStreetMap India - https://www.openstreetmap.in.
 * <p>
 * The default openstreetmap.org rendering draws the borders of India as they are on the
 * ground, which does not match the boundaries published by the Survey of India. The OSM
 * India community renders the very same OSM data with the boundaries drawn as required -
 * the "Indic map" - so every link that opens a location in a web map points there instead
 * of openstreetmap.org.
 * <p>
 * Only the map view moved: object pages (node / way / relation / note / user), the editing
 * API and OAuth still live on openstreetmap.org, as openstreetmap.in does not serve them.
 */
public class OsmIndiaWebMap {

	public static final String HOST = "openstreetmap.in";
	public static final String BASE_URL = "https://www." + HOST + "/";
	public static final String INDIC_MAP_URL = BASE_URL + "indic-map/";

	/**
	 * Builds a link to the Indic map centred on the given position, for example
	 * https://www.openstreetmap.in/indic-map/#5.1/23.300/82.000
	 */
	public static String getLocationUrl(String zoom, String latitude, String longitude) {
		return INDIC_MAP_URL + "#" + zoom + "/" + latitude + "/" + longitude;
	}

	public static boolean isWebMapHost(String host) {
		return host != null && (host.equals(HOST) || host.endsWith("." + HOST));
	}
}
