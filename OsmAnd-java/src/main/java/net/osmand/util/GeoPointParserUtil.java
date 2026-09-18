package net.osmand.util;

import net.osmand.map.OsmIndiaWebMap;

import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoPointParserUtil {

	private static String getQueryParameter(final String param, URI uri) {
		final String query = uri.getQuery();
		String value = null;
		if (query != null && query.contains(param)) {
			String[] params = query.split("&");
			for (String p : params) {
				if (p.contains(param)) {
					value = p.substring(p.indexOf("=") + 1, p.length());
					break;
				}
			}
		}
		return value;
	}

	/**
	 * This parses out all of the parameters in the query string for both
	 * http: and geo: URIs.  This will only work on URIs with valid syntax, so
	 * it will not work on URIs that do odd things like have a query string in
	 * the fragment, like this one:
	 * http://www.amap.com/#!poi!!q=38.174596,114.995033|2|%E5%AE%BE%E9%A6%86&radius=1000
	 *
	 * @param uri
	 * @return {@link Map<String, String>} a Map of the query parameters
	 */
	static Map<String, String> getQueryParameters(URI uri) {
		String query = null;
		if (uri.isOpaque()) {
			String schemeSpecificPart = uri.getSchemeSpecificPart();
			int pos = schemeSpecificPart.indexOf("?");
			if (pos == schemeSpecificPart.length()) {
				query = "";
			} else if (pos > -1) {
				query = schemeSpecificPart.substring(pos + 1);
			}
		} else {
			query = uri.getRawQuery();
		}
		return getQueryParameters(query);
	}

	private static Map<String, String> getQueryParameters(String query) {
		final LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		if (query != null && !query.isEmpty()) {
			String[] params = query.split("[&/]");
			for (String p : params) {
				String[] keyValue = p.split("=");
				if (keyValue.length == 1)
					map.put(keyValue[0], "");
				else if (keyValue.length > 1)
					map.put(keyValue[0], URLDecoder.decode(keyValue[1]));
			}
		}
		return map;
	}

	/**
	 * Parses geo and map intents:
	 *
	 * @param uriString The URI as a String
	 * @return {@link GeoParsedPoint}
	 */
	public static GeoParsedPoint parse(String uriString) {
		List<GeoParsedPoint> points = parsePoints(uriString);
		if (!Algorithms.isEmpty(points)) {
			return points.get(0);
		}
		return null;
	}

	public static List<GeoParsedPoint> parsePoints(String uriString) {
		URI uri = createUri(uriString);
		String scheme = uri != null ? uri.getScheme() : null;
		if (scheme != null) {
			scheme = scheme.toLowerCase(Locale.US);

			if ("http".equals(scheme) || "https".equals(scheme)) {
				return parseLinkUri(uri);
			} else if ("geo".equals(scheme) || "osmand.geo".equals(scheme)) {
				return parseGeoUri(uri);
			}
		}
		return null;
	}

	public static URI createUri(final String uriString) {
		try {
			// amap.com uses | in their URLs, which is an illegal character for a URL
			return URI.create(uriString.trim().replaceAll("\\s+", "+")
					.replaceAll("%20", "+")
					.replaceAll("%2C", ",")
					.replaceAll("\\|", ";")
					.replaceAll("\\(\\(\\S+\\)\\)", ""));
		} catch (IllegalArgumentException e) {
		}
		return null;
	}

	private static List<GeoParsedPoint> parseLinkUri(URI uri) {
		String host = uri.getHost();
		if (host == null) {
			return null;
		}
		host = host.toLowerCase(Locale.US);
		String path = uri.getPath();
		if (path == null) {
			path = "";
		}
		String fragment = uri.getFragment();

		// lat-double, lon - double, zoom or z - int
		Set<String> simpleDomains = new HashSet<String>();
		simpleDomains.add("osmand.net");
		simpleDomains.add("www.osmand.net");
		simpleDomains.add("test.osmand.net");
		simpleDomains.add("download.osmand.net");

		try {
			if (OsmIndiaWebMap.isWebMapHost(host)) {
				return parseOsmIndiaUri(uri, fragment);
			} else if (simpleDomains.contains(host)) {
				return parseSimpleDomainsUri(uri, path, getQueryParameters(uri), fragment);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Parses a link to the OpenStreetMap India web map, for example
	 * https://www.openstreetmap.in/indic-map/#5.1/23.300/82.000 - the position is carried
	 * by the fragment as zoom/lat/lon, the rest is read from the query string.
	 */
	private static List<GeoParsedPoint> parseOsmIndiaUri(URI uri, String fragment) {
		// data in the query and/or fragment strings
		double lat = 0;
		double lon = 0;
		int zoom = GeoParsedPoint.NO_ZOOM;
		Map<String, String> queryMap = getQueryParameters(uri);

		if (queryMap.containsKey("route")) {
			String routeValue = queryMap.get("route");
			Pattern coordinatesPattern = Pattern.compile("^(\\d+[.]?\\d*),(\\d+[.]?\\d*);(\\d+[.]?\\d*),(\\d+[.]?\\d*)");
			Matcher coordinatesMatcher = coordinatesPattern.matcher(routeValue);
			if (coordinatesMatcher.matches()) {
				GeoParsedPoint pointFrom = new GeoParsedPoint(parseSilentDouble(coordinatesMatcher.group(1)), parseSilentDouble(coordinatesMatcher.group(2)));
				GeoParsedPoint pointTo = new GeoParsedPoint(parseSilentDouble(coordinatesMatcher.group(3)), parseSilentDouble(coordinatesMatcher.group(4)));

				List<GeoParsedPoint> parsedPoints = new ArrayList<>();
				parsedPoints.add(pointFrom);
				parsedPoints.add(pointTo);

				return parsedPoints;
			}
		} else if (queryMap.containsKey("from") || queryMap.containsKey("to")) {
			GeoParsedPoint pointFrom = null;
			String from = queryMap.get("from");
			if (!Algorithms.isEmpty(from)) {
				String[] coordinates = from.split(",");
				lat = parseSilentDouble(coordinates[0]);
				lon = parseSilentDouble(coordinates[1]);
				pointFrom = new GeoParsedPoint(lat, lon);
			}
			GeoParsedPoint pointTo = null;
			String to = queryMap.get("to");
			if (!Algorithms.isEmpty(to)) {
				String[] coordinates = to.split(",");
				lat = parseSilentDouble(coordinates[0]);
				lon = parseSilentDouble(coordinates[1]);
				pointTo = new GeoParsedPoint(lat, lon);
			}
			List<GeoParsedPoint> parsedPoints = new ArrayList<>();
			parsedPoints.add(pointFrom);
			parsedPoints.add(pointTo);

			return parsedPoints;
		}
		if (fragment != null) {
			if (fragment.startsWith("map=")) {
				fragment = fragment.substring("map=".length());
			}
			String[] vls = fragment.split("/|&"); //"&" to split off trailing extra parameters
			if (vls.length >= 3) {
				zoom = parseZoom(vls[0]);
				lat = parseSilentDouble(vls[1]);
				lon = parseSilentDouble(vls[2]);
			}
		} else if (queryMap != null) {
			String queryStr = queryMap.get("query");
			if (queryStr != null) {
				queryStr = queryStr.replace("+", " ").replace(",", " ");
				String[] vls = queryStr.split(" ");
				if (vls.length == 2) {
					lat = parseSilentDouble(vls[0]);
					lon = parseSilentDouble(vls[1]);
				}
				if (lat == 0 || lon == 0) {
					return Collections.singletonList(new GeoParsedPoint(queryStr));
				}
			}
		}
		// the query string sometimes has higher resolution values
		String mlat = getQueryParameter("mlat", uri);
		if (mlat != null) {
			lat = parseSilentDouble(mlat);
		}
		String mlon = getQueryParameter("mlon", uri);
		if (mlon != null) {
			lon = parseSilentDouble(mlon);
		}
		return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom));
	}

	private static List<GeoParsedPoint> parseSimpleDomainsUri(URI uri, String path, Map<String, String> params, String fragment) {
		if (uri.getQuery() == null && params.size() == 0) {
			// DOUBLE check this may be wrong test of openstreetmap.de (looks very weird url and server doesn't respond)
			params = getQueryParameters(path.substring(1));
		}
		if (params.containsKey("lat") && params.containsKey("lon")) {
			final double lat = parseSilentDouble(params.get("lat"));
			final double lon = parseSilentDouble(params.get("lon"));
			int zoom = GeoParsedPoint.NO_ZOOM;
			if (params.containsKey("z")) {
				zoom = parseZoom(params.get("z"));
			} else if (params.containsKey("zoom")) {
				zoom = parseZoom(params.get("zoom"));
			}
			return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom));
		} else if (params.containsKey("pin")) {
			String[] coordinates = params.get("pin").split(",");
			final double lat = parseSilentDouble(coordinates[0]);
			final double lon = parseSilentDouble(coordinates[1]);
			int zoom = GeoParsedPoint.NO_ZOOM;
			if (!Algorithms.isEmpty(fragment)) {
				zoom = parseZoom(fragment.split("/")[0]);
			}
			return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom));
		}
		return null;
	}

	private static List<GeoParsedPoint> parseGeoUri(URI uri) {
		String schemeSpecific = uri.getSchemeSpecificPart();
		if (schemeSpecific == null) {
			return null;
		}
		if (uri.getRawSchemeSpecificPart().contains("%2B")) {
			schemeSpecific = schemeSpecific.replace("+", "%2B");
		}

		String name = null;
		final Pattern namePattern = Pattern.compile("[\\+\\s]*\\((.*)\\)[\\+\\s]*$");
		final Matcher nameMatcher = namePattern.matcher(schemeSpecific);
		if (nameMatcher.find()) {
			name = URLDecoder.decode(nameMatcher.group(1));
			if (name != null) {
				schemeSpecific = schemeSpecific.substring(0, nameMatcher.start());
			}
		}

		String positionPart;
		String queryPart = "";
		int queryStartIndex = schemeSpecific.indexOf('?');
		if (queryStartIndex == -1) {
			positionPart = schemeSpecific;
		} else {
			positionPart = schemeSpecific.substring(0, queryStartIndex);
			if (queryStartIndex < schemeSpecific.length()) {
				queryPart = schemeSpecific.substring(queryStartIndex + 1);
			}
		}

		final Pattern positionPattern = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)");
		final Matcher positionMatcher = positionPattern.matcher(positionPart);
		double lat = 0.0;
		double lon = 0.0;
		if (positionMatcher.find()) {
			lat = Double.valueOf(positionMatcher.group(1));
			lon = Double.valueOf(positionMatcher.group(2));
		}

		int zoom = GeoParsedPoint.NO_ZOOM;
		String searchRequest = null;
		for (String param : queryPart.split("&")) {
			String paramName;
			String paramValue = null;
			int nameValueDelimititerIndex = param.indexOf('=');
			if (nameValueDelimititerIndex == -1) {
				paramName = param;
			} else {
				paramName = param.substring(0, nameValueDelimititerIndex);
				if (nameValueDelimititerIndex < param.length()) {
					paramValue = param.substring(nameValueDelimititerIndex + 1);
				}
			}

			if ("z".equals(paramName) && paramValue != null) {
				zoom = (int) Float.parseFloat(paramValue);
			} else if ("q".equals(paramName) && paramValue != null) {
				searchRequest = URLDecoder.decode(paramValue);
			}
		}

		if (searchRequest != null) {
			String searchPattern = Pattern.compile("(?:\\.|,|\\s+|\\+|[+-]?\\d+(?:\\.\\d+)?)").pattern();
			String[] search = searchRequest.split(searchPattern);
			if (search.length > 0) {
				return Collections.singletonList(new GeoParsedPoint(searchRequest));
			}
			final Matcher positionInSearchRequestMatcher = positionPattern.matcher(searchRequest);
			if (lat == 0.0 && lon == 0.0 && positionInSearchRequestMatcher.find()) {
				double tempLat = Double.valueOf(positionInSearchRequestMatcher.group(1));
				double tempLon = Double.valueOf(positionInSearchRequestMatcher.group(2));
				if (MapUtils.isValidLatLon(tempLat, tempLon)) {
					lat = tempLat;
					lon = tempLon;
				}
			}
		}
		if (lat == 0.0 && lon == 0.0 && searchRequest != null) {
			return Collections.singletonList(new GeoParsedPoint(searchRequest));
		}
		if (zoom != GeoParsedPoint.NO_ZOOM) {
			return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom, name));
		}
		return Collections.singletonList(new GeoParsedPoint(lat, lon, name));
	}

	protected static int parseZoom(String zoom) {
		try {
			if (zoom != null) {
				return (int) Float.parseFloat(zoom);
			}
		} catch (NumberFormatException e) {
		}
		return GeoParsedPoint.NO_ZOOM;
	}

	private static double parseSilentDouble(String zoom) {
		return parseSilentDouble(zoom, 0);
	}

	private static double parseSilentDouble(String zoom, double vl) {
		try {
			if (zoom != null) {
				return Double.valueOf(zoom);
			}
		} catch (NumberFormatException e) {
		}
		return vl;
	}
}
