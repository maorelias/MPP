package ex1;

import java.util.concurrent.locks.ReentrantLock;

public class Ex1q6 {

	public static ReentrantLock COUNTER_LOCK = new ReentrantLock();
	public static int COUNTER = 0;
	public final static int ITERATION_COUNT = 1000000;

	public static void main(String[] args) {
		int numOfThreads = Integer.parseInt(args[0]);
		Thread[] threads = new Thread[numOfThreads];

		// spawn threads that do the required task
		for (int index = 0; index < numOfThreads; index++) {
			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					for (int i = 0; i < ITERATION_COUNT; i++) {
						COUNTER_LOCK.lock();  // use a lock to force mutual exclusion
						try {
							int currentCounter = COUNTER;
							currentCounter++;
							COUNTER = currentCounter;
						} finally {
							COUNTER_LOCK.unlock();
						}
					}
				}
			});

			threads[index] = thread;
		}

		long startTime = System.currentTimeMillis();

		// start all threads
		for (Thread thread : threads) {
			thread.start();
		}

		// wait for all threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

		long finishTime = System.currentTimeMillis();
		long elapsedTime = finishTime - startTime;

		System.out.println("Counter: " + COUNTER);
		System.out.println("Elapsed time in ms: " + elapsedTime);
	}

}
