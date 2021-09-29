public class Plant implements Runnable {
	// How long do we want to run the juice processing
	public static final long PROCESSING_TIME = 5 * 1000;
	private static final int NUM_PLANTS = 2;
	private static final int NUM_WORKERS = 15;
	private final BlockMutex m = new BlockMutex();

	public static void main(String[] args) {
		// Startup the plants
		Plant[] plants = new Plant[NUM_PLANTS];
		for (int i = 0; i < NUM_PLANTS; i++) {
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

	public void startPlant() {
		timeToWork = true;
		for (int i = 0; i < NUM_WORKERS; i++) {
			workers[i].start();
		}
	}

	public void stopPlant() {
		timeToWork = false;
	}

	public void waitToStop() {
		for (int i = 0; i < NUM_WORKERS; i++) {
			try {
				workers[i].join();
			} catch (InterruptedException e) {
				System.err.println(workers[i].getName() + " stop malfunction");
			}
		}
	}

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

	public int getProvidedOranges() {
		return orangesProvided;
	}

	public int getProcessedOranges() {
		return orangesProcessed;
	}

	public int getBottles() {
		return orangesProcessed / ORANGES_PER_BOTTLE;
	}

	public int getWaste() {
		return orangesProcessed % ORANGES_PER_BOTTLE;
	}
}
