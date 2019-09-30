import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private double root_ullat;
    private double root_ullon;
    private double root_lrlat;
    private double root_lrlon;
    private int tile_size;
    public Rasterer() {
        // YOUR CODE HERE
        root_ullat = MapServer.ROOT_ULLAT;
        root_ullon = MapServer.ROOT_ULLON;
        root_lrlat = MapServer.ROOT_LRLAT;
        root_lrlon = MapServer.ROOT_LRLON;
        tile_size = MapServer.TILE_SIZE;
    }


    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        double ullon = params.get("ullon");
        double ullat = params.get("ullat");
        double lrlon = params.get("lrlon");
        double lrlat = params.get("lrlat");
        double w = params.get("w");
        Map<String, Object> results = new HashMap<>();
        results.put("render_grid", new String[1][1]);
        results.put("raster_ul_lon", 0.0);
        results.put("raster_ul_lat", 0.0);
        results.put("raster_lr_lon", 0.0);
        results.put("raster_lr_lat", 0.0);
        results.put("depth", 0.0);
        results.put("query_success", true);

        // first deal with corner cases:
        // there are two cases where we should set query_success = false
        // first case is when the query box is located completely outside the root longitude/latitudes
        // second case is when the query box doesn't make sense - eg. ullon, ullat is located to the right of lrlon, lrlat
        if (ullon > root_lrlon || lrlon < root_ullon || lrlat > root_ullat || ullat < root_lrlat
        || ullon > lrlon || lrlat > ullat) {
            results.put("query_success", false);
            return results;
        }

        // if the input is valid, first identify the depth by calculating LonDPP
        // and then find the figure of depth i which has the greatest LonDPP_i among all the figure whose
        // LonDPP_k is less then or equal to required LonDPP
        // and then determine the grid
        // System.out.println(params);
        double LonDPP = (lrlon - ullon) * 1.0 / w;
        int depth = findDepth(LonDPP);
        results.put("depth", depth);
        // and then determine the grid
        double lon_step_size = (root_lrlon - root_ullon) * Math.pow(0.5, depth);
        double lat_step_size = (root_ullat - root_lrlat) * Math.pow(0.5, depth);
        int left_lon = findGreatestSmall(depth, ullon, root_ullon, lon_step_size);
        int upper_lat = findSmallestGreat(depth, ullat, root_ullat, lat_step_size);
        int right_lon = ((int) Math.pow(2, depth)) - 1 - findSmallestGreat(depth, lrlon, root_lrlon, lon_step_size);
        int lower_lat = ((int) Math.pow(2, depth)) - 1 - findGreatestSmall(depth, lrlat, root_lrlat, lat_step_size);
        // which means the grid is of d_depth; x_left_i ~ x_right_i; y_upper_j ~ y_lower_j
        results.put("raster_ul_lon", root_ullon + left_lon * lon_step_size);
        results.put("raster_ul_lat", root_ullat - upper_lat * lat_step_size);
        results.put("raster_lr_lon", root_ullon + (right_lon + 1) * lon_step_size);
        results.put("raster_lr_lat", root_ullat - (lower_lat + 1) * lat_step_size);
        int width =  right_lon + 1 - left_lon;
        int length = lower_lat + 1- upper_lat;
        String[][] render_grid = new String[length][width];
        for (int x = left_lon; x < right_lon + 1; x++) {
            for (int y = upper_lat; y < lower_lat + 1; y++) {
                render_grid[y - upper_lat][x - left_lon] = "d" + depth + "_x" + x + "_y" + y + ".png";
            }
        }
        results.put("render_grid", render_grid);


        return results;
    }
    private int findDepth(double LonDPP) {
        double bound = LonDPP * tile_size;
        for (int i = 0; i < 8; i++) {
            if ((root_lrlon - root_ullon) * Math.pow(0.5, i) < bound) {
                return i;
            }
        }
        return 7;
    }

    private int findSmallestGreat(int depth, double bound, double start, double step_size) {
        int k;
        for (k = 0; k < Math.pow(2, depth); k++)  {
            if (start - k * step_size < bound) {
                if (k == 0) { return k; }
                else {return k - 1;}
            }
        }
        return k - 1;
    }

    private int findGreatestSmall(int depth, double bound, double start, double step_size) {
        int k;
        for (k = 0; k < Math.pow(2, depth); k++) {
            if (start + k * step_size > bound) {
                if (k == 0) { return k; }
                else {return k - 1;}
            }
        }
        return k - 1;
    }

}
