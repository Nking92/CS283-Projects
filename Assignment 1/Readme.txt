The BenchmarkClient spawns 50 threads and writes 5000 lines to the socket, then waits for all of the input from the server.  The client tells how long it took for all threads to finish their work.  The numbers were chosen to ensure that the server does not reject any connections due to too many concurrent requests and also to highlight the advantage of the multithreaded server over the single threaded server.

Here are outputs for the single threaded and multithreaded servers:
Single threaded:
Run 1: 1965 ms
Run 2: 2126 ms
Run 3: 1945 ms
Run 4: 2098 ms
Run 5: 1796 ms

Multi threaded: 
Run 1: 2326 ms
Run 2: 1480 ms
Run 3: 1475 ms
Run 4: 1631 ms
Run 5: 1289 ms

The multithreaded server performs better in general, but for smaller numbers of queries (around 1000 per thread) the single threaded server outperformed the multithreaded server.  This is probably because of the overhead required to spawn a new thread for every request.  Reusing threads would improve this bottleneck.  The multithreaded server also varies a lot more in the time it might take to finish all of the work.  This is because the multithreaded server is at the mercy of the system scheduler.  The margin by which the multithreaded server will outperform the single threaded server is ultimately up to how the threads are scheduled, in this case.
