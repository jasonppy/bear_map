import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {
        List<Long> route = new ArrayList<>();
        Map<Long, Double> distTo = new HashMap<>();
        Map<Long, Long> edgeTo = new HashMap<>();
        Set<Long> marked = new HashSet<>();
        Long first = g.closest(stlon, stlat);
        Long last = g.closest(destlon, destlat);
        for (Long v: g.vertices()) {
            distTo.put(v, Double.POSITIVE_INFINITY);
            edgeTo.put(v, Long.MAX_VALUE);
        }
        class Node {
            Long id;
            Double d;
            Node (Long id, double d) {
                this.id = id;
                this.d = d;
            }
        }
        Comparator<Node> comparator = new Comparator<Node>() {
            @Override
            public int compare(Node v, Node w) {
                double cmp = v.d - w.d;
                if (cmp > 0) { return 1; }
                else if (cmp < 0) { return -1; }
                return 0;
            }
        };
        PriorityQueue<Node> pq = new PriorityQueue<>(comparator);
        pq.add(new Node(first, g.distance(first, last)));
        distTo.put(first, 0D);
        edgeTo.put(first, 0L);
        while (!pq.isEmpty()) {
            Node vNode = pq.remove();
            Long v = vNode.id;
            if (v.equals(last)) { break; }
            if (!marked.contains(v)) {
                marked.add(v);
                for (Long w: g.adjacent(v)) {
                    double gd = g.distance(v, w) + distTo.get(v);
                    if (gd < distTo.get(w)) {
                        distTo.replace(w, gd);
                        edgeTo.replace(w, v);
                        pq.add(new Node(w, g.distance(w, last) + gd));
                    }
                }
            }


        }
        for (Long v = last; v != 0L; v = edgeTo.get(v)) {
            route.add(0, v);
        }
        return route;

    }

    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        List<NavigationDirection> list = new ArrayList<>();
        Long startNode = route.get(0);
        double distance = 0;
        double relativeBearing;
        double prevBearing = g.bearing(route.get(0), route.get(1));
        int direction = 0;
        long curWay = 0;

        if (route.size() < 2) {
            return null;
        }


        for (int i = 1; i < route.size(); i++) {
            Long prevNode = route.get(i - 1);
            Long curNode = route.get(i);
            double curBearing = g.bearing(prevNode, curNode);
            relativeBearing = curBearing - prevBearing;

            if (prevNode.equals(startNode)) {
                curWay = getCurWay(g, prevNode, curNode);
            } else {
                prevBearing = curBearing;
            }

            if (i == route.size() - 1) {
                distance += g.distance(prevNode, curNode);
            } else if (g.nodes.get(curNode).ways.contains(curWay)) {
                distance += g.distance(prevNode, curNode);
                continue;
            }

            NavigationDirection turn = new NavigationDirection();
            turn.way = g.edges.get(curWay).name;
            turn.distance = distance;
            turn.direction = direction;
            list.add(turn);
            startNode = curNode;
            direction = getDirection(relativeBearing);
            distance = g.distance(prevNode, curNode);


        }
        return list;
    }

    private static Long getCurWay(GraphDB g, Long prevNode, Long curNode) {
        for (Long r: g.nodes.get(prevNode).ways) {
            if (g.nodes.get(curNode).ways.contains(r)) { return r; }
        }
        return null;
    }

    private static int getDirection(double relativeBearing) {
        if (-15 <= relativeBearing && relativeBearing <= 15) { return 1; }
        else if (-30 <= relativeBearing && relativeBearing < -15) { return 2; }
        else if (15 < relativeBearing && relativeBearing <= 30) { return 3; }
        else if (-100 <= relativeBearing && relativeBearing < -30) { return 4; }
        else if (30 < relativeBearing && relativeBearing <= 100) { return 5; }
        else if (relativeBearing < -100) { return 6; }
        else { return 7; }
    }


    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
