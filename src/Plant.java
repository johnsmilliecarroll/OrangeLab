public class Plant implements Runnable {
	// How long do we want to run the juice processing
	public static final long PROCESSING_TIME = 5 * 1000;
	private static final int NUM_PLANTS = 2;
	private static final int NUM_WORKERS = 15;
	private final BlockMutex m = new BlockMutex();

	public static void main(String[] args) {
		// Startup the plants
		Plant[] plants = new Plant[NUM_PLANTS];
		for (int i = 0; i < NUM_PLANTS; i++) { // instantiate plants based on how many are in NUM_PLANTS
			plants[i] = new Plant(1);
			plants[i].startPlant();
		}

		// Give the plants time to do work
		delay(PROCESSING_TIME, "Plant malfunction");

		// Stop the plant, and wait for it to shutdown
		for (Plant p : plants) {
			p.stopPlant();
		}
		for (Plant p : plants) {
			p.waitToStop();
		}

		// Summarize the results
		int totalProvided = 0;
		int totalProcessed = 0;
		int totalBottles = 0;
		int totalWasted = 0;

		for (Plant p : plants) {
			totalProvided += p.getProvidedOranges();
			totalProcessed += p.getProcessedOranges();
			totalBottles += p.getBottles();
			totalWasted += p.getWaste();
		}

		System.out.println("Total provided/processed = " + totalProvided + "/" + totalProcessed);
		System.out.println("Created " + totalBottles + ", wasted " + totalWasted + " oranges");
	}

	/**
	 * Allows a thread time to sleep, calls an error message if theres an
	 * interrupted exception.
	 */

	private static void delay(long time, String errMsg) {
		long sleepTime = Math.max(1, time);
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			System.err.println(errMsg);
		}
	}

	public final int ORANGES_PER_BOTTLE = 3;

	private int orangesProvided;
	private int orangesProcessed;
	private volatile boolean timeToWork;
	Thread[] workers = new Thread[NUM_WORKERS];
	private Orange currentOrange = new Orange();

	Plant(int threadNum) {
		orangesProvided = 0;
		orangesProcessed = 0;
		// create worker threads
		for (int i = 0; i < NUM_WORKERS; i++) {
			workers[i] = new Thread(this, "Worker " + (i + 1));
		}

	}

	/**
	 * Begins plant by setting timeToWork to true, and loops through the worker
	 * threads to start them.
	 */

	public void startPlant() {
		timeToWork = true;
		for (int i = 0; i < NUM_WORKERS; i++) {
			workers[i].start(); // start worker threads
		}
	}

	/**
	 * Sets time to work to false, the plant is trying to stop.
	 */

	public void stopPlant() {
		timeToWork = false;
	}

	/**
	 * Called upon when plant is stopping, the worker threads need to be notified to
	 * stop too.
	 */

	public void waitToStop() {
		for (int i = 0; i < NUM_WORKERS; i++) {
			try {
				workers[i].join(); // stop threads
			} catch (InterruptedException e) {
				System.err.println(workers[i].getName() + " stop malfunction");
			}
		}
	}

	/**
	 * This is where we run each thread. At the beginning we print each thread that
	 * is going to be processing, then run the first one, calling on Block Mutex to
	 * manage them. We print out which thread is working and begin work on the
	 * orange. When it complete, we release the mutex, and call Thread.yield() to
	 * make sure the thread takes a break and lets someone else do some work.
	 */

	public void run() {
		System.out.print(Thread.currentThread().getName() + " Processing oranges");
		System.out.println("");
		while (timeToWork) {
			m.acquire(); // acquire block mutex
			try {
				String completedState = currentOrange.getState().toString(); // storing current state for the print statement down the line
				System.out.println(Thread.currentThread().getName() + " is busy...");
				processOrange(currentOrange);
				System.out.println(Thread.currentThread().getName() + " has " + completedState + " the orange!");
			} finally {
				m.release(); // release the mutex
			}
			Thread.yield(); // The worker takes a break and lets another worker do something
		}
	}

	/**
	 * This function relies on runProcess() in Orange. It does the "work" and
	 * changes the state of the orange. If an orange has been bottled, we increment
	 * oranges provided and processed and we create a new orange.
	 */

	private void processOrange(Orange o) {
		if (o.getState() == Orange.State.Bottled) { // We've successfully processed an orange!
			orangesProcessed++;
			orangesProvided++;
			currentOrange = new Orange(); // We need a new orange
		} else // We need to do work
		{
			o.runProcess(); // do work, change task for the next worker
		}

	}

	/**
	 * This returns current number of provided oranges. It is part of the
	 * constructor for plant.
	 */

	public int getProvidedOranges() {
		return orangesProvided;
	}

	/**
	 * This returns current number of processed oranges. It is part of the
	 * constructor for plant.
	 */

	public int getProcessedOranges() {
		return orangesProcessed;
	}

	/**
	 * This returns current number of bottles that have been filled, taking total
	 * oranges processed divided by how many it takes to fill a bottle. It is part
	 * of the constructor for plant.
	 */

	public int getBottles() {
		return orangesProcessed / ORANGES_PER_BOTTLE;
	}

	/**
	 * This returns the number of oranges that have not been bottled, taking total
	 * oranges processed mod how many it takes to fill a bottle, resulting in the
	 * unbottled oranges that arent being used. It is part of the constructor for
	 * plant.
	 */

	public int getWaste() {
		return orangesProcessed % ORANGES_PER_BOTTLE;
	}
}