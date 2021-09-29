
public class BlockMutex {

	private volatile boolean occupied;

	BlockMutex() {
		occupied = false;
	}

	synchronized public void acquire() {
		while (occupied) { // other threads can't occupy
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		occupied = true; // a thread has occupied the mutex
	}

	synchronized public void release() {
		occupied = false; // mutex is free for a thread to occupy
		notifyAll(); // let the threads know
	}
}
