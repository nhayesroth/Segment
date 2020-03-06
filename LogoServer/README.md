### Front LOGO Server

- Assignment: https://frontapp.github.io/front-backend-exercise/2019-06-14-dbd77b/
- Summary: Multi-threaded TCP server that allows clients to draw on/render a virtual canvas.

#### Classes
- [Server](https://github.com/nhayes-roth/front-logo-server/blob/master/src/main/java/server/Server.java): Server class that creates TCP connections and spawns threads for accepted clients.
- [RequestHandler](https://github.com/nhayes-roth/front-logo-server/blob/master/src/main/java/server/RequestHandler.java): Runnable class that handles all requests for a single client connection.
- [Request](https://github.com/nhayes-roth/front-logo-server/blob/master/src/main/java/server/Request.java): Contains information the client passes in (as a String), parsed and normalized.
- [Canvas](https://github.com/nhayes-roth/front-logo-server/blob/master/src/main/java/server/Canvas.java): Holds the virtual canvas object, as well as various state information (such as current cursor coordinates/mode).
- [Coordinates](https://github.com/nhayes-roth/front-logo-server/blob/master/src/main/java/server/Coordinates.java): Simple class representing (x,y) coordinates.
- [Direction](https://github.com/nhayes-roth/front-logo-server/blob/master/src/main/java/server/Direction.java): Simple class representing directions on the canvas, as well as the order in which the direction can be switched.
