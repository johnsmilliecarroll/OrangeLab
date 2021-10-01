public class Orange {
	public enum State {
		Fetched(15), Peeled(38), Squeezed(29), Bottled(17), Processed(1);

		private static final int finalIndex = State.values().length - 1;

		final int timeToComplete;

		State(int timeToComplete) {
			this.timeToComplete = timeToComplete;
		}
		
		/**
		* Increments the state, from the enumerator list State.
		*/

		State getNext() {
			int currIndex = this.ordinal();
			if (currIndex >= finalIndex) {
				throw new IllegalStateException("Already at final state");
			}
			return State.values()[currIndex + 1];
		}
	}

	private State state;

	public Orange() {
		state = State.Fetched;
		doWork();
	}
	
	/**
	* Returns the enum state that the orange is currently on
	*/

	public State getState() {
		return state;
	}
	
	/**
	* If the orange is already processed, call an exception; we dont need to work on it.
	* Next it calls doWork() to allow the state duration to elapse and to increment to
	* the next state.
	*/

	public void runProcess() {
		// Don't attempt to process an already completed orange
		if (state == State.Processed) {
			throw new IllegalStateException("This orange has already been processed");
		}
		doWork();
		state = state.getNext();
	}
	
	/**
	* Calls the current thread to stop running (or sleep) for the time designated in the
	* State enum list. If it is interrupted before that time has elapsed, it throws an error.
	*/

	private void doWork() {
		// Sleep for the amount of time necessary to do the work
		try {
			Thread.sleep(state.timeToComplete);
		} catch (InterruptedException e) {
			System.err.println("Incomplete orange processing, juice may be bad");
		}
	}
}